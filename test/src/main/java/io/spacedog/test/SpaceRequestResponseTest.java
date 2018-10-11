/**
 * © David Attias 2015
 */
package io.spacedog.test;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.utils.Utils;

public class SpaceRequestResponseTest extends SpaceTest {

	@Test
	public void testHeadersIgnoreCase() {

		prepareTest();
		clearServer();
		SpaceDog guest = SpaceDog.dog();

		// CORS for simple requests
		SpaceResponse response = guest.get("/2/data").go(200)//
				.assertHeaderEquals("GET, POST, PUT, DELETE, HEAD", SpaceHeaders.ACCESS_CONTROL_ALLOW_METHODS)
				.assertHeaderEquals("get, POST, put, DELETE, HEAD", SpaceHeaders.ACCESS_CONTROL_ALLOW_METHODS)//
				.assertHeaderContains("authorization", SpaceHeaders.ACCESS_CONTROL_ALLOW_HEADERS)//
				.assertHeaderContains("AuthoriZation", SpaceHeaders.ACCESS_CONTROL_ALLOW_HEADERS);

		List<String> expected = Lists.newArrayList("get", "POST", "put", "DELETE", "HEAD");
		List<String> accessControlAllowMethods = response.headerAsList(SpaceHeaders.ACCESS_CONTROL_ALLOW_METHODS);
		assertTrue(Utils.equalsIgnoreCase(expected, accessControlAllowMethods));
	}

}
