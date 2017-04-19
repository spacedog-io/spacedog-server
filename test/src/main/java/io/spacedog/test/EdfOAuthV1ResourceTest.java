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
import io.spacedog.utils.SpaceHeaders;

public class EdfOAuthV1ResourceTest extends SpaceTest {

	private static final String redirectUri = //
			"https://noefy5jt.noe.edf.fr:5641" //
					+ "/invoke/PAAS_OAUTH.services:defaultRedirectURI";

	@Test
	public void testSpaceDogEdfOAuth() {

		// prepare without any test header because EDF OAuth
		// endpoint doesn't like any unspecified header
		prepareTest(false);
		SpaceDog test = resetTestBackend();

		// super admin sets credentials oauth settings
		OAuthSettings oauth = new OAuthSettings();
		oauth.clientId = SpaceEnv.defaultEnv().get("edf.oauth.v1.client.id");
		oauth.clientSecret = SpaceEnv.defaultEnv().get("edf.oauth.v1.client.secret");
		oauth.useExpiresIn = true;

		CredentialsSettings settings = new CredentialsSettings();
		settings.oauth = oauth;
		test.settings().save(settings);

		// USER01 gets an authorization code from EDF
		SpaceDog user01 = SpaceDog.backend(test).username("USER01");

		String code = SpaceRequest.post("/ws/iOAuthApprove/do")//
				.backend("https://noefy5jt.noe.edf.fr:5641")//
				.basicAuth(user01.username(), user01.username())//
				.setHeader(SpaceHeaders.ACCEPT, "application/json")//
				.formField("approved", "true")//
				.formField("scope", "1001Espaces")//
				.formField("client_id", oauth.clientId)//
				.formField("response_type", "code")//
				.formField("redirect_uri", redirectUri)//
				.go(200).getString("code");

		// USER01 gets an access token from SpaceDog
		ObjectNode node = SpaceRequest.post("/1/service/oauth/accessToken")//
				.backend(test)//
				.formField("code", code)//
				.formField("username", "USER01")//
				.formField("redirect_uri", redirectUri)//
				.go(200).asJsonObject();

		user01.id(node.get("credentials").get("id").asText());
		user01.accessToken(node.get("accessToken").asText());

		// USER01 checks his access token is working fine
		// by getting his credentials
		Credentials credentials = user01.credentials()//
				.getByUsername(user01.username()).get();
		assertEquals(user01.id(), credentials.id());
		assertEquals(user01.username(), credentials.name());
	}

	@Test
	public void testDirectEdfOAuth() {

		// prepare
		OAuthSettings oauth = new OAuthSettings();
		oauth.clientId = SpaceEnv.defaultEnv().get("edf.oauth.v1.client.id");
		oauth.clientSecret = SpaceEnv.defaultEnv().get("edf.oauth.v1.client.secret");

		// USER01 gets authorization code from EDF
		String code = SpaceRequest.post("/ws/iOAuthApprove/do")//
				.backend("https://noefy5jt.noe.edf.fr:5641")//
				.basicAuth("USER01", "USER01")//
				.setHeader(SpaceHeaders.ACCEPT, "application/json")//
				.formField("approved", "true")//
				.formField("scope", "1001Espaces")//
				.formField("client_id", oauth.clientId)//
				.formField("response_type", "code")//
				.formField("redirect_uri", redirectUri)//
				.go(200).getString("code");

		// USER01 gets an access token from EDF
		SpaceRequest.post("/ws/iOAuthGetToken/do")//
				.backend("https://noefy5jt.noe.edf.fr:5641")//
				.basicAuth(oauth.clientId, oauth.clientSecret)//
				.setHeader(SpaceHeaders.ACCEPT, "application/json")//
				.bodyJson("grant_type", "authorization_code", "code", code, //
						"client_id", oauth.clientId, //
						"redirect_uri", redirectUri)//
				.go(200);
	}

}
