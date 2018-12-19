/**
 * © David Attias 2015
 */
package io.spacedog.client.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Assert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SpaceResponse implements Closeable {

	private Request okRequest;
	private Response okResponse;
	private DateTime before;
	private long duration;
	private byte[] bytesBody;
	private String stringBody;
	private JsonNode jsonBody;
	private boolean consumed;

	private static OkHttpClient okHttpClient;

	public SpaceResponse(SpaceRequest spaceRequest, Request okRequest) {

		this.okRequest = okRequest;
		boolean debug = SpaceEnv.env().debug();

		if (debug) {
			Utils.info();
			spaceRequest.printRequest(okRequest);
			Utils.info();
			Utils.infoNoLn("=>>> ");
		}

		this.before = DateTime.now();

		try {
			this.okResponse = okHttpClient().newCall(okRequest).execute();
		} catch (IOException e) {
			throw Exceptions.runtime(e, "request [%s][%s] failed", //
					okRequest.method(), okRequest.url());
		}

		this.duration = DateTime.now().getMillis() - this.before.getMillis();

		if (debug) {
			Utils.info("%s (%s) in %s ms", okResponse.message(), //
					okResponse.code(), duration);

			Utils.info("Response headers:");

			Headers headers = okResponse.headers();
			for (int i = 0; i < headers.size(); i++)
				Utils.info(" %s: %s", headers.name(i), headers.value(i));

			if (isJson())
				Utils.info("Response body: %s", Json.toPrettyString(asJson()));
		}
	}

	public static OkHttpClient okHttpClient() {

		if (okHttpClient == null) {
			SpaceEnv env = SpaceEnv.env();

			okHttpClient = new OkHttpClient.Builder()//
					.followRedirects(true)//
					.followSslRedirects(false)//
					.connectTimeout(env.httpTimeoutMillis(), TimeUnit.MILLISECONDS)//
					.readTimeout(env.httpTimeoutMillis(), TimeUnit.MILLISECONDS)//
					.writeTimeout(env.httpTimeoutMillis(), TimeUnit.MILLISECONDS)//
					.build();
		}
		return okHttpClient;
	}

	public DateTime before() {
		return before;
	}

	public long duration() {
		return duration;
	}

	public ResponseBody body() {
		return this.okResponse.body();
	}

	public byte[] asBytes() {

		if (bytesBody != null)
			return bytesBody;

		if (consumed)
			throw Exceptions.runtime("payload has already been consumed as string");

		try {
			consumed = true;
			bytesBody = this.body().bytes();
		} catch (IOException e) {
			throw Exceptions.runtime(e, //
					"failed to read as bytes payload of request [%s][%s]", //
					okRequest.method(), okRequest.url());
		} finally {
			this.close();
		}

		return bytesBody;
	}

	public String asString() {

		if (stringBody != null)
			return stringBody;

		if (consumed)
			throw Exceptions.runtime("payload has already been consumed as bytes");

		try {
			consumed = true;
			stringBody = this.okResponse.body().string();
		} catch (IOException e) {
			throw Exceptions.runtime(e, //
					"failed to read as string payload of request [%s][%s]", //
					okRequest.method(), okRequest.url());
		} finally {
			this.close();
		}

		return stringBody;
	}

	public boolean isJson() {
		return ContentTypes.isJsonContent(contentType());
	}

	public JsonNode asJson() {
		if (jsonBody != null)
			return jsonBody;

		String body = asString();

		// Body can be empty. Example:
		// HEAD request strip response body
		if (Utils.isNullOrEmpty(body))
			jsonBody = NullNode.getInstance();

		else if (Json.isJson(body))
			jsonBody = Json.readNode(body);

		else
			throw Exceptions.runtime("payload isn't JSON");

		return jsonBody;
	}

	public ObjectNode asJsonObject() {
		return Json.checkObject(asJson());
	}

	public ArrayNode asJsonArray() {
		return Json.checkArray(asJson());
	}

	public InputStream asByteStream() {
		return okResponse.body().byteStream();
	}

	public Reader asCharStream() {
		return okResponse.body().charStream();
	}

	public <K> K asPojo(Class<K> pojoClass) {
		return Json.toPojo(asString(), pojoClass);
	}

	public <K> K asPojo(TypeReference<K> pojoClass) {
		return Json.toPojo(asString(), pojoClass);
	}

	public <K> K asPojo(JavaType type) {
		return Json.toPojo(asString(), type);
	}

	public <K> K asPojo(K result) {
		return Json.updatePojo(asString(), result);
	}

	public <K> K asPojo(String fieldPath, Class<K> pojoClass) {
		return Json.toPojo(asJson(), fieldPath, pojoClass);
	}

	public Request okRequest() {
		return okRequest;
	}

	public Response okResponse() {
		if (okResponse == null)
			throw new IllegalStateException("no response yet");
		return okResponse;
	}

	public int status() {
		return okResponse.code();
	}

	@Override
	public void close() {
		if (this.okResponse != null)
			this.okResponse.close();
	}

	public SpaceResponse asVoid() {
		this.close();
		return this;
	}

	public String contentType() {
		return header(SpaceHeaders.CONTENT_TYPE);
	}

	public long contentLength() {
		String header = header(SpaceHeaders.CONTENT_LENGTH);
		return Strings.isNullOrEmpty(header) ? 0 : Long.valueOf(header);
	}

	public String etag() {
		return header(SpaceHeaders.ETAG);
	}

	public String header(String name) {
		return okResponse.header(name);
	}

	// when header contains multiple comma separated values
	public List<String> headerAsList(String name) {
		String[] values = header(name).split(",");
		for (int i = 0; i < values.length; i++)
			values[i] = values[i].trim();
		return Lists.newArrayList(values);
	}

	// when same header multiple times
	public List<String> headers(String name) {
		return okResponse.headers(name);
	}

	public JsonNode debug() {
		String header = header(SpaceHeaders.SPACEDOG_DEBUG);
		return Strings.isNullOrEmpty(header) ? NullNode.getInstance() : Json.readNode(header);
	}

	public boolean has(String jsonPath) {
		return isJson() //
				&& Json.get(asJson(), jsonPath) != null;
	}

	public JsonNode get(String jsonPath) {
		return Json.get(asJson(), jsonPath);
	}

	public String getString(String jsonPath) {
		return Json.checkString(Json.get(asJson(), jsonPath));
	}

	public JsonNode findValue(String fieldName) {
		return asJson().findValue(fieldName);
	}

	public SpaceResponse assertEquals(String expected, String jsonPath) {
		Assert.assertEquals(expected, Json.checkString(//
				Json.checkNotNull(Json.get(asJson(), jsonPath))));
		return this;
	}

	public SpaceResponse assertEquals(int expected, String jsonPath) {
		JsonNode node = Json.get(asJson(), jsonPath);
		Assert.assertEquals(expected, //
				node.isArray() || node.isObject() ? node.size() : node.intValue());
		return this;
	}

	public SpaceResponse assertEquals(long expected, String jsonPath) {
		Assert.assertEquals(expected, Json.get(asJson(), jsonPath).longValue());
		return this;
	}

	public SpaceResponse assertEquals(float expected, String jsonPath, float delta) {
		Assert.assertEquals(expected, Json.get(asJson(), jsonPath).floatValue(), delta);
		return this;
	}

	public SpaceResponse assertEquals(double expected, String jsonPath, double delta) {
		Assert.assertEquals(expected, Json.get(asJson(), jsonPath).doubleValue(), delta);
		return this;
	}

	public SpaceResponse assertEquals(DateTime expected, String jsonPath) {
		Assert.assertEquals(expected, DateTime.parse(Json.get(asJson(), jsonPath).asText()));
		return this;
	}

	public SpaceResponse assertEquals(boolean expected, String jsonPath) {
		Assert.assertEquals(expected, Json.get(asJson(), jsonPath).asBoolean());
		return this;
	}

	public SpaceResponse assertEquals(JsonNode expected) {
		Assert.assertEquals(expected, asJson());
		return this;
	}

	public SpaceResponse assertEquals(JsonNode expected, String jsonPath) {
		Assert.assertEquals(expected, Json.get(asJson(), jsonPath));
		return this;
	}

	public SpaceResponse assertEqualsWithoutMeta(ObjectNode expected) {
		Assert.assertEquals(expected.deepCopy().without("meta"), //
				asJsonObject().deepCopy().without("meta"));
		return this;
	}

	public SpaceResponse assertNotNull(String jsonPath) {
		JsonNode node = Json.get(asJson(), jsonPath);
		if (node == null || node.isNull())
			Assert.fail(String.format("json property [%s] is null", jsonPath));
		return this;
	}

	public SpaceResponse assertNotNullOrEmpty(String jsonPath) {
		JsonNode node = Json.get(asJson(), jsonPath);
		if (Strings.isNullOrEmpty(node.asText()))
			Assert.fail(String.format("json string property [%s] is null or empty", jsonPath));
		return this;
	}

	public SpaceResponse assertTrue(String jsonPath) {
		Assert.assertTrue(Json.get(asJson(), jsonPath).asBoolean());
		return this;
	}

	public SpaceResponse assertFalse(String jsonPath) {
		Assert.assertFalse(Json.get(asJson(), jsonPath).asBoolean());
		return this;
	}

	public SpaceResponse assertDateIsValid(String jsonPath) {
		assertNotNull(jsonPath);
		JsonNode node = Json.get(asJson(), jsonPath);
		if (node.isTextual()) {
			try {
				DateTime.parse(node.asText());
				return this;
			} catch (IllegalArgumentException e) {
			}
		}
		Assert.fail(String.format(//
				"json property [%s] with value [%s] is not a valid SpaceDog date", jsonPath, node));
		return this;
	}

	public SpaceResponse assertDateIsRecent(String jsonPath) {
		long now = DateTime.now().getMillis();
		assertDateIsValid(jsonPath);
		DateTime date = DateTime.parse(Json.get(asJson(), jsonPath).asText());
		if (date.isBefore(now - 3000) || date.isAfter(now + 3000))
			Assert.fail(String.format(//
					"json property [%s] with value [%s] is not a recent SpaceDog date (now +/- 3s)", jsonPath, date));
		return this;
	}

	public SpaceResponse assertSizeEquals(int size, String jsonPath) {
		assertJsonContent();
		JsonNode node = Json.get(asJson(), jsonPath);
		if (Json.isNull(node))
			Assert.fail(String.format("property [%s] is null", jsonPath));
		if (size != node.size())
			Assert.fail(String.format("expected size [%s], json property [%s] node size [%s]", //
					size, jsonPath, node.size()));
		return this;
	}

	public SpaceResponse assertSizeEquals(int size) {
		assertJsonContent();
		if (size != asJson().size())
			Assert.fail(String.format("expected size [%s], root json node size [%s]", //
					size, asJson().size()));
		return this;
	}

	public SpaceResponse assertJsonContent() {
		if (asJson() == null)
			Assert.fail("response content is not json formatted");
		return this;
	}

	public SpaceResponse assertContainsValue(String expected, String fieldName) {
		assertJsonContent();
		if (!asJson().findValuesAsText(fieldName).contains(expected))
			Assert.fail(String.format("no field named [%s] found with value [%s]", fieldName, expected));
		return this;
	}

	public SpaceResponse assertNotPresent(String jsonPath) {
		assertJsonContent();
		JsonNode node = Json.get(asJson(), jsonPath);
		if (node != null)
			Assert.fail(String.format(//
					"json path [%s] contains [%s]", jsonPath, node));
		return this;
	}

	public SpaceResponse assertPresent(String jsonPath) {
		assertJsonContent();
		JsonNode node = Json.get(asJson(), jsonPath);
		if (node == null)
			Assert.fail(String.format(//
					"json path [%s] not found", jsonPath));
		return this;
	}

	public SpaceResponse assertString(String jsonPath) {
		assertPresent(jsonPath);
		JsonNode node = Json.get(asJson(), jsonPath);
		if (!node.isTextual())
			Assert.fail(String.format(//
					"json path [%s] not string", jsonPath));
		return this;
	}

	public SpaceResponse assertInteger(String jsonPath) {
		assertPresent(jsonPath);
		JsonNode node = Json.get(asJson(), jsonPath);
		if (!node.isInt())
			Assert.fail(String.format(//
					"json path [%s] not integer", jsonPath));
		return this;
	}

	public SpaceResponse assertHeaderEquals(Object expected, String headerName) {
		String headerValue = header(headerName);
		if (!expected.toString().equalsIgnoreCase(headerValue))
			Assert.fail(String.format("header [%s] not equal to [%s] but %s", //
					headerName, expected, headerValue));

		return this;
	}

	public SpaceResponse assertHeaderContains(Object expected, String headerName) {
		List<String> headerValues = headerAsList(headerName);

		if (headerValues == null)
			Assert.fail(String.format("header [%s] not found", headerName));

		if (!Utils.containsIgnoreCase(headerValues, expected.toString()))
			Assert.fail(String.format("value [%s] not found in header [%s] containing %s", //
					expected, headerName, headerValues));

		return this;
	}

	public SpaceResponse assertHeaderNotPresent(String headerName) {
		String headerValue = header(headerName);

		if (headerValue != null)
			Assert.fail(String.format("header [%s] is present with value [%s]", //
					headerName, headerValue));

		return this;
	}

	public void assertBodyEquals(String expected) {
		Assert.assertEquals(expected, asString());
	}

}
