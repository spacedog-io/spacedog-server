/**
 * © David Attias 2015
 */
package io.spacedog.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.google.common.primitives.Ints;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;

import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceHeaders;

public class SpaceRequest {
	private JsonNode bodyJson;
	private HttpMethod method;
	private String backendId;
	private String uri;
	private Object body;
	private Map<String, String> routeParams = Maps.newHashMap();
	private Map<String, String> queryStrings = Maps.newHashMap();
	private Map<String, String> headers = Maps.newHashMap();
	private Map<String, Object> fields = Maps.newHashMap();
	private String password;
	private String username;
	private Boolean forTesting = null;

	// static defaults
	private static boolean forTestingDefault = false;
	private static SpaceRequestConfiguration configurationDefault;

	static {
		Unirest.setHttpClient(HttpClients.createMinimal(//
				new BasicHttpClientConnectionManager()));
	}

	public SpaceRequest(String uri, HttpMethod method) {
		this.uri = uri;
		this.method = method;
	}

	private String computeUrl() {
		// works for both http and https urls
		if (uri.startsWith("http"))
			return uri;

		SpaceTarget target = configuration().target();

		if (Strings.isNullOrEmpty(backendId))
			backendId = Backends.ROOT_API;

		return target.url(backendId, uri).toString();
	}

	public static SpaceRequest get(String uri) {
		return new SpaceRequest(uri, HttpMethod.GET);
	}

	public static SpaceRequest post(String uri) {
		return new SpaceRequest(uri, HttpMethod.POST);
	}

	public static SpaceRequest put(String uri) {
		return new SpaceRequest(uri, HttpMethod.PUT);
	}

	public static SpaceRequest delete(String uri) {
		return new SpaceRequest(uri, HttpMethod.DELETE);
	}

	public static SpaceRequest options(String uri) {
		return new SpaceRequest(uri, HttpMethod.OPTIONS);
	}

	public static SpaceRequest head(String uri) {
		return new SpaceRequest(uri, HttpMethod.HEAD);
	}

	public SpaceRequest backend(Backend backend) {
		return backendId(backend.backendId);
	}

	public SpaceRequest backendId(String backendId) {
		if (!Strings.isNullOrEmpty(this.backendId))
			Exceptions.runtime("backend already set");
		this.backendId = backendId;
		return this;
	}

	public SpaceRequest adminAuth(Backend backend) {
		return basicAuth(backend.backendId, backend.username, backend.password);
	}

	public SpaceRequest userAuth(User user) {
		return basicAuth(user.backendId, user.username, user.password);
	}

	public SpaceRequest basicAuth(Backend backend, String username, String password) {
		return basicAuth(backend.backendId, username, password);
	}

	public SpaceRequest basicAuth(String backendId, String username, String password) {
		this.username = username;
		this.password = password;
		return backendId(backendId);
	}

	public SpaceRequest body(byte[] bytes) {
		this.body = bytes;
		return this;
	}

	public SpaceRequest body(String body) {
		this.body = body;
		return this;
	}

	public SpaceRequest body(Path path) throws IOException {
		return body(Files.readAllBytes(path));
	}

	public SpaceRequest body(Object... elements) {
		return body(Json.object(elements));
	}

	public SpaceRequest body(JsonNode body) {
		this.bodyJson = body;
		this.body = body.toString();
		return this;
	}

	public SpaceRequest body(JsonBuilder<ObjectNode> jsonBody) {
		return body(jsonBody.build());
	}

	public SpaceRequest resource(String path) throws IOException {
		return body(Resources.toByteArray(Resources.getResource(path)));
	}

	public SpaceRequest routeParam(String name, String value) {
		this.routeParams.put(name, value);
		return this;
	}

	public SpaceRequest queryString(String name, String value) {
		this.queryStrings.put(name, value);
		return this;
	}

	public SpaceRequest size(int size) {
		return this.queryString("size", String.valueOf(size));
	}

	public SpaceRequest from(int from) {
		return this.queryString("from", String.valueOf(from));
	}

	public SpaceRequest refresh() {
		return this.queryString("refresh", "true");
	}

	public SpaceRequest header(String name, String value) {
		this.headers.put(name, value);
		return this;
	}

	public SpaceResponse go(int... expectedStatus) throws Exception {
		SpaceResponse response = go();
		HttpResponse<String> httpResponse = response.httpResponse();
		if (!Ints.contains(expectedStatus, httpResponse.getStatus()))
			throw Exceptions.runtime("[%s %s] returns status [%s] and payload [%s]", method, uri,
					httpResponse.getStatusText(), httpResponse.getBody());
		return response;
	}

