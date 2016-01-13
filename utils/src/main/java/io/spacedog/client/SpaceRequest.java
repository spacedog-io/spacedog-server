/**
 * © David Attias 2015
 */
package io.spacedog.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;

import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.MultipartBody;

import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceHeaders;

public class SpaceRequest {

	private static boolean debug = true;
	private static SpaceTarget target = null;

	private HttpRequest request;
	private JsonNode body;

	private static String superdogName;
	private static String superdogPassword;

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

	public static void setTarget(SpaceTarget target) {
		SpaceRequest.target = target;
	}

	public static String getTargetHost() {
		return target.host();
	}

	private static String computeMainUrl(String uri) {
		if (target == null)
			target = SpaceTarget.valueOf(System.getProperty("target", "local"));
		return (target.ssl() ? "https://" : "http://") + target.host()
				+ (target.port() == 443 ? "" : ":" + target.port()) + uri;
	}

	private static String computeOptionalUrl(String uri) {
		if (target == null)
			target = SpaceTarget.valueOf(System.getProperty("target", "local"));
		return "http://" + target.host() + (target.optionalPort() == 80 ? "" : ":" + target.optionalPort()) + uri;
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
		return new SpaceResponse(request, body, debug);
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
		SpaceRequest.debug = debug;
	}

	public SpaceRequest superdogAuth() {
		if (superdogName == null) {

			Path path = Paths.get(//
					System.getProperty("user.home"), ".superdog.properties");

			if (Files.exists(path)) {
				try {
					Properties superdog = new Properties();
					superdog.load(Files.newInputStream(path));

					superdogPassword = superdog.getProperty("superdog.password");
					if (Strings.isNullOrEmpty(superdogPassword))
						throw new IllegalArgumentException("password null or empty");

					superdogName = superdog.getProperty("superdog.username");
					if (Strings.isNullOrEmpty(superdogName))
						throw new IllegalArgumentException("username null or empty");

				} catch (IOException e) {
					throw new RuntimeException(e);
				}

			} else
				throw new IllegalStateException(//
						String.format(".superdog.properties file not found at [%s]", path.toAbsolutePath()));
		}

		return basicAuth(superdogName, superdogPassword);
	}
}
