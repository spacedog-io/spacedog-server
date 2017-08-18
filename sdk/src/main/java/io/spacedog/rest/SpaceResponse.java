/**
 * © David Attias 2015
 */
package io.spacedog.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Utils;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpaceResponse {

	private Request okRequest;
	private Response okResponse;
	private DateTime before;
	private long duration;
	private byte[] bytesBody;
	private String stringBody;
	private JsonNode jsonNode;
	private boolean consumed;

	private static OkHttpClient okHttpClient;

	public SpaceResponse(SpaceRequest spaceRequest, Request okRequest) {

		this.okRequest = okRequest;
		boolean debug = SpaceRequest.env().debug();

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
			SpaceEnv env = SpaceRequest.env();

			okHttpClient = new OkHttpClient.Builder()//
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

	public byte[] asBytes() {

		if (bytesBody != null)
			return bytesBody;

		if (consumed)
			throw Exceptions.runtime("payload has already been consumed as string");

		try {
			consumed = true;
			bytesBody = this.okResponse.body().bytes();
		} catch (IOException e) {
			throw Exceptions.runtime(e, //
					"failed to read as bytes payload of request [%s][%s]", //
					okRequest.method(), okRequest.url());
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
		}

		try {
			if (Json.isJson(stringBody))
				jsonNode = Json.readNode(stringBody);
		} catch (Exception ignore) {
			// not really a json body
		}

		return stringBody;
	}

	public boolean isJson() {
		return SpaceHeaders.isJsonContent(contentType());
	}

	public JsonNode asJson() {
		if (jsonNode != null)
			return jsonNode;

		asString();

		if (jsonNode == null)
			throw Exceptions.runtime("payload isn't of type JSON");

		return jsonNode;
	}

	public ObjectNode asJsonObject() {
		JsonNode node = asJson();
		if (!node.isObject())
			throw Exceptions.runtime("not a JSON object but [%s]", node.getNodeType());
		return (ObjectNode) node;
	}

	public ArrayNode asJsonArray() {
		JsonNode node = asJson();
		if (!node.isArray())
			throw Exceptions.runtime("not a JSON array but [%s]", node.getNodeType());
		return (ArrayNode) node;
	}

	public InputStream asByteStream() {
		return okResponse.body().byteStream();
	}

	public Reader asCharStream() {
		return okResponse.body().charStream();
	}

	public Request okRequest() {
		return okRequest;
	}

	public Response okResponse() {
		if (okResponse == null)
			throw new IllegalStateException("no response yet");
		return okResponse;
	}

	public String contentType() {
		return header(SpaceHeaders.CONTENT_TYPE);
	}

	public String header(String name) {
		return okResponse.header(name);
	}

	public List<String> headers(String name) {
		return okResponse.headers(name);
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

	public SpaceResponse assertContains(JsonNode expected) {
		assertJsonContent();
		if (!Iterators.contains(asJson().elements(), expected))
			Assert.fail(String.format(//
					"response does not contain [%s]", expected));
		return this;
	}

	public SpaceResponse assertContains(JsonNode expected, String jsonPath) {
		assertJsonContent();
		if (!Iterators.contains(Json.get(asJson(), jsonPath).elements(), expected))
			Assert.fail(String.format(//
					"field [%s] does not contain [%s]", jsonPath, expected));
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

	public SpaceResponse assertHeaderEquals(String expected, String headerName) {
		if (!Arrays.asList(expected).equals(headers(headerName)))
			Assert.fail(String.format("response header [%s] not equal to [%s] but to %s", //
					headerName, expected, headers(headerName)));

		return this;
	}

	public SpaceResponse assertHeaderContains(String expected, String headerName) {
		List<String> headerValues = headers(headerName);

		if (headerValues == null)
			Assert.fail(String.format("response header [%s] not found", headerName));

		if (!headerValues.contains(expected))
			Assert.fail(String.format(//
					"response header [%s] does not contain value [%s] but %s", //
					headerName, expected, headerValues));

		return this;
	}

	public void assertBodyEquals(String expected) {
		Assert.assertEquals(expected, asString());
	}

	public <K> K toPojo(Class<K> pojoClass) {
		return Json.toPojo(asJson(), pojoClass);
	}

	public <K> K toPojo(String fieldPath, Class<K> pojoClass) {
		return Json.toPojo(asJson(), fieldPath, pojoClass);
	}

	public int status() {
		return okResponse.code();
	}
}
