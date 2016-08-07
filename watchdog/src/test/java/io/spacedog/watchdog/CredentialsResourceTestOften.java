/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class CredentialsResourceTestOften extends Assert {

	@Test
	public void userIsSigningUpAndMore() throws Exception {

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// fails since invalid bodies

		// empty user body
		SpaceRequest.post("/1/credentials/").backend(test)//
				.body(Json.object()).go(400);

		// no username
		SpaceRequest.post("/1/credentials/").backend(test)//
				.body("password", "hi titi", "email", "titi@dog.com").go(400);

		// no email
		SpaceRequest.post("/1/credentials/").backend(test)//
				.body("username", "titi", "password", "hi titi").go(400);

		// username too small
		SpaceRequest.post("/1/credentials/").backend(test)//
				.body("username", "ti", "password", "hi titi").go(400);

		// password too small
		SpaceRequest.post("/1/credentials/").backend(test)//
				.body("username", "titi", "password", "hi").go(400);

		// vince signs up
		User vince = SpaceClient.newCredentials(test, "vince", "hi vince", "vince@dog.com");

		// vince gets his credentials
		SpaceRequest.get("/1/credentials/vince").userAuth(vince).go(200)//
				.assertEquals("vince", "username")//
				.assertEquals("vince@dog.com", "email")//
				.assertEquals("USER", "level")//
				.assertSizeEquals(1, "roles")//
				.assertDateIsRecent("createdAt")//
				.assertDateIsRecent("updatedAt");

		// vince fails to get his credentials if wrong username
		SpaceRequest.get("/1/credentials/vince").basicAuth(test, "XXX", "hi vince").go(401);

		// vince fails to get his credentials if wrong password
		SpaceRequest.get("/1/credentials/vince").basicAuth(test, "vince", "XXX").go(401);

		// vince fails to get his credentials if wrong backend id
		SpaceRequest.get("/1/credentials/vince")//
				.basicAuth("XXX", vince.username, vince.password).go(401);

		// anonymous fails to get vince credentials
		SpaceRequest.get("/1/credentials/vince").backend(test).go(403);

		// another user fails to get vince credentials
		User fred = SpaceClient.newCredentials(test, "fred", "hi fred");
		SpaceRequest.get("/1/credentials/vince").userAuth(fred).go(403);

		// vince succeeds to login
		SpaceRequest.get("/1/login").userAuth(vince).go(200);

		// vince fails to login if wrong password
		SpaceRequest.get("/1/login").basicAuth(test, "vince", "XXX").go(401);
	}

	@Test
	public void testAccessTokenAndExpiration() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		Backend test2 = SpaceClient.resetTest2Backend();

		// fails to access backend if unknown token
		SpaceRequest.get("/1/data").bearerAuth(test, "XXX").go(401);

		// vince signs up and get a brand new access token
		User vince = SpaceClient.newCredentials(test, "vince", "hi vince");

		// vince request fails because wrong backend
		SpaceRequest.get("/1/data").bearerAuth(test2, vince.accessToken).go(401);

		// vince request succeeds since valid [backend / access token] pair
		SpaceRequest.get("/1/data").bearerAuth(vince).go(200);

		// vince logs out and cancels its access token
		SpaceRequest.get("/1/logout").bearerAuth(vince).go(200);

		// vince fails to logout a second time
		// since its access token is canceled
		SpaceRequest.get("/1/logout").bearerAuth(vince).go(401);

		// vince fails to access backend
		// since its access token is canceled
		SpaceRequest.get("/1/data").bearerAuth(vince).go(401);

		// vince fails to login with its canceled access token
		SpaceRequest.get("/1/login").bearerAuth(vince).go(401);

		// vince logs in again
		ObjectNode node = SpaceRequest.get("/1/login").userAuth(vince).go(200).objectNode();
		vince.accessToken = node.get("accessToken").asText();
		long expiresIn = node.get("expiresIn").asLong();
		vince.expiresAt = DateTime.now().plus(expiresIn);
		assertTrue(expiresIn < (1000 * 60 * 60 * 24));

		// vince can access backend again with its brand new access token
		SpaceRequest.get("/1/data").bearerAuth(vince).go(200);

		// if vince logs in again with access token
		// its access token is not reset
		String vinceOldAccessToken = vince.accessToken;
		SpaceRequest.get("/1/login").bearerAuth(vince).go(200)//
				.assertNotPresent("accessToken");

		// vince can access backend with its old token
		// since it has not changed
		SpaceRequest.get("/1/data").bearerAuth(test, vinceOldAccessToken).go(200);

		// if vince logs in again with its password
		// its access token is reset
		node = SpaceRequest.get("/1/login").userAuth(vince).go(200).objectNode();
		vince.accessToken = node.get("accessToken").asText();
		expiresIn = node.get("expiresIn").asLong();
		vince.expiresAt = DateTime.now().plus(expiresIn);

		// vince fails to access backend with its old token
		// since it has been reset
		SpaceRequest.get("/1/data").bearerAuth(test, vinceOldAccessToken).go(401);

		// vince logs out and cancels its access token
		SpaceRequest.get("/1/logout").bearerAuth(vince).go(200);

		// vince logs in with token expiration of 2 seconds
		node = SpaceRequest.get("/1/login").userAuth(vince)//
				.queryParam("expiresEarly", "true")//
				.go(200).objectNode();

		vince.accessToken = node.get("accessToken").asText();
		expiresIn = node.get("expiresIn").asLong();
		vince.expiresAt = DateTime.now().plus(expiresIn);

		assertTrue(expiresIn < 2000);

		// vince access token expires after 2 seconds
		Thread.sleep(2000);
		SpaceRequest.get("/1/data").bearerAuth(vince).go(401);
	}

	@Test
	public void deleteCredentials() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.newCredentials(test, "fred", "hi fred");
		User vince = SpaceClient.newCredentials(test, "vince", "hi vince");

		// vince and fred can login
		SpaceRequest.get("/1/login").userAuth(vince).go(200);
		SpaceRequest.get("/1/login").userAuth(fred).go(200);

		// anonymous fails to deletes vince credentials
		SpaceRequest.delete("/1/credentials/vince").backend(test).go(403);

		// fred fails to delete vince credentials
		SpaceRequest.delete("/1/credentials/vince").userAuth(fred).go(403);

		// fred deletes his own credentials
		SpaceRequest.delete("/1/credentials/fred").userAuth(fred).go(200);

		// fred fails to login from now on
		SpaceRequest.get("/1/login").userAuth(fred).go(401);

		// admin deletes vince credentials
		SpaceRequest.delete("/1/credentials/vince").adminAuth(test).go(200);

		// vince fails to login from now on
		SpaceRequest.get("/1/login").userAuth(vince).go(401);
	}

	@Test
	public void setAndResetPassword() throws Exception {

		// prepare

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		SpaceClient.newCredentials(test, "toto", "hi toto");

		// sign up without password should succeed

		String passwordResetCode = SpaceRequest.post("/1/credentials/").backend(test)//
				.body("username", "titi", "email", "titi@dog.com").go(201)//
				.assertNotNull("passwordResetCode")//
				.getFromJson("passwordResetCode")//
				.asText();

		// no password user login should fail
		// I can not pass a null password anyway to the basicAuth method

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "XXX").go(401);

		// no password user trying to create password with empty reset code
		// should fail

		SpaceRequest.post("/1/credentials/titi/password?passwordResetCode=").backend(test)//
				.formField("password", "hi titi").go(400);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(401);

		// no password user setting password with wrong reset code should fail

		SpaceRequest.post("/1/credentials/titi/password?passwordResetCode=XXX").backend(test)
				.formField("password", "hi titi").go(400);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(401);

		// titi inits its own password with right reset code should succeed

		SpaceRequest.post("/1/credentials/titi/password").backend(test)//
				.formField("passwordResetCode", passwordResetCode)//
				.formField("password", "hi titi")//
				.go(200);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(200);

		// toto user changes titi password should fail

		SpaceRequest.put("/1/credentials/titi/password").basicAuth(test, "toto", "hi toto")//
				.formField("password", "XXX").go(403);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "XXX").go(401);

		// titi changes its password should fail since password size < 6

		SpaceRequest.put("/1/credentials/titi/password").basicAuth(test, "titi", "hi titi")//
				.formField("password", "XXX").go(400);

		// titi changes its password should succeed

		SpaceRequest.put("/1/credentials/titi/password").basicAuth(test, "titi", "hi titi")
				.formField("password", "hi titi 2").go(200);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi 2").go(200);

		// login with old password should fail

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(401);

		// admin user changes titi user password should succeed

		SpaceRequest.put("/1/credentials/titi/password").basicAuth(test, "test", "hi test")//
				.formField("password", "hi titi 3").go(200);
		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi 3").go(200);

		// login with old password should fail

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi 2").go(401);

		// admin deletes titi password should succeed

		String newPasswordResetCode = SpaceRequest.delete("/1/credentials/titi/password")
				.basicAuth(test, "test", "hi test").go(200).getFromJson("passwordResetCode").asText();

		// titi login should fail

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi 3").go(401);

		// titi inits its password with old reset code should fail

		SpaceRequest.post("/1/credentials/titi/password?passwordResetCode=" + passwordResetCode)//
				.backend(test).formField("password", "hi titi").go(400);

		// titi inits its password with new reset code should fail

		SpaceRequest.post("/1/credentials/titi/password?passwordResetCode=" + newPasswordResetCode)//
				.backend(test).formField("password", "hi titi").go(200);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(200);
	}

	@Test
	public void credentialsAreDeletedWhenBackendIsDeleted() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.newCredentials(test, "fred", "hi fred");

		// fred can login
		SpaceRequest.get("/1/login").userAuth(fred).go(200);

		// admin deletes backend
		SpaceClient.deleteBackend(test);

		// fred fails to login since backend is no more
		SpaceRequest.get("/1/login").userAuth(fred).go(401);

		// admin creates backend with the same name
		SpaceClient.createBackend(test);

		// fred fails to login since backend is brand new
		SpaceRequest.get("/1/login").userAuth(fred).go(401);
	}

	@Test
	public void getPutAndDeleteUserCredentialsRoles() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.newCredentials(test, "fred", "hi fred", "fred@dog.com");

		// fred gets his credentials roles
		SpaceRequest.get("/1/credentials/fred/roles").userAuth(fred).go(200)//
				.assertSizeEquals(1)//
				.assertEquals("user", "0");

		// fred fails to set a role since he is no admin
		SpaceRequest.put("/1/credentials/fred/roles/silver").userAuth(fred).go(403);

		// admin sets fred's roles
		SpaceRequest.put("/1/credentials/fred/roles/silver").adminAuth(test).go(200);
		SpaceRequest.put("/1/credentials/fred/roles/gold").adminAuth(test).go(200);
		SpaceRequest.get("/1/credentials/fred/roles").adminAuth(test).go(200)//
				.assertSizeEquals(3)//
				.assertContains(TextNode.valueOf("user"))//
				.assertContains(TextNode.valueOf("silver"))//
				.assertContains(TextNode.valueOf("gold"));

		// fred fails to delete one of his roles since he is no admin
		SpaceRequest.delete("/1/credentials/fred/roles/silver").userAuth(fred).go(403);

		// admin deletes one of fred's roles
		SpaceRequest.delete("/1/credentials/fred/roles/gold").adminAuth(test).go(200);
		SpaceRequest.get("/1/credentials/fred/roles").adminAuth(test).go(200)//
				.assertSizeEquals(2)//
				.assertContains(TextNode.valueOf("user"))//
				.assertContains(TextNode.valueOf("silver"));

		// admin deletes all fred's roles
		SpaceRequest.delete("/1/credentials/fred/roles").adminAuth(test).go(200);
		SpaceRequest.get("/1/credentials/fred/roles").userAuth(fred).go(200)//
				.assertSizeEquals(1)//
				.assertEquals("user", "0");
	}

	@Test
	public void linkedinSignUpAndMore() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// no linkedin settings means no linkedin credentials
		SpaceRequest.post("/1/credentials/linkedin").backend(test).go(404);

		// admin sets linkedin settings
		String redirectUri = SpaceRequest.configuration().target()//
				.url(test.backendId, "/1/credentials/linkedin");
		SpaceRequest.put("/1/settings/linkedin")//
				.adminAuth(test)//
				.body("clientId", "78uk3jfazu0wj2", "clientSecret", "42AfVLDNEXtgO9CG", //
						"redirectUri", redirectUri)//
				.go(201);

		// fails to create linkedin credentials if no authorization code
		SpaceRequest.post("/1/credentials/linkedin").backend(test).go(400);

		// fails to create linkedin credentials if invalid authorization code
		SpaceRequest.post("/1/credentials/linkedin").backend(test)//
				.formField("code", "XXX")//
				.go(401);

		// fred signs up with linkedin

		// SpaceResponse response = SpaceRequest//
		// .get("https://www.linkedin.com/oauth/v2/authorization")//
		// .queryParam("response_type", "code")//
		// .queryParam("redirect_uri", redirectUri)//
		// .queryParam("client_id", "78uk3jfazu0wj2")//
		// .queryParam("state", "thisisit")//
		// .go(303);
		//
		// List<String> cookies = response.header(SpaceHeaders.SET_COOKIE);
		// String location = response.headerFirst(SpaceHeaders.LOCATION);
		//
		// SpaceRequest.get(location)//
		// .cookies(cookies)//
		// .go();
		//
		// String linkedinLogin = "XXX", linkedinPassword = "XXX";
		// SpaceRequest.post("https://www.linkedin.com/uas/login-submit")//
		// .formField("session_key", linkedinLogin)//
		// .formField("session_password", linkedinPassword)//
		// .go(200);
		//
		// String authorizationCode = "XXX";
		//
		// response = SpaceRequest.post("/1/credentials/linkedin")//
		// .backend(test)//
		// .formField("code", authorizationCode)//
		// .formField("redirect_uri", redirectUri)//
		// .go(201);
		//
		// String accessToken = response.getFronJson("accessToken");
		// String id = response.headerFirst(SpaceHeaders.SPACEDOG_OBJECT_ID);
		//
		// SpaceRequest.get("/1/credentials/" + id).bearerAuth(test,
		// accessToken).go(200);
	}

}
