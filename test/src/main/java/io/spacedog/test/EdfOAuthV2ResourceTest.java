/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.CredentialsSettings;
import io.spacedog.model.CredentialsSettings.OAuthSettings;
import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Json7;

public class EdfOAuthV2ResourceTest extends SpaceTest {

	private static final String redirectUri = "https://paas-pp.edf.fr" //
			+ "/gardian/oauth2/v2/redirect";

	@Test
	public void spacedogAuthenticationWithEdfOAuthV2() {

		// prepare without any test header because EDF OAuth
		// endpoint doesn't like any unspecified header
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend("test");
		SpaceDog digital01 = SpaceDog.backend("test").username("DIGITAL01");

		// digital01 gets an access token from SpaceDog
		guest.post("/1/service/oauth/v2/accessToken").go(400);

		// super admin sets credentials oauth settings
		OAuthSettings oauth = new OAuthSettings();
		oauth.clientId = SpaceEnv.defaultEnv().get("edf.oauth.v2.client.id");
		oauth.clientSecret = SpaceEnv.defaultEnv().get("edf.oauth.v2.client.secret");
		oauth.backendUrl = "https://paas-pp.edf.fr";
		oauth.useExpiresIn = true;

		CredentialsSettings settings = new CredentialsSettings();
		settings.oauth = oauth;
		superadmin.settings().save(settings);

		// digital01 gets an authorization code from EDF
		String code = SpaceRequest.post("/gardian/oauth2/v2/approval")//
				.backend(oauth.backendUrl)//
				.basicAuth("DIGITAL01", "DIGITAL01")//
				.bodyJson("client_id", oauth.clientId, "response_type", "code", //
						"redirect_uri", redirectUri, "approved", true, //
						"scope", "1001Espaces")//
				.go(200).getString("code");

		// digital01 fails to get an access token from SpaceDog
		// if code or redirect_uri are null or invalid
		guest.post("/1/service/oauth/v2/accessToken")//
				.formField("code", code).go(400);
		guest.post("/1/service/oauth/v2/accessToken")//
				.formField("redirect_uri", redirectUri).go(400);
		guest.post("/1/service/oauth/v2/accessToken")//
				.formField("code", "XXX").formField("redirect_uri", redirectUri).go(400)//
				.assertEquals("edf-oauth-error", "error.code");
		guest.post("/1/service/oauth/v2/accessToken")//
				.formField("code", code).formField("redirect_uri", "XXX").go(400)//
				.assertEquals("edf-oauth-error", "error.code");

		// digital01 gets an access token from SpaceDog
		ObjectNode node = guest.post("/1/service/oauth/v2/accessToken")//
				.formField("code", code)//
				.formField("redirect_uri", redirectUri)//
				.go(200)//
				.assertNotNull("accessToken")//
				.assertNotNull("expiresIn")//
				.assertEquals("test", "credentials.backendId")//
				.assertEquals("DIGITAL01", "credentials.username")//
				.assertEquals(Json7.array("user"), "credentials.roles")//
				.assertNotPresent("credentials.email")//
				.asJsonObject();

		digital01.id(node.get("credentials").get("id").asText());
		digital01.accessToken(node.get("accessToken").asText());

		// digital01 checks his access token is working fine
		// by getting his own credentials
		Credentials credentials = digital01.credentials()//
				.getByUsername(digital01.username()).get();
		assertEquals(digital01.id(), credentials.id());
		assertEquals(digital01.username(), credentials.name());
	}

	@Test
	public void directAuthenticationWithEdfOAuthV2() {

		// prepare
		OAuthSettings oauth = new OAuthSettings();
		oauth.clientId = SpaceEnv.defaultEnv().get("edf.oauth.v2.client.id");
		oauth.clientSecret = SpaceEnv.defaultEnv().get("edf.oauth.v2.client.secret");
		oauth.backendUrl = "https://paas-pp.edf.fr";

		// edf user gets authorization scopes from EDF
		SpaceRequest.post("/gardian/oauth2/v2/authorization")//
				.backend(oauth.backendUrl)//
				.bodyJson("client_id", oauth.clientId, "response_type", "code", //
						"redirect_uri", redirectUri)//
				.go(200);

		// edf user gets authorization code from EDF
		String code = SpaceRequest.post("/gardian/oauth2/v2/approval")//
				.backend(oauth.backendUrl)//
				.basicAuth("DIGITAL01", "DIGITAL01")//
				.bodyJson("client_id", oauth.clientId, "response_type", "code", //
						"redirect_uri", redirectUri, "approved", true, //
						"scope", "1001Espaces")//
				.go(200)//
				.assertNotNull("code")//
				.getString("code");

		// edf user gets an access token from EDF
		String accessToken = SpaceRequest.post("/gardian/oauth2/v2/token")//
				.backend(oauth.backendUrl)//
				.basicAuth(oauth.clientId, oauth.clientSecret)//
				.bodyJson("grant_type", "authorization_code", "code", code, //
						"client_id", oauth.clientId, "redirect_uri", redirectUri)//
				.go(200)//
				.assertNotNull("access_token")//
				.assertNotNull("expires_in")//
				.getString("access_token");

		// edf user gets an token info from EDF
		SpaceRequest.get("/gardian/oauth2/v2/tokeninfo")//
				.backend(oauth.backendUrl)//
				.bearerAuth(accessToken)//
				.go(200)//
				.assertNotNullOrEmpty("uid");
	}

}
