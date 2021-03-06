/**
 * © David Attias 2015
 */
package io.spacedog.test.credentials;

import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.CredentialsSettings;
import io.spacedog.client.credentials.CredentialsSettings.OAuthSettings;
import io.spacedog.client.http.SpaceEnv;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.test.SpaceTest;

public class LinkedinRestyTest extends SpaceTest {

	@Test
	public void login() {

		// prepare
		prepareTest();
		SpaceEnv env = SpaceEnv.env();
		SpaceDog superadmin = clearServer();
		String redirectUri = superadmin.backend().url("/2/login/linkedin");

		// no linkedin settings means no linkedin credentials
		SpaceRequest.post("/2/login/linkedin")//
				.backend(superadmin.backend())//
				.formField("code", "XXX")//
				.formField("redirect_uri", "XXX")//
				.go(400).asVoid();

		// admin sets linkedin settings without redirect uri
		CredentialsSettings settings = new CredentialsSettings();
		settings.linkedin = new OAuthSettings();
		settings.linkedin.backendUrl = "https://www.linkedin.com";
		settings.linkedin.clientId = env.getOrElseThrow("spacedog.test.linkedin.client.id");
		settings.linkedin.clientSecret = env.getOrElseThrow("spacedog.test.linkedin.client.secret");
		superadmin.credentials().settings(settings);

		// fails to create linkedin credentials if no authorization code
		SpaceRequest.post("/2/login/linkedin")//
				.backend(superadmin.backend())//
				.formField("redirect_uri", redirectUri)//
				.go(400).asVoid();

		// fails to create linkedin credentials if invalid redirect_uri
		SpaceRequest.post("/2/login/linkedin")//
				.backend(superadmin.backend())//
				.formField("code", "XXX")//
				.formField("redirect_uri", "XXX")//
				.go(401).asVoid();

		// fails to create linkedin credentials if invalid code
		SpaceRequest.post("/2/login/linkedin")//
				.backend(superadmin.backend())//
				.formField("code", "XXX")//
				.formField("redirect_uri", redirectUri)//
				.go(401).asVoid();

		// fails to create linkedin credentials if invalid code
		// if no redirect_uri parameter is set spacedog uses
		// its default redirect uri
		SpaceRequest.post("/2/login/linkedin")//
				.backend(superadmin.backend())//
				.formField("code", "XXX")//
				.go(401).asVoid();
	}
}
