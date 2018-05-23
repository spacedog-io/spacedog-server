/**
 * © David Attias 2015
 */
package io.spacedog.client.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

import io.spacedog.client.SpaceDog;
import io.spacedog.utils.AuthorizationHeader;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class SpaceRequest {

	private static enum HttpVerb {
		GET, POST, PUT, DELETE, HEAD, OPTIONS
	}

	private HttpVerb method;
	private SpaceBackend backend;
	private String path;
	private JsonNode bodyJson;
	private Object body;
	private Request.Builder requestBuilder;
	private Map<String, String> pathParams = Maps.newHashMap();
	private Map<String, String> queryParams = Maps.newHashMap();
	private Map<String, String> formFields = Maps.newHashMap();
	private Boolean forTesting = null;
	private MediaType contentType;
	private long contentLength = -1;

	// static defaults
	private static boolean forTestingDefault = false;

	private SpaceRequest(String path, HttpVerb verb) {
		this.path = path;
		this.method = verb;
		this.requestBuilder = new Request.Builder();
	}

	private HttpUrl computeHttpUrl() {

		HttpUrl.Builder builder = path.startsWith("http") //
				? HttpUrl.parse(path).newBuilder() //
				: computeHttpUrlFromBackendAndPath();

		for (Entry<String, String> queryParam : this.queryParams.entrySet())
			builder.addQueryParameter(queryParam.getKey(), queryParam.getValue());

		return builder.build();
	}

	private HttpUrl.Builder computeHttpUrlFromBackendAndPath() {
		if (backend == null)
			backend = SpaceEnv.env().apiBackend();

		HttpUrl.Builder builder = new HttpUrl.Builder()//
				.scheme(backend.scheme())//
				.host(backend.host())//
				.port(backend.port());

		path = applyPathParams(path);

		for (String segment : WebPath.parse(path))
			builder.addPathSegment(segment);

		return builder;
	}

	private String applyPathParams(String uri) {
		Check.notNull(uri, "uri");

		for (Entry<String, String> pathParam : this.pathParams.entrySet()) {
			if (uri.indexOf('{') == -1)
				break;

			uri = uri.replace(pathParam.getKey(), pathParam.getValue());
		}
		return uri;
	}

	public static SpaceRequest get(String path) {
		return new SpaceRequest(path, HttpVerb.GET);
	}

	public static SpaceRequest post(String path) {
		return new SpaceRequest(path, HttpVerb.POST);
	}

	public static SpaceRequest put(String path) {
		return new SpaceRequest(path, HttpVerb.PUT);
	}

	public static SpaceRequest delete(String path) {
		return new SpaceRequest(path, HttpVerb.DELETE);
	}

	public static SpaceRequest options(String path) {
		return new SpaceRequest(path, HttpVerb.OPTIONS);
	}

	public static SpaceRequest head(String path) {
		return new SpaceRequest(path, HttpVerb.HEAD);
	}

	public SpaceRequest backend(SpaceDog dog) {
		return backend(dog.backend());
	}

	public SpaceRequest backend(SpaceBackend backend) {
		this.backend = backend;
		return this;
	}

	public SpaceRequest backend(String backend) {
		this.backend = SpaceBackend.valueOf(backend);
		return this;
	}

	public SpaceRequest backendId(String backendId) {
		this.backend = SpaceEnv.env().apiBackend().fromBackendId(backendId);
		return this;
	}

	public SpaceRequest basicAuth(SpaceDog dog) {
		if (dog.password().isPresent())
			return basicAuth(dog.username(), dog.password().get());
		throw Exceptions.illegalArgument("password not set");
	}

	public SpaceRequest basicAuth(String username, String password) {
		Check.notNull(username, "username");
		Check.notNull(password, "password");
		return setHeader(SpaceHeaders.AUTHORIZATION, //
				Credentials.basic(username, password, Utils.UTF8));
	}

	public SpaceRequest bearerAuth(SpaceDog dog) {
		return bearerAuth(dog.accessToken().get());
	}

	public SpaceRequest bearerAuth(String accessToken) {
		Check.notNull(accessToken, "access token");
		return setHeader(SpaceHeaders.AUTHORIZATION, //
				Utils.join(" ", SpaceHeaders.BEARER_SCHEME, accessToken));
	}

	public MediaType getContentType(String defaultContentType) {
		return getContentType(MediaType.parse(defaultContentType));
	}

	public MediaType getContentType(MediaType defaultContentType) {
		return contentType == null ? defaultContentType : contentType;
	}

	public SpaceRequest withContentType(String contentType) {
		return withContentType(MediaType.parse(contentType));
	}

	public SpaceRequest withContentType(MediaType contentType) {
		this.contentType = contentType;
		return this;
	}

	public SpaceRequest withContentLength(long contentLength) {
		if (contentLength >= 0)
			setHeader(SpaceHeaders.CONTENT_LENGTH, contentLength);

		this.contentLength = contentLength;
		return this;
	}

	public SpaceRequest body(byte[] bytes) {
		this.body = bytes;
		return this;
	}

	public SpaceRequest body(String string) {
		this.body = string;
		return this;
	}

	public SpaceRequest body(RequestBody body) {
		this.body = body;
		return this;
	}

	public SpaceRequest body(File file) {
		if (contentType == null) //
			contentType = MediaType.parse(//
					ContentTypes.parseFileExtension(file.getName()));

		this.body = file;
		return this;
	}

	public SpaceRequest body(InputStream byteStream) {
		if (contentType == null)
			contentType = OkHttp.OCTET_STREAM;

		this.body = byteStream;
		return this;
	}

	public SpaceRequest bodyPojo(Object pojo) {
		return bodyJson(Json.toJsonNode(pojo));
	}

	public SpaceRequest bodyJson(String json) {
		this.contentType = OkHttp.JSON;
		if (SpaceEnv.env().debug())
			this.bodyJson = Json.readNode(json);
		this.body = json;
		return this;
	}

	public SpaceRequest bodyJson(Object... elements) {
		return bodyJson(Json.object(elements));
	}

	public SpaceRequest bodyJson(JsonNode jsonNode) {
		if (jsonNode == null)
			jsonNode = NullNode.instance;
		this.contentType = OkHttp.JSON;
		this.bodyJson = jsonNode;
		this.body = jsonNode.toString();
		return this;
	}

	public SpaceRequest routeParam(String name, String value) {
		this.pathParams.put('{' + name + '}', value);
		return this;
	}

	public SpaceRequest queryParam(String name) {
		this.queryParams.put(name, null);
		return this;
	}

	public SpaceRequest queryParam(String name, Object value) {
		if (value != null)
			this.queryParams.put(name, value.toString());
		return this;
	}

	public SpaceRequest size(Integer size) {
		return this.queryParam(SpaceParams.SIZE_PARAM, size);
	}

	public SpaceRequest from(Integer from) {
		return this.queryParam(SpaceParams.FROM_PARAM, from);
	}

	public SpaceRequest refresh() {
		return refresh(true);
	}

	public SpaceRequest refresh(boolean refresh) {
		if (refresh)
			this.queryParam(SpaceParams.REFRESH_PARAM, refresh);
		return this;
	}

	public SpaceRequest setHeader(String name, Object value) {
		this.requestBuilder.header(name, value.toString());
		return this;
	}

	public SpaceRequest addHeader(String name, Object value) {
		this.requestBuilder.addHeader(name, value.toString());
		return this;
	}

	public SpaceRequest removeHeader(String name) {
		this.requestBuilder.removeHeader(name);
		return this;
	}

	public SpaceResponse go(int... expectedStatus) {
		SpaceResponse response = go();
		if (!Ints.contains(expectedStatus, response.okResponse().code()))
			throw new SpaceRequestException(response);
		return response;
	}

	public SpaceResponse go() {
		if ((forTesting == null && forTestingDefault)//
				|| (forTesting != null && forTesting))
			this.setHeader(SpaceHeaders.SPACEDOG_TEST, "true");

		requestBuilder.url(computeHttpUrl())//
				.method(method.name(), computeRequestBody());

		return new SpaceResponse(this, requestBuilder.build());
	}

	private RequestBody computeRequestBody() {

		if (!formFields.isEmpty()) {
			FormBody.Builder builder = new FormBody.Builder();
			for (Entry<String, String> formField : formFields.entrySet())
				builder.add(formField.getKey(), formField.getValue());
			return builder.build();
		}

		if (body instanceof byte[])
			return RequestBody.create(//
					getContentType(OkHttp.OCTET_STREAM), (byte[]) body);

		if (body instanceof String)
			return RequestBody.create(//
					getContentType(OkHttp.TEXT_PLAIN), (String) body);

		if (body instanceof InputStream)
			return new ByteStreamRequestBody();

		if (body instanceof File)
			return RequestBody.create(//
					getContentType(OkHttp.OCTET_STREAM), (File) body);

		if (body instanceof RequestBody)
			return (RequestBody) body;

		// OkHttp doesn't accept null body for PUT and POST
		if (method.equals(HttpVerb.PUT) //
				|| method.equals(HttpVerb.POST))
			return RequestBody.create(//
					getContentType(OkHttp.OCTET_STREAM), "");

		return null;
	}

	public class ByteStreamRequestBody extends RequestBody {

		@Override
		public MediaType contentType() {
			return SpaceRequest.this.getContentType(OkHttp.OCTET_STREAM);
		}

		@Override
		public long contentLength() {
			return SpaceRequest.this.contentLength;
		}

		@Override
		public void writeTo(BufferedSink sink) throws IOException {
			Source source = null;
			try {
				source = Okio.source((InputStream) SpaceRequest.this.body);
				sink.writeAll(source);
			} finally {
				Utils.closeSilently(source);
			}
		}

	}

	public SpaceRequest formField(String name, String value) {
		Check.notNull(value, "form field " + name);
		this.formFields.put(name, value);
		return this;
	}

	public SpaceRequest forTesting(boolean forTesting) {
		this.forTesting = forTesting;
		return this;
	}

	public static void setForTestingDefault(boolean value) {
		forTestingDefault = value;
	}

	public static String getBackendKey(JsonNode account) {
		return new StringBuilder(account.get("backendId").asText())//
				.append(':')//
				.append(account.get("backendKey").get("name").asText())//
				.append(':')//
				.append(account.get("backendKey").get("secret").asText())//
				.toString();
	}

	public SpaceRequest debugServer() {
		return setHeader(SpaceHeaders.SPACEDOG_DEBUG, "true");
	}

	public SpaceRequest cookies(String... cookies) {
		removeHeader(SpaceHeaders.COOKIE);
		for (String cookie : cookies)
			addHeader(SpaceHeaders.COOKIE, cookie);
		return this;
	}

	public SpaceRequest cookies(List<String> cookies) {
		removeHeader(SpaceHeaders.COOKIE);
		for (String cookie : cookies)
			addHeader(SpaceHeaders.COOKIE, cookie);
		return this;
	}

	public void printRequest(Request okRequest) {
		Utils.info("%s %s", okRequest.method(), okRequest.url());

		Utils.info("Request headers:");
		Headers headers = okRequest.headers();
		for (int i = 0; i < headers.size(); i++)
			printRequestHeader(headers.name(i), headers.value(i));

		if (okRequest.body() != null) {
			Utils.info("  Content-Type: %s", okRequest.body().contentType());
			try {
				Utils.info("  Content-Length: %s", okRequest.body().contentLength());
			} catch (IOException ignore) {
				ignore.printStackTrace();
			}
		}

		if (!formFields.isEmpty()) {
			Utils.info("Request form body:");
			for (Entry<String, String> entry : formFields.entrySet())
				Utils.info("  %s = %s", entry.getKey(), entry.getValue());
		}

		if (bodyJson != null)
			Utils.info("Request Json body: %s", Json.toPrettyString(bodyJson));
	}

	private void printRequestHeader(String key, String value) {
		if (AuthorizationHeader.isKey(key)) {
			AuthorizationHeader authHeader = new AuthorizationHeader(value, false);
			if (authHeader.isBasic()) {
				Utils.info("  Authorization: Basic %s:*******", authHeader.username());
				return;
			}
		}
		Utils.info("  %s: %s", key, value);
	}
}
