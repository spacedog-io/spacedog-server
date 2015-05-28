package com.magiclabs.restapi;

import org.junit.Assert;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public abstract class AbstractTest extends Assert {

	protected static void refreshIndex(String index) throws UnirestException {
		System.out.println();
		System.out
				.println(String.format("Refresh index [%s] => %s", index,
						Unirest.post("http://localhost:9200/{index}/_refresh")
								.routeParam("index", index).asString()
								.getStatusText()));
	}

	protected static JsonObject get(GetRequest req, int... expectedStatus)
			throws UnirestException {
		return exec(req.getHttpRequest(), null, expectedStatus);
	}

	protected static JsonObject post(RequestBodyEntity req,
			int... expectedStatus) throws UnirestException {
		return exec(req.getHttpRequest(), req.getBody(), expectedStatus);
	}

	protected static JsonObject delete(HttpRequestWithBody req,
			int... expectedStatus) throws UnirestException {
		return exec(req.getHttpRequest(), req.getBody(), expectedStatus);
	}

	protected static JsonObject put(RequestBodyEntity req,
			int... expectedStatus) throws UnirestException {
		return exec(req.getHttpRequest(), req.getBody(), expectedStatus);
	}

	protected static JsonObject exec(HttpRequest req, Object requestBody,
			int... expectedStatus) throws UnirestException {

		HttpResponse<String> resp = req.asString();

		printHttpRequest(req, resp);

		if (requestBody != null)
			System.out.println(String.format("request body = %s", requestBody));

		try {
			JsonObject result = null;
			if (!Strings.isNullOrEmpty(resp.getBody())) {
				result = JsonObject.readFrom(resp.getBody());

				if (resp.getStatus() >= 400) {
					System.out.println(String.format("Error type = %s",
							result.get("type")));
					System.out.println(String.format("Error message = %s",
							result.get("message")));
					System.out.println("Error trace = ");
					result.get("trace")
							.asArray()
							.forEach(
									jsonValue -> System.out.println(String
											.format("    %s", jsonValue)));
				} else {
					System.out.println(String.format("response body = %s",
							result));
				}
			}
			assertTrue(Ints.contains(expectedStatus, resp.getStatus()));
			return result;
		} catch (ParseException e) {
			System.out.println(String.format("Response parse error [%s]",
					resp.getBody()));
			throw e;
		}
	}

	protected static void printHttpRequest(HttpRequest req,
			HttpResponse<String> resp) {
		System.out.println();
		System.out.println(String.format("%s %s => %s => %s",
				req.getHttpMethod(), req.getUrl(), resp.getStatus(),
				resp.getStatusText()));

		req.getHeaders().forEach(
				(key, value) -> System.out.println(String.format("%s : %s",
						key, value)));
	}

	static <T> void print(RequestBodyEntity request, HttpResponse<T> response) {
		print(request.getHttpRequest(), response);
		System.out
				.println(String.format("request body = %s", request.getBody()));
	}

	static <T> void print(HttpRequest request, HttpResponse<T> response) {
		System.out.println();
		System.out.println(String.format("%s %s => %s => %s",
				request.getHttpMethod(), request.getUrl(),
				response.getStatus(), response.getStatusText()));

		if (request.getHeaders().containsKey("x-magic-account-id")) {
			System.out.println("x-magic-account-id = "
					+ request.getHeaders().get("x-magic-account-id"));
		}
		if (request.getHeaders().containsKey("Authorization")) {
			System.out.println("Authorization = "
					+ request.getHeaders().get("Authorization"));
		}

		System.out.println(String.format("response body = %s",
				response.getBody()));
	}
}
