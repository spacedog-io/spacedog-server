/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;

import net.codestory.http.constants.Headers;

public class CrossOriginFilterTest extends AbstractTest {

	@BeforeClass
	public static void resetTestAccount() throws UnirestException, InterruptedException, IOException {
		AdminResourceTest.resetTestAccount();
	}

	@Test
	public void shouldReturnCORSHeaders() throws Exception {

		// CORS for simple requests

		GetRequest req1 = prepareGet("/v1/data", AdminResourceTest.testClientKey());
		Result res1 = get(req1, 200);

		assertEquals("*", res1.response().getHeaders().getFirst(Headers.ACCESS_CONTROL_ALLOW_ORIGIN.toLowerCase()));
		assertEquals(CrossOriginFilter.ALLOW_METHODS,
				res1.response().getHeaders().getFirst(Headers.ACCESS_CONTROL_ALLOW_METHODS.toLowerCase()));

		// CORS pre-flight request

		HttpRequestWithBody req2 = prepareOptions("/v1/user/mynameisperson", AdminResourceTest.testClientKey())
				.header(Headers.ORIGIN, "http://www.apple.com").header(Headers.ACCESS_CONTROL_REQUEST_METHOD, "PUT");

		Result res2 = options(req2, 200);

		assertEquals("http://www.apple.com",
				res2.response().getHeaders().getFirst(Headers.ACCESS_CONTROL_ALLOW_ORIGIN.toLowerCase()));

		assertEquals(CrossOriginFilter.ALLOW_METHODS,
				res2.response().getHeaders().getFirst(Headers.ACCESS_CONTROL_ALLOW_METHODS.toLowerCase()));
		assertEquals("31536000", res2.response().getHeaders().getFirst(Headers.ACCESS_CONTROL_MAX_AGE.toLowerCase()));

	}

}
