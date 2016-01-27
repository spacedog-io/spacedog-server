/**
 * © David Attias 2015
 */
package io.spacedog.client;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Ints;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.MultipartBody;

import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceHeaders;

public class SpaceRequest {
	private HttpRequest request;
	private JsonNode body;

	public SpaceRequest(HttpRequest request) {
		this.request = request;
	}

	public static SpaceRequest get(String uri) {
		return get(true, uri);
	}

	public static SpaceRequest get(boolean ssl, String uri) {
		String url = uri.startsWith("http") ? uri//
				: ssl ? computeMainUrl(uri) : computeOptionalUrl(uri);
		GetRequest request = Unirest.get(url);
		return new SpaceRequest(request);
	}

	public static SpaceRequest post(String uri) {
		return post(true, uri);
	}

	public static SpaceRequest post(boolean ssl, String uri) {
		String url = uri.startsWith("http") ? uri//
				: ssl ? computeMainUrl(uri) : computeOptionalUrl(uri);
		HttpRequestWithBody request = Unirest.post(url);
		return new SpaceRequest(request);
	}

	public static SpaceRequest put(String uri) {
		return put(true, uri);
	}

	public static SpaceRequest put(boolean ssl, String uri) {
		String url = uri.startsWith("http") ? uri//
				: ssl ? computeMainUrl(uri) : computeOptionalUrl(uri);
		HttpRequestWithBody request = Unirest.put(url);
		return new SpaceRequest(request);
	}

	public static SpaceRequest delete(String uri) {
		return delete(true, uri);
	}

	public static SpaceRequest delete(boolean ssl, String uri) {
		String url = uri.startsWith("http") ? uri//
				: ssl ? computeMainUrl(uri) : computeOptionalUrl(uri);
		HttpRequestWithBody request = Unirest.delete(url);
		return new SpaceRequest(request);
	}

	public static SpaceRequest options(String uri) {
		return options(true, uri);
	}

	public static SpaceRequest options(boolean ssl, String uri) {
		String url = uri.startsWith("http") ? uri//
				: ssl ? computeMainUrl(uri) : computeOptionalUrl(uri);
		HttpRequestWithBody request = Unirest.options(url);
		return new SpaceRequest(request);
	}

	private static String computeMainUrl(String uri) {
		SpaceTarget target = SpaceRequestConfiguration.get().target();
		return (target.ssl() ? "https://" : "http://") + target.host()
				+ (target.port() == 443 ? "" : ":" + target.port()) + uri;
	}

	private static String computeOptionalUrl(String uri) {
		SpaceTarget target = SpaceRequestConfiguration.get().target();
		return "http://" + target.host()//
				+ (target.optionalPort() == 80 ? "" : ":" + target.optionalPort()) + uri;
	}

	public SpaceRequest backendKey(String backendKey) {
		if (backendKey != null)
			request.header(SpaceHeaders.BACKEND_KEY, backendKey);
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

	public SpaceRequest basicAuth(String[] credentials) {
		return basicAuth(credentials[0], credentials[1]);
	}

	public SpaceRequest basicAuth(String username, String password) {
		request.basicAuth(username, password);
		return this;
	}

	public SpaceRequest body(byte[] bytes) {
		if (request instanceof HttpRequestWithBody) {
			((HttpRequestWithBody) request).body(bytes);
			return this;
		}

		throw new IllegalStateException(
				String.format("%s requests don't take any body", request.getHttpMethod().name()));
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

	public SpaceResponse go() throws Exception {
		return new SpaceResponse(request, body, SpaceRequestConfiguration.get().debug());
	}

	public SpaceResponse go(int... expectedStatus) throws Exception {
		SpaceResponse spaceResponse = go();
		Assert.assertTrue(Ints.contains(expectedStatus, spaceResponse.httpResponse().getStatus()));
		return spaceResponse;
	}

	public SpaceRequest field(String name, File value) {
		Optional<MultipartBody> multipartBody = checkBodyIsMultipart();
		if (multipartBody.isPresent())
			multipartBody.get().field(name, value);
		else
			((HttpRequestWithBody) request).field(name, value);
		return this;
	}

	public SpaceRequest field(String name, String value) {
		return field(name, value, null);
	}

	public SpaceRequest field(String name, String value, String contentType) {
		Optional<MultipartBody> multipartBody = checkBodyIsMultipart();
		if (multipartBody.isPresent())
			multipartBody.get().field(name, value, contentType);
		else
			((HttpRequestWithBody) request).field(name, value, contentType);
		return this;
	}

	public SpaceRequest field(String name, Collection<?> value) {
		Optional<MultipartBody> multipartBody = checkBodyIsMultipart();
		if (multipartBody.isPresent())
			multipartBody.get().field(name, value);
		else
			((HttpRequestWithBody) request).field(name, value);
		return this;
	}

	private Optional<MultipartBody> checkBodyIsMultipart() {

		HttpRequestWithBody requestWithBody = checkRequestWithBody();

		if (requestWithBody.getBody() == null)
			return Optional.empty();

		if (requestWithBody.getBody() instanceof MultipartBody)
			return Optional.of((MultipartBody) requestWithBody.getBody());

		throw new IllegalStateException(String.format("request body is not multipart but [%s]", //
				request.getBody().getClass().getName()));
	}

	private HttpRequestWithBody checkRequestWithBody() {

		if (request instanceof HttpRequestWithBody)
			return (HttpRequestWithBody) request;

		throw new IllegalStateException(//
				String.format("illegal for requests of type [%s]", request.getHttpMethod()));
	}

	public static void setLogDebug(boolean debug) {
		SpaceRequestConfiguration.get().debug(debug);
	}

	public SpaceRequest superdogAuth() {
		SpaceRequestConfiguration conf = SpaceRequestConfiguration.get();
		return basicAuth(conf.superdogName(), conf.superdogPassword());
	}

	public SpaceRequest forTesting(boolean test) {
		if (test)
			this.queryString("test", "true");
		return this;
	}
}
