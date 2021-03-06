/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.http.SpaceHeaders;

public class CrossOriginFilterTest extends SpaceTest {

	@Test
	public void returnCORSHeaders() {

		prepareTest();
		clearServer();
		SpaceDog guest = SpaceDog.dog();

		// CORS for simple requests
		guest.get("/2/data").go(200).asVoid()//
				.assertHeaderEquals("*", SpaceHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)//
				.assertHeaderEquals(SpaceHeaders.ALLOW_METHODS, SpaceHeaders.ACCESS_CONTROL_ALLOW_METHODS)
				.assertHeaderContains(SpaceHeaders.AUTHORIZATION, SpaceHeaders.ACCESS_CONTROL_ALLOW_HEADERS)//
				.assertHeaderContains(SpaceHeaders.CONTENT_TYPE, SpaceHeaders.ACCESS_CONTROL_ALLOW_HEADERS)//
				.assertHeaderContains(SpaceHeaders.CONTENT_ENCODING, SpaceHeaders.ACCESS_CONTROL_ALLOW_HEADERS)//
				.assertHeaderContains(SpaceHeaders.SPACEDOG_DEBUG, SpaceHeaders.ACCESS_CONTROL_ALLOW_HEADERS);

		// CORS pre-flight request
		guest.options("/toto")//
				.setHeader(SpaceHeaders.ORIGIN, "https://app.toolee.fr")
				.setHeader(SpaceHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")//
				.go(200).asVoid()//
				.assertHeaderEquals("https://app.toolee.fr", SpaceHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)//
				.assertHeaderEquals(SpaceHeaders.ALLOW_METHODS, SpaceHeaders.ACCESS_CONTROL_ALLOW_METHODS)//
				.assertHeaderEquals(31536000, SpaceHeaders.ACCESS_CONTROL_MAX_AGE);
	}

}
