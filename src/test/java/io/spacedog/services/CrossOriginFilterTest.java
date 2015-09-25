package io.spacedog.services;

import io.spacedog.services.CrossOriginFilter;
import net.codestory.http.constants.Headers;

import org.junit.BeforeClass;
import org.junit.Test;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;

public class CrossOriginFilterTest extends AbstractTest {

	@BeforeClass
	public static void resetTestAccount() throws UnirestException,
			InterruptedException {
		AccountResourceTest.resetTestAccount();
	}

	@Test
	public void shouldReturnCORSHeaders() throws Exception {

		// CORS for simple requests

		GetRequest req1 = Unirest.get("http://localhost:8080/v1/user")
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test");

		Result res1 = get(req1, 200);

		assertEquals(
				"*",
				res1.response()
						.getHeaders()
						.getFirst(
								Headers.ACCESS_CONTROL_ALLOW_ORIGIN
										.toLowerCase()));
		assertEquals(
				CrossOriginFilter.ALLOW_METHODS,
				res1.response()
						.getHeaders()
						.getFirst(
								Headers.ACCESS_CONTROL_ALLOW_METHODS
										.toLowerCase()));

		// CORS pre-flight request

		HttpRequestWithBody req2 = Unirest
				.options("http://localhost:8080/v1/user/mynameisperson")
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test")
				.header(Headers.ORIGIN, "http://www.apple.com")
				.header(Headers.ACCESS_CONTROL_REQUEST_METHOD, "PUT");

		Result res2 = options(req2, 200);

		assertEquals("http://www.apple.com", res2.response().getHeaders()
				.getFirst(Headers.ACCESS_CONTROL_ALLOW_ORIGIN.toLowerCase()));

		assertEquals(
				CrossOriginFilter.ALLOW_METHODS,
				res2.response()
						.getHeaders()
						.getFirst(
								Headers.ACCESS_CONTROL_ALLOW_METHODS
										.toLowerCase()));
		assertEquals(
				"31536000",
				res2.response().getHeaders()
						.getFirst(Headers.ACCESS_CONTROL_MAX_AGE.toLowerCase()));

	}

}
