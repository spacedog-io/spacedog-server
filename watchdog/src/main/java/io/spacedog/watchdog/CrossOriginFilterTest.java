/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class CrossOriginFilterTest extends Assert {

	@Test
	public void returnCORSHeaders() throws Exception {

		SpaceDogHelper.prepareTest();
		Account testAccount = SpaceDogHelper.resetTestAccount();

		// CORS for simple requests

		com.mashape.unirest.http.Headers req1headers = SpaceRequest.get("/v1/data?refresh=true").backendKey(testAccount)
				.go(200).httpResponse().getHeaders();

		assertEquals("*", req1headers.getFirst(SpaceHeaders.ACCESS_CONTROL_ALLOW_ORIGIN.toLowerCase()));
		assertEquals(SpaceHeaders.ALLOW_METHODS,
				req1headers.getFirst(SpaceHeaders.ACCESS_CONTROL_ALLOW_METHODS.toLowerCase()));

		// CORS pre-flight request

		com.mashape.unirest.http.Headers req2headers = SpaceRequest.options("/v1/user/mynameisperson")
				.backendKey(testAccount).header(SpaceHeaders.ORIGIN, "http://www.apple.com")
				.header(SpaceHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT").go(200).httpResponse().getHeaders();

		assertEquals("http://www.apple.com",
				req2headers.getFirst(SpaceHeaders.ACCESS_CONTROL_ALLOW_ORIGIN.toLowerCase()));

		assertEquals(SpaceHeaders.ALLOW_METHODS,
				req2headers.getFirst(SpaceHeaders.ACCESS_CONTROL_ALLOW_METHODS.toLowerCase()));
		assertEquals("31536000", req2headers.getFirst(SpaceHeaders.ACCESS_CONTROL_MAX_AGE.toLowerCase()));
	}

}
