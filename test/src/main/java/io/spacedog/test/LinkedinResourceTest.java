/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import io.spacedog.model.CredentialsSettings;
import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class LinkedinResourceTest extends SpaceTest {

	@Test
	public void login() {

		// prepare
		prepareTest();
		SpaceEnv env = SpaceEnv.defaultEnv();
		SpaceDog test = resetTestBackend();
		String redirectUri = env.target().url(test.backendId(), "/1/login/linkedin");

		// no linkedin settings means no linkedin credentials
		SpaceRequest.post("/1/login/linkedin").backend(test)//
				.formField("code", "XXX")//
				.formField("redirect_uri", "XXX")//
				.go(400);

		// admin sets linkedin settings without redirect uri
		CredentialsSettings settings = new CredentialsSettings();
		settings.linkedinId = env.get("spacedog.test.linkedin.client.id");
		settings.linkedinSecret = env.get("spacedog.test.linkedin.client.secret");
		test.settings().save(settings);

		// fails to create linkedin credentials if no authorization code
		SpaceRequest.post("/1/login/linkedin").backend(test)//
				.formField("redirect_uri", redirectUri)//
				.go(400);

		// fails to create linkedin credentials if invalid redirect_uri
		SpaceRequest.post("/1/login/linkedin").backend(test)//
				.formField("code", "XXX")//
				.formField("redirect_uri", "XXX")//
				.go(401);

		// fails to create linkedin credentials if invalid code
		SpaceRequest.post("/1/login/linkedin").backend(test)//
				.formField("code", "XXX")//
				.formField("redirect_uri", redirectUri)//
				.go(400);

		// fails to create linkedin credentials if invalid code
		// if no redirect_uri parameter is set spacedog uses
		// its default redirect uri
		SpaceRequest.post("/1/login/linkedin").backend(test)//
				.formField("code", "XXX")//
				.go(400);
	}
}
