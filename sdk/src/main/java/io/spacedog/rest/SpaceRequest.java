/**
 * © David Attias 2015
 */
package io.spacedog.rest;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;

import io.spacedog.model.Schema;
import io.spacedog.model.Settings;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.AuthorizationHeader;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json7;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Utils;
import io.spacedog.utils.WebPath;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

public class SpaceRequest {

	private static enum HttpVerb {
		GET, POST, PUT, DELETE, HEAD, OPTIONS
	}

	private HttpVerb method;
	private String backend;
	private String path;
	private JsonNode bodyJson;
	private Object body;
	private Request.Builder requestBuilder;
	private Map<String, String> pathParams = Maps.newHashMap();
	private Map<String, String> queryParams = Maps.newHashMap();
	private Map<String, String> formFields = Maps.newHashMap();
	private Boolean forTesting = null;
	private MediaType contentType;

	// static defaults
	private static boolean forTestingDefault = false;
	private static SpaceEnv env;

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
		HttpUrl.Builder builder = null;

		if (Strings.isNullOrEmpty(backend))
			backend = Backends.rootApi();

		if (backend.startsWith("http"))
			builder = HttpUrl.parse(backend).newBuilder();
		else {
			SpaceTarget target = env().target();
			builder = new HttpUrl.Builder()//
					.scheme(target.scheme())//
					.host(target.host(backend))//
					.port(target.port());
		}

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

	public SpaceRequest www(SpaceDog backend) {
		return www(backend.backendId());
	}

	private SpaceRequest www(String backendId) {
		return backend(backendId + ".www");
	}

	public SpaceRequest backend(SpaceDog dog) {
		return backend(dog.backendId());
	}

	public SpaceRequest backend(String backend) {
		if (!Strings.isNullOrEmpty(this.backend))
			Exceptions.runtime("backend already set");
		this.backend = backend;
		return this;
	}

	public SpaceRequest auth(SpaceDog dog) {
		// TODO when all test are refactored like
		// vince.get("/1/...")...
		// this method should be removed

		backend(dog);

		Optional7<String> accessToken = dog.accessToken();
		if (accessToken.isPresent())
			return bearerAuth(accessToken.get());

		Optional7<String> password = dog.password();
		if (password.isPresent())
			return basicAuth(dog.username(), password.get());

		// if no password nor access token then no auth
		return this;
	}

	public SpaceRequest basicAuth(SpaceDog user) {
		// TODO when all tests are refactored like
		// vince.delete("/1/...")...
		// this method should not set the backend of the space request
		// but only the basic authorization
		return backend(user).basicAuth(user.username(), user.password().get());
	}

	public SpaceRequest basicAuth(String username, String password) {
		Check.notNull(username, "username");
		Check.notNull(password, "password");
		return setHeader(SpaceHeaders.AUTHORIZATION, //
				Credentials.basic(username, password, Utils.UTF8));
	}

	public SpaceRequest bearerAuth(SpaceDog user) {
		// TODO when all tests are refactored like
		// vince.get("/1/...")...
		// this method should not set the backend of the space request
		// but only the basic authorization
		return backend(user).bearerAuth(user.accessToken().get());
	}

	public SpaceRequest bearerAuth(String accessToken) {
		Check.notNull(accessToken, "access token");
		return setHeader(SpaceHeaders.AUTHORIZATION, //
				Utils.join(" ", SpaceHeaders.BEARER_SCHEME, accessToken));
	}

	public SpaceRequest contentType(MediaType contentType) {
		this.contentType = contentType;
		return this;
	}

	public SpaceRequest bodyBytes(byte[] bytes) {
		this.contentType = OkHttp.OCTET_STREAM;
		this.body = bytes;
		return this;
	}

	public SpaceRequest bodyString(String body) {
		this.contentType = OkHttp.TEXT_PLAIN;
		this.body = body;
		return this;
	}

	public SpaceRequest bodyMultipart(MultipartBody body) {
		this.body = body;
		return this;
	}

	public SpaceRequest bodyFile(File file) {
		try {
			return bodyBytes(Files.toByteArray(file));
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public SpaceRequest bodySettings(Settings settings) {
		return bodyJson(Json7.mapper().valueToTree(settings));
	}

	public SpaceRequest bodyPojo(Object pojo) {
		return bodyJson(Json7.toNode(pojo));
	}

	public SpaceRequest bodySchema(Schema schema) {
		return bodyJson(schema.node());
	}

	public SpaceRequest bodyJson(String body) {
		this.contentType = OkHttp.JSON;
		if (env.debug())
			this.bodyJson = Json7.readNode(body);
		this.body = body;
		return this;
	}

	public SpaceRequest bodyJson(Object... elements) {
		return bodyJson(Json7.object(elements));
	}

	public SpaceRequest bodyJson(JsonNode body) {
		this.contentType = OkHttp.JSON;
		this.bodyJson = body;
		this.body = body.toString();
		return this;
	}

	public SpaceRequest bodyResource(String path) {
		return bodyBytes(Utils.readResource(path));
	}

	public SpaceRequest bodyResource(Class<?> contextClass, String resourceName) {
		return bodyBytes(Utils.readResource(contextClass, resourceName));
	}

	public SpaceRequest routeParam(String name, String value) {
		this.pathParams.put('{' + name + '}', value);
		return this;
	}

	public SpaceRequest queryParam(String name, String value) {
		this.queryParams.put(name, value);
		return this;
	}

	public SpaceRequest size(int size) {
		return this.queryParam("size", String.valueOf(size));
	}

	public SpaceRequest from(int from) {
		return this.queryParam("from", String.valueOf(from));
	}

	public SpaceRequest refresh() {
		return refresh(true);
	}

	public SpaceRequest refresh(boolean value) {
		return this.queryParam("refresh", String.valueOf(value));
	}

	public SpaceRequest setHeader(String name, String value) {
		this.requestBuilder.header(name, value);
		return this;
	}

	public SpaceRequest addHeader(String name, String value) {
		this.requestBuilder.addHeader(name, value);
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
			return RequestBody.create(contentType, (byte[]) body);

		if (body instanceof String)
			return RequestBody.create(contentType, (String) body);

		// OkHttp doesn't accept null body for PUT and POST
		if (method.equals(HttpVerb.PUT) //
				|| method.equals(HttpVerb.POST))
			return RequestBody.create(OkHttp.TEXT_PLAIN, "");

		return null;
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

	public static void env(SpaceEnv value) {
		env = value;
	}

	public static SpaceEnv env() {
		if (env == null)
			env(SpaceEnv.defaultEnv());
		return env;
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
			Utils.info("Request Json body: %s", Json7.toPrettyString(bodyJson));
	}

	private void printRequestHeader(String key, String value) {
		if (AuthorizationHeader.isKey(key)) {
			AuthorizationHeader authHeader = new AuthorizationHeader(value, false);
			if (authHeader.isBasic()) {
				Utils.info("  Authorization: [Basic %s:*******]", authHeader.username());
				return;
			}
		}
		Utils.info("  %s: %s", key, value);
	}

	public SpaceRequest id(String value) {
		return this.queryParam("id", value);
	}
}
