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

		// credentials settings with guest sign up enabled
		CredentialsSettings settings = setCredentialsSettings(test, false, false);

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
	public void getMyLinkedinProfil() {

		// prepare
		SpaceClient.prepareTest();

		// get my profil
		SpaceRequest.get("/1/linkedin/people/me/firstName,picture-url,location,summary")//
				.bearerAuth("XXXXXXXXXXXX")//
				.backendId("test")//
				.go(200);
	}

	@Test
	public void linkedinLoginWITHPreRegisteredCredentialsByAdmin() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// set credentials settings with guest sign up disabled
		CredentialsSettings settings = setCredentialsSettings(test, true, false);

		// admin pre registers some credentials for a new user
		// username must equals the linkedin account email
		String resetCode = SpaceRequest.post("/1/credentials").adminAuth(test)//
				.body("username", "attias666@gmail.com", "email", "attias666@gmail.com")//
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

	@Test
	public void linkedinRedirectedLogin() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// credentials settings with guest sign up enabled
		// and redirected login url
		CredentialsSettings settings = setCredentialsSettings(test, false, true);

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

	private CredentialsSettings setCredentialsSettings(Backend backend, boolean disableGuestSignUp,
			boolean redirectLogin) {
		CredentialsSettings settings = new CredentialsSettings();

		settings.disableGuestSignUp = disableGuestSignUp;
		settings.linkedinId = SpaceRequest.configuration().testLinkedinClientId();
		settings.linkedinSecret = SpaceRequest.configuration().testLinkedinClientSecret();
		settings.linkedinRedirectUri = SpaceRequest.configuration().target()//
				.url(backend.backendId, "/1/login/linkedin");

		if (redirectLogin)
			settings.linkedinRedirectUri = settings.linkedinRedirectUri + "/redirect";

		SpaceClient.saveSettings(backend, settings);
		return settings;
	}
}
