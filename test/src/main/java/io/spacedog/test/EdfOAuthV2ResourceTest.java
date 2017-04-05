/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.CredentialsSettings;
import io.spacedog.model.CredentialsSettings.OAuthSettings;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Credentials;

public class EdfOAuthV2ResourceTest extends SpaceTest {

	private static final String EDF_BASE_URL = "https://paas-pp.edf.fr";

	private static final String redirectUri = EDF_BASE_URL //
			+ "/gardian/oauth2/v2/redirect";

	@Test
	public void spacedogAuthenticationWithEdfOAuthV2() {

		// prepare without any test header because EDF OAuth
		// endpoint doesn't like any unspecified header
		prepareTest(false);
		SpaceDog test = resetTestBackend();

		// super admin sets credentials oauth settings
		OAuthSettings oauth = new OAuthSettings();
		oauth.clientId = SpaceRequest.env().get("edf.oauth.client.id");
		oauth.clientSecret = SpaceRequest.env().get("edf.oauth.client.secret");
		oauth.useExpiresIn = true;

		CredentialsSettings settings = new CredentialsSettings();
		settings.oauth = oauth;
		test.settings().save(settings);

		// digital01 gets an authorization code from EDF
		SpaceDog digital01 = SpaceDog.backend(test).username("DIGITAL01");

		String code = SpaceRequest.post("/gardian/oauth2/v2/approval")//
				.baseUrl(EDF_BASE_URL)//
				.basicAuth("DIGITAL01", "DIGITAL01")//
				.body("client_id", oauth.clientId, "response_type", "code", //
						"redirect_uri", redirectUri, "approved", true, //
						"scope", "1001Espaces")//
				.go(200).getString("code");

		// digital01 gets an access token from SpaceDog
		ObjectNode node = SpaceRequest.post("/1/service/oauth/v2/accessToken")//
				.backend(test)//
				.formField("code", code)//
				.formField("redirect_uri", redirectUri)//
				.go(200).objectNode();

		digital01.id(node.get("credentials").get("id").asText());
		digital01.accessToken(node.get("accessToken").asText());

		// digital01 checks his access token is working fine
		// by getting his credentials
		Credentials credentials = digital01.credentials()//
				.getByUsername(digital01.username()).get();
		assertEquals(digital01.id(), credentials.id());
		assertEquals(digital01.username(), credentials.name());
	}

	@Test
	public void directAuthenticationWithEdfOAuthV2() {

		// prepare
		OAuthSettings oauth = new OAuthSettings();
		oauth.clientId = SpaceRequest.env().get("edf.oauth.v2.client.id");
		oauth.clientSecret = SpaceRequest.env().get("edf.oauth.v2.client.secret");

		// edf user gets authorization scopes from EDF
		SpaceRequest.post("/gardian/oauth2/v2/authorization")//
				.baseUrl(EDF_BASE_URL)//
				.body("client_id", oauth.clientId, "response_type", "code", //
						"redirect_uri", redirectUri)//
				.go(200);

		// edf user gets authorization code from EDF
		String code = SpaceRequest.post("/gardian/oauth2/v2/approval")//
				.baseUrl(EDF_BASE_URL)//
				.basicAuth("DIGITAL01", "DIGITAL01")//
				.body("client_id", oauth.clientId, "response_type", "code", //
						"redirect_uri", redirectUri, "approved", true, //
						"scope", "1001Espaces")//
				.go(200)//
				.assertNotNull("code")//
				.getString("code");

		// edf user gets an access token from EDF
		String accessToken = SpaceRequest.post("/gardian/oauth2/v2/token")//
				.baseUrl(EDF_BASE_URL)//
				.basicAuth(oauth.clientId, oauth.clientSecret)//
				.body("grant_type", "authorization_code", "code", code, //
						"client_id", oauth.clientId, "redirect_uri", redirectUri)//
				.go(200)//
				.assertNotNull("access_token")//
				.assertNotNull("expires_in")//
				.getString("access_token");

		// edf user gets an token info from EDF
		SpaceRequest.get("/gardian/oauth2/v2/tokeninfo")//
				.baseUrl(EDF_BASE_URL)//
				.bearerAuth(accessToken)//
				.go(200)//
				.assertNotNullOrEmpty("uid");
	}

}
