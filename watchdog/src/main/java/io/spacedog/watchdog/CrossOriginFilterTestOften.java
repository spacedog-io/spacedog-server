/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.SpaceHeaders;

public class CrossOriginFilterTestOften extends SpaceTest {

	@Test
	public void returnCORSHeaders() {

		prepareTest();
		SpaceDog test = resetTestBackend();

		// CORS for simple requests

		SpaceRequest.get("/1/data").refresh().backend(test).go(200)//
				.assertHeaderEquals("*", SpaceHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)//
				.assertHeaderEquals(SpaceHeaders.ALLOW_METHODS, SpaceHeaders.ACCESS_CONTROL_ALLOW_METHODS);

		// TODO make this work
		// Unirest seems to have bugs in getting header value as list

		// .assertHeaderEquals(SpaceHeaders.AUTHORIZATION,
		// SpaceHeaders.ACCESS_CONTROL_ALLOW_HEADERS)//
		// .assertHeaderContains(SpaceHeaders.CONTENT_TYPE,
		// SpaceHeaders.ACCESS_CONTROL_ALLOW_HEADERS)//
		// .assertHeaderContains(SpaceHeaders.SPACEDOG_TEST,
		// SpaceHeaders.ACCESS_CONTROL_ALLOW_HEADERS);

		// CORS pre-flight request

		SpaceRequest.options("/v1/user/mynameisperson").backend(test)
				.setHeader(SpaceHeaders.ORIGIN, "http://www.apple.com")
				.setHeader(SpaceHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")//
				.go(200)//
				.assertHeaderEquals("http://www.apple.com", SpaceHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)//
				.assertHeaderEquals(SpaceHeaders.ALLOW_METHODS, SpaceHeaders.ACCESS_CONTROL_ALLOW_METHODS)//
				.assertHeaderEquals("31536000", SpaceHeaders.ACCESS_CONTROL_MAX_AGE);
	}

}
