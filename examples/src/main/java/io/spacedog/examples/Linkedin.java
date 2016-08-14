/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.CredentialsSettings;

public class Linkedin extends SpaceClient {

	@Test
	public void linkedinLoginWITHOUTPreRegisteredCredentialsByAdmin() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// credentials settings with guest sign up disabled
		CredentialsSettings settings = setCredentialsSettings(test, true);

		// start linkedin authentication process to display
		// url to call inside the browser for testing

		SpaceRequest//
				.get("https://www.linkedin.com/oauth/v2/authorization")//
				.queryParam("response_type", "code")//
				.queryParam("state", "thisIsSpaceDog")//
				.queryParam("redirect_uri", settings.linkedinRedirectUri)//
				.queryParam("client_id", settings.linkedinId)//
				.go(200, 303);
	}

	@Test
	public void linkedinLoginWITHPreRegisteredCredentialsByAdmin() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// set credentials settings with guest sign up disabled
		CredentialsSettings settings = setCredentialsSettings(test, true);

		// admin pre registers some credentials for a new user
		String resetCode = SpaceRequest.post("/1/credentials").adminAuth(test)//
				.body("username", "dave", "email", "attias666@gmail.com")//
				.go(201).getString("passwordRestCode");

		// start linkedin authentication process to display
		// url to call inside the browser for testing

		SpaceRequest//
				.get("https://www.linkedin.com/oauth/v2/authorization")//
				.queryParam("response_type", "code")//
				.queryParam("state", resetCode)//
				.queryParam("redirect_uri", settings.linkedinRedirectUri)//
				.queryParam("client_id", settings.linkedinId)//
				.go(200, 303);
	}

	private CredentialsSettings setCredentialsSettings(Backend backend, boolean disableGuestSignUp) {
		CredentialsSettings settings = new CredentialsSettings();

		settings.disableGuestSignUp = disableGuestSignUp;
		settings.linkedinId = "78uk3jfazu0wj2";
		settings.linkedinSecret = "42AfVLDNEXtgO9CG";
		settings.linkedinRedirectUri = SpaceRequest.configuration().target()//
				.url(backend.backendId, "/1/login/linkedin");

		SpaceClient.saveSettings(backend, settings);
		return settings;
	}
}
