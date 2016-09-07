/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.CredentialsSettings;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class LinkedinResourceTestOften extends Assert {

	@Test
	public void linkedinSignUp() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		String redirectUri = SpaceRequest.configuration().target()//
				.url(test.backendId, "/1/credentials/linkedin");

		// no linkedin settings means no linkedin credentials
		SpaceRequest.post("/1/credentials/linkedin")//
				.formField("code", "XXX")//
				.formField("redirect_uri", "XXX")//
				.backend(test).go(400);

		// admin sets linkedin settings without redirect uri
		CredentialsSettings settings = new CredentialsSettings();
		settings.linkedinId = SpaceRequest.configuration().linkedinClientId();
		settings.linkedinSecret = SpaceRequest.configuration().linkedinClientSecret();
		SpaceClient.saveSettings(test, settings);

		// fails to create linkedin credentials if no authorization code
		SpaceRequest.post("/1/credentials/linkedin")//
				.formField("redirect_uri", redirectUri).backend(test).go(400);

		// fails to create linkedin credentials if no redirect_uri
		// in parameters nor settings
		SpaceRequest.post("/1/credentials/linkedin")//
				.formField("code", "XXX").backend(test).go(400);

		// fails to create linkedin credentials if invalid code
		SpaceRequest.post("/1/credentials/linkedin").backend(test)//
				.formField("code", "XXX")//
				.formField("redirect_uri", redirectUri)//
				.go(401);

		// admin sets linkedin settings with redirect uri
		settings.linkedinRedirectUri = redirectUri;
		SpaceClient.saveSettings(test, settings);

		// fails to create linkedin credentials if invalid code
		// redirectUri is not necessary because found in settings
		SpaceRequest.post("/1/credentials/linkedin")//
				.formField("code", "XXX").backend(test).go(401);
	}
}
