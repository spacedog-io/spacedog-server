/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.junit.Test;

import io.spacedog.model.CredentialsSettings;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class Linkedin extends SpaceTest {

	// @Test
	public void getMyLinkedinProfil() {

		// prepare
		prepareTest();

		// get my profil
		SpaceRequest.get("/1/linkedin/people/me/firstName,picture-url,location,summary")//
				.bearerAuth("XXXXXXXXXXXX")//
				.backend("test")//
				.go(200);
	}

	@Test
	public void login() //
			throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// credentials settings with guest sign up enabled
		CredentialsSettings settings = defaultCredentialsSettings(false, test);
		test.settings().save(settings);

		// login succeeds
		linkedinLogin(test, false, settings);
	}

	@Test
	public void loginWithSpecificSessionMaximumLifetime() //
			throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// credentials settings with guest sign up enabled
		CredentialsSettings settings = defaultCredentialsSettings(false, test);
		settings.useLinkedinExpiresIn = false;
		settings.sessionMaximumLifetime = 10000;
		test.settings().save(settings);

		// expiresIn is between 9000 and 10000
		linkedinLogin(test, false, settings);
	}

	@Test
	public void loginFailsCauseGuestSignUpIsDisabled() //
			throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// set credentials settings with guest sign up disabled
		CredentialsSettings settings = defaultCredentialsSettings(true, test);
		test.settings().save(settings);

		// login fails since guest sign in is disabled
		// and credentials has not been pre created by admin
		linkedinLogin(test, false, settings);
	}

	@Test
	public void loginSucceedsCauseCredentialsPreRegisteredByAdmin() //
			throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// set credentials settings with guest sign up disabled
		CredentialsSettings settings = defaultCredentialsSettings(true, test);
		test.settings().save(settings);

		// admin pre registers some credentials for a new user
		// username must equals the linkedin account email
		SpaceRequest.post("/1/credentials").auth(test)//
				.body("username", "attias666@gmail.com", "email", "attias666@gmail.com")//
				.go(201).assertPresent("passwordResetCode");

		// login succeeds
		// check credentialsId in json payload is identical
		// to credentialsId in previous request before login
		linkedinLogin(test, false, settings);
	}

	@Test
	public void loginFailsCauseInvalidSecret() //
			throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// set credentials settings with guest sign up disabled
		CredentialsSettings settings = defaultCredentialsSettings(true, test);
		settings.linkedinSecret = "XXX";
		test.settings().save(settings);

		// login fails since secret is invalid
		// check error in response json payload
		linkedinLogin(test, false, settings);
	}

	@Test
	public void redirectedLogin() throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// credentials settings with guest sign up enabled
		CredentialsSettings settings = defaultCredentialsSettings(false, test);
		test.settings().save(settings);

		// login succeeds
		// check access token in redirect url params
		linkedinLogin(test, true, settings);
	}

	@Test
	public void redirectedLoginFailsCauseInvalidSecret() //
			throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// set credentials settings with guest sign up disabled
		CredentialsSettings settings = defaultCredentialsSettings(true, test);
		settings.linkedinSecret = "XXX";
		test.settings().save(settings);

		// login fails since secret is invalid
		// check error in redirect url params
		linkedinLogin(test, true, settings);
	}

	private void linkedinLogin(SpaceDog backend, boolean redirect, CredentialsSettings settings)
			throws URISyntaxException, IOException {

		// test redirect uri
		String redirectUri = SpaceRequest.env().target()//
				.url(backend.backendId(), "/1/login/linkedin");

		if (redirect)
			redirectUri = redirectUri + "/redirect";

		// build the linkedin oauth2 signin url
		URIBuilder url = new URIBuilder("https://www.linkedin.com/oauth/v2/authorization");
		url.addParameter("response_type", "code");
		url.addParameter("state", "thisIsSpaceDog");
		url.addParameter("redirect_uri", redirectUri);
		url.addParameter("client_id", settings.linkedinId);

		// open a browser with the linkedin signin url
		// to start the linkedin oauth2 authentication process
		Runtime.getRuntime().exec("open " + url.toString());
	}

	private CredentialsSettings defaultCredentialsSettings(boolean disableGuestSignUp, SpaceDog backend) {
		CredentialsSettings settings = new CredentialsSettings();
		settings.disableGuestSignUp = disableGuestSignUp;
		settings.linkedinId = SpaceRequest.env().get("spacedog.test.linkedin.client.id");
		settings.linkedinSecret = SpaceRequest.env().get("spacedog.test.linkedin.client.secret");
		settings.linkedinFinalRedirectUri = SpaceRequest.env().target().url(backend.backendId());
		return settings;
	}
}