	public SpaceResponse go() throws Exception {
		if ((forTesting == null && forTestingDefault)//
				|| (forTesting != null && forTesting))
			this.header(SpaceHeaders.SPACEDOG_TEST, "true");

		HttpRequest request = (method == HttpMethod.GET || method == HttpMethod.HEAD) //
				? new GetRequest(method, computeUrl())//
				: new HttpRequestWithBody(method, computeUrl());

		if (!Strings.isNullOrEmpty(username))
			request.basicAuth(username, password);

		this.headers.forEach((key, value) -> request.header(key, value));
		this.queryStrings.forEach((key, value) -> request.queryString(key, value));
		this.routeParams.forEach((key, value) -> request.routeParam(key, value));

		if (request instanceof HttpRequestWithBody) {
			HttpRequestWithBody requestWithBody = (HttpRequestWithBody) request;
			if (!fields.isEmpty())
				requestWithBody.fields(fields);
			else if (body instanceof byte[])
				requestWithBody.body((byte[]) body);
			else if (body instanceof String)
				requestWithBody.body((String) body);
		}

		return new SpaceResponse(request, bodyJson, configuration().debug());
	}

	public SpaceRequest field(String name, String value) {
		this.fields.put(name, value);
		return this;
	}

	// public SpaceRequest field(String name, File value) {
	// this.fields.put(name, value.getAbsolutePath());
	//
	// Optional<MultipartBody> multipartBody = checkBodyIsMultipart();
	// if (multipartBody.isPresent())
	// multipartBody.get().field(name, value);
	// else
	// ((HttpRequestWithBody) request).field(name, value);
	//
	// return this;
	// }

	// public SpaceRequest field(String name, String value, String contentType)
	// {
	//
	// Optional<MultipartBody> multipartBody = checkBodyIsMultipart();
	// if (multipartBody.isPresent())
	// multipartBody.get().field(name, value, contentType);
	// else
	// ((HttpRequestWithBody) request).field(name, value, contentType);
	//
	// return this;
	// }

	// public SpaceRequest field(String name, Collection<?> value) {
	// Optional<MultipartBody> multipartBody = checkBodyIsMultipart();
	// if (multipartBody.isPresent())
	// multipartBody.get().field(name, value);
	// else
	// ((HttpRequestWithBody) request).field(name, value);
	// return this;
	// }

	// private Optional<MultipartBody> checkBodyIsMultipart() {
	//
	// HttpRequestWithBody requestWithBody = checkRequestWithBody();
	//
	// if (requestWithBody.getBody() == null)
	// return Optional.empty();
	//
	// if (requestWithBody.getBody() instanceof MultipartBody)
	// return Optional.of((MultipartBody) requestWithBody.getBody());
	//
	// throw new IllegalStateException(String.format("request body is not
	// multipart but [%s]", //
	// request.getBody().getClass().getName()));
	// }
	//
	// private HttpRequestWithBody checkRequestWithBody() {
	//
	// if (request instanceof HttpRequestWithBody)
	// return (HttpRequestWithBody) request;
	//
	// throw new IllegalStateException(//
	// String.format("illegal for requests of type [%s]",
	// request.getHttpMethod()));
	// }

	public static void setLogDebug(boolean debug) {
		configuration().debug(debug);
	}

	public SpaceRequest superdogAuth() {
		return superdogAuth(Backends.ROOT_API);
	}

	public SpaceRequest superdogAuth(Backend backend) {
		return superdogAuth(backend.backendId);
	}

	public SpaceRequest superdogAuth(String backendId) {
		SpaceRequestConfiguration conf = configuration();
		return basicAuth(backendId, conf.superdogName(), conf.superdogPassword());
	}

	public SpaceRequest forTesting(boolean forTesting) {
		this.forTesting = forTesting;
		return this;
	}

	public static void setForTestingDefault(boolean value) {
		forTestingDefault = value;
	}

	public static void setConfigurationDefault(SpaceRequestConfiguration configuration) {
		configurationDefault = configuration;
	}

	public static SpaceRequestConfiguration configuration() {
		if (configurationDefault == null)
			configurationDefault = SpaceRequestConfiguration.get();
		return configurationDefault;
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
		return header(SpaceHeaders.SPACEDOG_DEBUG, "true");
	}

	// public static void setDefaultBackend(String backendId) {
	// defaultBackendId = Optional.of(backendId);
	// }

}
