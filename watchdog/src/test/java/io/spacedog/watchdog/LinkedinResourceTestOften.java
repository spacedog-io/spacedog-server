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
	public void login() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		String redirectUri = SpaceRequest.configuration().target()//
				.url(test.backendId, "/1/login/linkedin");

		// no linkedin settings means no linkedin credentials
		SpaceRequest.post("/1/login/linkedin").backend(test)//
				.formField("code", "XXX")//
				.formField("redirect_uri", "XXX")//
				.go(400);

		// admin sets linkedin settings without redirect uri
		CredentialsSettings settings = new CredentialsSettings();
		settings.linkedinId = SpaceRequest.configuration().testLinkedinClientId();
		settings.linkedinSecret = SpaceRequest.configuration().testLinkedinClientSecret();
		SpaceClient.saveSettings(test, settings);

		// fails to create linkedin credentials if no authorization code
		SpaceRequest.post("/1/login/linkedin").backend(test)//
				.formField("redirect_uri", redirectUri)//
				.go(400);

		// fails to create linkedin credentials if invalid redirect_uri
		SpaceRequest.post("/1/login/linkedin").backend(test)//
				.formField("code", "XXX")//
				.formField("redirect_uri", "XXX")//
				.go(400);

		// fails to create linkedin credentials if invalid code
		SpaceRequest.post("/1/login/linkedin").backend(test)//
				.formField("code", "XXX")//
				.formField("redirect_uri", redirectUri)//
				.go(401);

		// fails to create linkedin credentials if invalid code
		// if no redirect_uri parameter is set spacedog uses
		// its default redirect uri
		SpaceRequest.post("/1/login/linkedin").backend(test)//
				.formField("code", "XXX")//
				.go(401);
	}
}
