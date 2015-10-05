package io.spacedog.services;

import org.junit.Assert;

import com.eclipsesource.json.JsonObject;
import com.google.common.primitives.Ints;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public abstract class AbstractTest extends Assert {

	private static String backendDomain = "http://localhost:8080";

	protected static void refreshIndex(String index) throws UnirestException {
		System.out.println();
		System.out
				.println(String.format("Refresh index [%s] => %s", index,
						Unirest.post("http://localhost:9200/{index}/_refresh")
								.routeParam("index", index).asString()
								.getStatusText()));
	}

	protected static GetRequest prepareGet(String uri) {
		return prepareGet(uri, null);
	}

	protected static GetRequest prepareGet(String uri, String spacedogKey) {
		String url = new StringBuilder(backendDomain).append(uri).toString();
		GetRequest request = Unirest.get(url);
		if (spacedogKey != null)
			request.header(AdminResource.SPACEDOG_KEY_HEADER, spacedogKey);
		return request;
	}

	protected static HttpRequestWithBody preparePost(String uri) {
		return preparePost(uri, null);
	}

	protected static HttpRequestWithBody preparePost(String uri,
			String spacedogKey) {
		String url = new StringBuilder(backendDomain).append(uri).toString();
		HttpRequestWithBody request = Unirest.post(url);
		if (spacedogKey != null)
			request.header(AdminResource.SPACEDOG_KEY_HEADER, spacedogKey);
		return request;
	}

	protected static HttpRequestWithBody preparePut(String uri) {
		return preparePut(uri, null);
	}

	protected static HttpRequestWithBody preparePut(String uri,
			String spacedogKey) {
		String url = new StringBuilder(backendDomain).append(uri).toString();
		HttpRequestWithBody request = Unirest.put(url);
		if (spacedogKey != null)
			request.header(AdminResource.SPACEDOG_KEY_HEADER, spacedogKey);
		return request;
	}

	protected static HttpRequestWithBody prepareDelete(String uri) {
		return prepareDelete(uri, null);
	}

	protected static HttpRequestWithBody prepareDelete(String uri,
			String spacedogKey) {
		String url = new StringBuilder(backendDomain).append(uri).toString();
		HttpRequestWithBody request = Unirest.delete(url);
		if (spacedogKey != null)
			request.header(AdminResource.SPACEDOG_KEY_HEADER, spacedogKey);
		return request;
	}

	protected HttpRequestWithBody prepareOptions(String uri) {
		return prepareOptions(uri, null);
	}

	protected HttpRequestWithBody prepareOptions(String uri, String spacedogKey) {
		String url = new StringBuilder(backendDomain).append(uri).toString();
		HttpRequestWithBody request = Unirest.options(url);
		if (spacedogKey != null)
			request.header(AdminResource.SPACEDOG_KEY_HEADER, spacedogKey);
		return request;
	}

	protected static Result get(HttpRequest req, int... expectedStatus)
			throws UnirestException {
		return exec(req.getHttpRequest(), null, expectedStatus);
	}

	protected static Result post(RequestBodyEntity req, int... expectedStatus)
			throws UnirestException {
		return exec(req.getHttpRequest(), req.getBody(), expectedStatus);
	}

	protected static Result delete(HttpRequestWithBody req,
			int... expectedStatus) throws UnirestException {
		return exec(req.getHttpRequest(), req.getBody(), expectedStatus);
	}

	protected static Result put(RequestBodyEntity req, int... expectedStatus)
			throws UnirestException {
		return exec(req.getHttpRequest(), req.getBody(), expectedStatus);
	}

	protected static Result options(HttpRequestWithBody req,
			int... expectedStatus) throws UnirestException {
		return exec(req.getHttpRequest(), req.getBody(), expectedStatus);
	}

	private static Result exec(HttpRequest req, Object requestBody,
			int... expectedStatus) throws UnirestException {

		HttpResponse<String> resp = req.asString();

		System.out.println();
		System.out.println(String.format("%s %s => %s => %s",
				req.getHttpMethod(), req.getUrl(), resp.getStatus(),
				resp.getStatusText()));

		req.getHeaders().forEach(
				(key, value) -> System.out.println(String.format("%s : %s",
						key, value)));

		if (requestBody != null)
			System.out.println(String
					.format("Request body = [%s]", requestBody));

		Result result = new Result(resp);

		resp.getHeaders().forEach(
				(key, value) -> System.out.println(String.format("=> %s : %s",
						key, value)));

		System.out.println(String.format("=> Response body = [%s]", result
				.isJson() ? Json.prettyString(result.json) : resp.getBody()));

		assertTrue(Ints.contains(expectedStatus, resp.getStatus()));
		return result;
	}

	public static class Result {
		private JsonObject json;
		private HttpResponse<String> response;

		public Result(HttpResponse<String> response) {
			this.response = response;

			String body = response.getBody();
			if (Json.isJson(body)) {
				json = JsonObject.readFrom(body);
			}
		}

		public boolean isJson() {
			return json != null;
		}

		public JsonObject json() {
			return json;
		}

		public HttpResponse<String> response() {
			return response;
		}

	}
}
