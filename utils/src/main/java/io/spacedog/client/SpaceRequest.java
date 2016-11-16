/**
 * © David Attias 2015
 */
package io.spacedog.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;

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
import io.spacedog.utils.AuthorizationHeader;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Schema;
import io.spacedog.utils.Settings;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Utils;

public class SpaceRequest {

	private JsonNode bodyJson;
	private HttpMethod method;
	private String backendId;
	private String uri;
	private Object body;
	private Map<String, String> routeParams = Maps.newHashMap();
	private Map<String, String> queryParams = Maps.newHashMap();
	private Map<String, String> headers = Maps.newHashMap();
	private Map<String, Object> formFields = Maps.newHashMap();
	private String password;
	private String username;
	private String bearerToken;
	private Boolean forTesting = null;

	// static defaults
	private static boolean forTestingDefault = false;
	private static SpaceRequestConfiguration configurationDefault;

	// static {
	// Unirest.setHttpClient(HttpClients.createMinimal(//
	// new BasicHttpClientConnectionManager()));
	// }

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
			backendId = Backends.rootApi();

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

	public SpaceRequest www(Backend backend) {
		return www(backend.backendId);
	}

	private SpaceRequest www(String backendId) {
		return backendId(backendId + ".www");
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
		return userAuth(backend.adminUser);
	}

	public SpaceRequest userAuth(User user) {
		if (user.accessToken == null)
			return basicAuth(user.backendId, user.username, user.password);
		else
			return bearerAuth(user.backendId, user.accessToken);
	}

	public SpaceRequest basicAuth(Backend backend) {
		return basicAuth(backend.adminUser);
	}

	public SpaceRequest basicAuth(User user) {
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

	public SpaceRequest bearerAuth(Backend backend) {
		return bearerAuth(backend.adminUser);
	}

	public SpaceRequest bearerAuth(User user) {
		return bearerAuth(user.backendId, user.accessToken);
	}

	public SpaceRequest bearerAuth(Backend backend, String accessToken) {
		return bearerAuth(backend.backendId, accessToken);
	}

	public SpaceRequest bearerAuth(String backendId, String accessToken) {
		bearerAuth(accessToken);
		return backendId(backendId);
	}

	public SpaceRequest bearerAuth(String accessToken) {
		this.bearerToken = accessToken;
		return this;
	}

	public SpaceRequest body(byte[] bytes) {
		this.body = bytes;
		return this;
	}

	public SpaceRequest body(String body) {
		this.body = body;
		return this;
	}

	public SpaceRequest bodyFromPath(Path path) {
		try {
			return body(Files.readAllBytes(path));
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public SpaceRequest body(Object... elements) {
		return body(Json.object(elements));
	}

	public SpaceRequest body(Settings settings) {
		return body(Json.mapper().valueToTree(settings));
	}

	public SpaceRequest body(Schema schema) {
		return body(schema.node());
	}

	public SpaceRequest body(JsonNode body) {
		this.bodyJson = body;
		this.body = body.toString();
		return this;
	}

	public SpaceRequest body(JsonBuilder<ObjectNode> jsonBody) {
		return body(jsonBody.build());
	}

	public SpaceRequest resource(String path) {
		try {
			return body(Resources.toByteArray(Resources.getResource(path)));
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public SpaceRequest routeParam(String name, String value) {
		this.routeParams.put(name, value);
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
		return this.queryParam("refresh", "true");
	}

	public SpaceRequest header(String name, String value) {
		this.headers.put(name, value);
		return this;
	}

	public SpaceResponse go(int... expectedStatus) {
		SpaceResponse response = go();
		HttpResponse<String> httpResponse = response.httpResponse();
		if (!Ints.contains(expectedStatus, httpResponse.getStatus()))
			throw Exceptions.runtime("[%s %s] returns status [%s] and payload [%s]", method, uri,
					httpResponse.getStatusText(), httpResponse.getBody());
		return response;
	}

	public SpaceResponse go() {
		if ((forTesting == null && forTestingDefault)//
				|| (forTesting != null && forTesting))
			this.header(SpaceHeaders.SPACEDOG_TEST, "true");

		HttpRequest request = (method == HttpMethod.GET || method == HttpMethod.HEAD) //
				? new GetRequest(method, computeUrl())//
				: new HttpRequestWithBody(method, computeUrl());

		if (!Strings.isNullOrEmpty(username))
			request.basicAuth(username, password);

		else if (!Strings.isNullOrEmpty(bearerToken))
			request.header(SpaceHeaders.AUTHORIZATION, //
					String.join(" ", SpaceHeaders.BEARER_SCHEME, bearerToken));

		this.headers.forEach((key, value) -> request.header(key, value));
		this.queryParams.forEach((key, value) -> request.queryString(key, value));
		this.routeParams.forEach((key, value) -> request.routeParam(key, value));

		if (request instanceof HttpRequestWithBody) {
			HttpRequestWithBody requestWithBody = (HttpRequestWithBody) request;
			if (!formFields.isEmpty())
				requestWithBody.fields(formFields);
			else if (body instanceof byte[])
				requestWithBody.body((byte[]) body);
			else if (body instanceof String)
				requestWithBody.body((String) body);
		}

		return new SpaceResponse(this, request);
	}

	public SpaceRequest formField(String name, String value) {
		this.formFields.put(name, value);
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
		return superdogAuth(Backends.rootApi());
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
		Unirest.setTimeouts(configuration.httpTimeoutMillis(), configuration.httpTimeoutMillis());
	}

	public static SpaceRequestConfiguration configuration() {
		if (configurationDefault == null)
			setConfigurationDefault(SpaceRequestConfiguration.get());
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

	public SpaceRequest cookies(String... cookies) {
		for (String cookie : cookies)
			header(SpaceHeaders.COOKIE, cookie);
		return this;
	}

	public SpaceRequest cookies(List<String> cookies) {
		for (String cookie : cookies)
			header(SpaceHeaders.COOKIE, cookie);
		return this;
	}

	public void printRequest(HttpRequest httpRequest) {
		Utils.info("%s %s", httpRequest.getHttpMethod(), httpRequest.getUrl());

		Utils.info("Request headers:");
		httpRequest.getHeaders().forEach((key, value) -> printRequestHeader(key, value));

		if (httpRequest.getBody() != null) {
			Header header = httpRequest.getBody().getEntity().getContentType();
			if (header != null)
				Utils.info("  %s: [%s]", header.getName(), header.getValue());
		}

		if (!formFields.isEmpty()) {
			Utils.info("Request form body:");
			for (Entry<String, Object> entry : formFields.entrySet())
				Utils.info("  %s = %s", entry.getKey(), entry.getValue());
		}

		if (bodyJson != null)
			Utils.info("Request Json body: %s", Json.toPrettyString(bodyJson));
	}

	private void printRequestHeader(String key, List<String> value) {
		if (AuthorizationHeader.isKey(key)) {
			AuthorizationHeader authHeader = new AuthorizationHeader(value);
			if (authHeader.isBasic()) {
				Utils.info("  Authorization: [Basic %s:*******]", authHeader.username());
				return;
			}
		}
		Utils.info("  %s: %s", key, value);
	}

	// public static void setDefaultBackend(String backendId) {
	// defaultBackendId = Optional.of(backendId);
	// }

}
