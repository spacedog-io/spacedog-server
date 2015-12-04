/**
 * © David Attias 2015
 */
package io.spacedog.client;

import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Ints;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;

import io.spacedog.services.JsonBuilder;
import io.spacedog.services.SpaceContext;

public class SpaceRequest {

	static String backendDomain = "http://localhost:8080";
	// static String backendDomain = "https://spacedog.io";

	private HttpRequest request;

	private JsonNode body;

	public SpaceRequest(HttpRequest request) {
		this.request = request;
	}

	public static SpaceRequest get(String uri) {
		String url = computeUrl(uri);
		GetRequest request = Unirest.get(url);
		return new SpaceRequest(request);
	}

	public static SpaceRequest post(String uri) {
		String url = computeUrl(uri);
		HttpRequestWithBody request = Unirest.post(url);
		return new SpaceRequest(request);
	}

	public static SpaceRequest put(String uri) {
		String url = computeUrl(uri);
		HttpRequestWithBody request = Unirest.put(url);
		return new SpaceRequest(request);
	}

	public static SpaceRequest delete(String uri) {
		String url = computeUrl(uri);
		HttpRequestWithBody request = Unirest.delete(url);
		return new SpaceRequest(request);
	}

	public static SpaceRequest options(String uri) {
		String url = computeUrl(uri);
		HttpRequestWithBody request = Unirest.options(url);
		return new SpaceRequest(request);
	}

	private static String computeUrl(String uri) {
		return new StringBuilder(SpaceRequest.backendDomain).append(uri).toString();
	}

	public SpaceRequest backendKey(String backendKey) {
		if (backendKey != null)
			request.header(SpaceContext.BACKEND_KEY_HEADER, backendKey);
		return this;
	}

	public SpaceRequest backendKey(SpaceDogHelper.Account account) {
		return backendKey(account.backendKey);
	}

	public SpaceRequest basicAuth(SpaceDogHelper.Account account) {
		return basicAuth(account.username, account.password);
	}

	public SpaceRequest basicAuth(SpaceDogHelper.User user) {
		return basicAuth(user.username, user.password);
	}

	public SpaceRequest basicAuth(String username, String password) {
		request.basicAuth(username, password);
		return this;
	}

	public SpaceRequest body(String body) {
		if (request instanceof HttpRequestWithBody) {
			((HttpRequestWithBody) request).body(body);
			return this;
		}

		throw new IllegalStateException(
				String.format("%s requests don't take any body", request.getHttpMethod().name()));
	}

	public SpaceRequest body(JsonNode body) {
		this.body = body;
		return body(body.toString());
	}

	public SpaceRequest body(JsonBuilder<ObjectNode> jsonBody) {
		return body(jsonBody.build());
	}

	public SpaceRequest routeParam(String name, String value) {
		request.routeParam(name, value);
		return this;
	}

	public SpaceRequest queryString(String name, String value) {
		request.queryString(name, value);
		return this;
	}

	public SpaceRequest header(String name, String value) {
		request.header(name, value);
		return this;
	}

	public SpaceResponse go(int... expectedStatus) throws Exception {
		SpaceResponse spaceResponse = new SpaceResponse(request, body);
		Assert.assertTrue(Ints.contains(expectedStatus, spaceResponse.httpResponse().getStatus()));
		return spaceResponse;
	}
}
