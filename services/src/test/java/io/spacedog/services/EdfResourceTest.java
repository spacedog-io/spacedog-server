/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.CredentialsSettings;
import io.spacedog.utils.CredentialsSettings.OAuthSettings;
import io.spacedog.utils.SpaceHeaders;

public class EdfResourceTest extends SpaceTest {

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
		oauth.clientId = SpaceRequest.env().get("edf.oauth.client.id");
		oauth.clientSecret = SpaceRequest.env().get("edf.oauth.client.secret");
		oauth.useExpiresIn = true;

		CredentialsSettings settings = new CredentialsSettings();
		settings.oauth = oauth;
		test.settings().save(settings);

		// USER01 gets an authorization code from EDF
		SpaceDog user01 = SpaceDog.backend(test).username("USER01");

		String code = SpaceRequest
				.post(//
						"https://noefy5jt.noe.edf.fr:5641/ws/iOAuthApprove/do")//
				.basicAuth(user01.username(), user01.username())//
				.header(SpaceHeaders.ACCEPT, "application/json")//
				.header(SpaceHeaders.ACCEPT_ENCODING, "gzip")//
				.header(SpaceHeaders.USER_AGENT, "spacedog-server")//
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
				.go(200).objectNode();

		user01.id(node.get("credentials").get("id").asText());
		user01.accessToken(node.get("accessToken").asText());

		// USER01 checks his access token is working fine
		// by getting his credentials
		Credentials credentials = user01.credentials().getByUsername(user01.username()).get();
		assertEquals(user01.id(), credentials.id());
		assertEquals(user01.username(), credentials.name());
	}

	@Test
	public void testDirectEdfOAuth() {

		// prepare
		OAuthSettings oauth = new OAuthSettings();
		oauth.clientId = SpaceRequest.env().get("edf.oauth.client.id");
		oauth.clientSecret = SpaceRequest.env().get("edf.oauth.client.secret");
		String redirectUri = "https://noefy5jt.noe.edf.fr:5641" //
				+ "/invoke/PAAS_OAUTH.services:defaultRedirectURI";

		// USER01 gets authorization code from EDF
		String code = SpaceRequest
				.post(//
						"https://noefy5jt.noe.edf.fr:5641/ws/iOAuthApprove/do")//
				.basicAuth("USER01", "USER01")//
				.header(SpaceHeaders.ACCEPT, "application/json")//
				.header(SpaceHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")//
				.header(SpaceHeaders.CONNECTION, "Keep-Alive")//
				.header(SpaceHeaders.ACCEPT_ENCODING, "gzip")//
				.header(SpaceHeaders.USER_AGENT, "spacedog-server")//
				.formField("approved", "true")//
				.formField("scope", "1001Espaces")//
				.formField("client_id", oauth.clientId)//
				.formField("response_type", "code")//
				.formField("redirect_uri", redirectUri)//
				.go(200).getString("code");

		// USER01 gets an access token from EDF
		SpaceRequest
				.post(//
						"https://noefy5jt.noe.edf.fr:5641/ws/iOAuthGetToken/do")//
				.basicAuth(oauth.clientId, oauth.clientSecret)//
				.header(SpaceHeaders.ACCEPT, "application/json")//
				.header(SpaceHeaders.CONNECTION, "Keep-Alive")//
				.header(SpaceHeaders.ACCEPT_ENCODING, "gzip")//
				.header(SpaceHeaders.USER_AGENT, "spacedog-server")//
				.body("grant_type", "authorization_code", "code", code, //
						"client_id", oauth.clientId, //
						"redirect_uri", redirectUri)//
				.go(200);
	}

}
