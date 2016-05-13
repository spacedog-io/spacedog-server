/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceRequestConfiguration;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class HttpPermanentRedirectTest extends Assert {

	@Test
	public void beRedirectedToMainPort() throws Exception {

		SpaceClient.prepareTest();

		// should redirect from http to https and get the root page

		SpaceRequest.get("/").redirected(true).go(301)//
				.assertHeaderEquals(SpaceRequestConfiguration.get().target().url("/"), "location");
	}
}