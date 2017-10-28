/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceEnv;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceTest;
import io.spacedog.model.CredentialsSettings;
import io.spacedog.model.CredentialsSettings.OAuthSettings;

public class Linkedin extends SpaceTest {

	// @Test
	public void getMyLinkedinProfil() {

		// prepare
		prepareTest();

		// get my profil
		SpaceRequest.get("/1/linkedin/me/firstName,picture-url,location,summary")//
				.bearerAuth("XXXXXXXXXXXX")//
				.backend("test")//
				.go(200);
	}

	@Test
	public void login() //
			throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// credentials settings with guest sign up enabled
		CredentialsSettings settings = defaultCredentialsSettings(superadmin, false);
		superadmin.settings().save(settings);

		// login succeeds
		linkedinLogin(superadmin, false, settings);
	}

	@Test
	public void loginWithSpecificSessionMaximumLifetime() //
			throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// credentials settings with guest sign up enabled
		CredentialsSettings settings = defaultCredentialsSettings(superadmin, false);
		settings.linkedin.useExpiresIn = false;
		settings.sessionMaximumLifetime = 10000;
		superadmin.settings().save(settings);

		// expiresIn is between 9000 and 10000
		linkedinLogin(superadmin, false, settings);
	}

	@Test
	public void loginFailsCauseGuestSignUpIsDisabled() //
			throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// set credentials settings with guest sign up disabled
		CredentialsSettings settings = defaultCredentialsSettings(superadmin, true);
		superadmin.settings().save(settings);

		// login fails since guest sign in is disabled
		// and credentials has not been pre created by admin
		linkedinLogin(superadmin, false, settings);
	}

	@Test
	public void loginSucceedsCauseCredentialsPreRegisteredByAdmin() //
			throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// set credentials settings with guest sign up disabled
		CredentialsSettings settings = defaultCredentialsSettings(superadmin, true);
		superadmin.settings().save(settings);

		// admin pre registers some credentials for a new user
		// username must equals the linkedin account email
		superadmin.post("/1/credentials")//
				.bodyJson("username", "attias666@gmail.com", "email", "attias666@gmail.com")//
				.go(201).assertPresent("passwordResetCode");

		// login succeeds
		// check credentialsId in json payload is identical
		// to credentialsId in previous request before login
		linkedinLogin(superadmin, false, settings);
	}

	@Test
	public void loginFailsCauseInvalidSecret() //
			throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// set credentials settings with guest sign up disabled
		CredentialsSettings settings = defaultCredentialsSettings(superadmin, true);
		settings.linkedin.clientSecret = "XXX";
		superadmin.settings().save(settings);

		// login fails since secret is invalid
		// check error in response json payload
		linkedinLogin(superadmin, false, settings);
	}

	@Test
	public void redirectedLogin() throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// credentials settings with guest sign up enabled
		CredentialsSettings settings = defaultCredentialsSettings(superadmin, false);
		superadmin.settings().save(settings);

		// login succeeds
		// check access token in redirect url params
		linkedinLogin(superadmin, true, settings);
	}

	@Test
	public void redirectedLoginFailsCauseInvalidSecret() //
			throws URISyntaxException, IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// set credentials settings with guest sign up disabled
		CredentialsSettings settings = defaultCredentialsSettings(superadmin, true);
		settings.linkedin.clientSecret = "XXX";
		superadmin.settings().save(settings);

		// login fails since secret is invalid
		// check error in redirect url params
		linkedinLogin(superadmin, true, settings);
	}

	private void linkedinLogin(SpaceDog dog, boolean redirect, CredentialsSettings settings)
			throws URISyntaxException, IOException {

		// test redirect uri
		String redirectUri = dog.backend().url("/1/login/linkedin");

		if (redirect)
			redirectUri = redirectUri + "/redirect";

		// build the linkedin oauth2 signin url
		URIBuilder url = new URIBuilder("https://www.linkedin.com/oauth/v2/authorization");
		url.addParameter("response_type", "code");
		url.addParameter("state", "thisIsSpaceDog");
		url.addParameter("redirect_uri", redirectUri);
		url.addParameter("client_id", settings.linkedin.clientId);

		// open a browser with the linkedin signin url
		// to start the linkedin oauth2 authentication process
		Runtime.getRuntime().exec("open " + url.toString());
	}

	private CredentialsSettings defaultCredentialsSettings(//
			SpaceDog dog, boolean disableGuestSignUp) {

		SpaceEnv env = SpaceEnv.defaultEnv();
		CredentialsSettings settings = new CredentialsSettings();
		settings.guestSignUpEnabled = !disableGuestSignUp;
		settings.linkedin = new OAuthSettings();
		settings.linkedin.backendUrl = "https://www.linkedin.com";
		settings.linkedin.clientId = env.getOrElseThrow("spacedog.test.linkedin.client.id");
		settings.linkedin.clientSecret = env.getOrElseThrow("spacedog.test.linkedin.client.secret");
		settings.linkedin.finalRedirectUri = dog.backend().url();
		return settings;
	}
}
