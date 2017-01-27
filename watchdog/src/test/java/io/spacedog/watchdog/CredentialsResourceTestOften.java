/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.CredentialsSettings;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class CredentialsResourceTestOften extends SpaceTest {

	@Test
	public void userIsSigningUpAndMore() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// fails since empty user body
		SpaceRequest.post("/1/credentials/").backend(test)//
				.body(Json.object()).go(400);

		// fails since no username
		SpaceRequest.post("/1/credentials/").backend(test)//
				.body("password", "hi titi", "email", "titi@dog.com").go(400);

		// fails since no email
		SpaceRequest.post("/1/credentials/").backend(test)//
				.body("username", "titi", "password", "hi titi").go(400);

		// fails since username too small
		SpaceRequest.post("/1/credentials/").backend(test)//
				.body("username", "ti", "password", "hi titi").go(400);

		// fails since password too small
		SpaceRequest.post("/1/credentials/").backend(test)//
				.body("username", "titi", "password", "hi").go(400);

		// vince signs up
		String vinceId = SpaceRequest.post("/1/credentials").backend(test)
				.body("username", "vince", "password", "hi vince", "email", "vince@dog.com")//
				.go(201).getString("id");

		// vince fails to sign up again since his credentials already exixts
		SpaceRequest.post("/1/credentials").backend(test)
				.body("username", "vince", "password", "hello boby", "email", "vince@dog.com")//
				.go(400)//
				.assertEquals("already-exists", "error.code");

		// vince logs in
		ObjectNode node = SpaceRequest.get("/1/login")//
				.basicAuth(test.backendId(), "vince", "hi vince").go(200)//
				.assertPresent("accessToken")//
				.assertPresent("expiresIn")//
				.assertEquals(vinceId, "credentials.id")//
				.assertEquals("test", "credentials.backendId")//
				.assertEquals("vince", "credentials.username")//
				.assertEquals("vince@dog.com", "credentials.email")//
				.assertEquals(true, "credentials.enabled")//
				.assertEquals("USER", "credentials.level")//
				.assertSizeEquals(1, "credentials.roles")//
				.assertEquals("user", "credentials.roles.0")//
				.assertPresent("credentials.createdAt")//
				.assertPresent("credentials.updatedAt")//
				.objectNode();

		SpaceDog vince = SpaceDog.backend("test").username("vince")//
				.id(vinceId).password("hi vince").email("vince@dog.com")//
				.accessToken(node.get("accessToken").asText()) //
				.expiresAt(DateTime.now().plus(node.get("expiresIn").asLong()));

		// vince gets his credentials
		SpaceRequest.get("/1/credentials/" + vince.id()).userAuth(vince).go(200)//
				.assertEquals("vince", "username")//
				.assertEquals("vince@dog.com", "email")//
				.assertEquals("USER", "level")//
				.assertSizeEquals(1, "roles")//
				.assertDateIsRecent("createdAt")//
				.assertDateIsRecent("updatedAt");

		// vince fails to get his credentials if wrong username
		SpaceRequest.get("/1/credentials/" + vince.id()).basicAuth("test", "XXX", "hi vince").go(401);

		// vince fails to get his credentials if wrong password
		SpaceRequest.get("/1/credentials/" + vince.id()).basicAuth("test", "vince", "XXX").go(401);

		// vince fails to get his credentials if wrong backend id
		SpaceRequest.get("/1/credentials/" + vince.id())//
				.basicAuth("XXX", vince.username(), vince.password().get()).go(401);

		// anonymous fails to get vince credentials
		SpaceRequest.get("/1/credentials/" + vince.id()).backend(test).go(403);

		// another user fails to get vince credentials
		SpaceDog fred = signUp("test", "fred", "hi fred");
		SpaceRequest.get("/1/credentials/" + vince.id()).userAuth(fred).go(403);

		// vince succeeds to login
		SpaceRequest.get("/1/login").userAuth(vince).go(200);

		// vince fails to login if wrong password
		SpaceRequest.get("/1/login").basicAuth("test", "vince", "XXX").go(401);
	}

	@Test
	public void testAccessTokenAndExpiration() throws InterruptedException {

		// prepare
		prepareTest();
		resetTestBackend();
		resetTest2Backend();

		// fails to access backend if unknown token
		SpaceRequest.get("/1/data").bearerAuth("test", "XXX").go(401);

		// vince signs up and get a brand new access token
		SpaceDog vince = SpaceDog.backend("test").username("vince").password("hi vince").signUp();

		// vince request fails because wrong backend
		SpaceRequest.get("/1/data").bearerAuth("test2", vince.accessToken().get()).go(401);

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
		ObjectNode node = SpaceRequest.get("/1/login").basicAuth(vince).go(200).objectNode();
		vince.accessToken(node.get("accessToken").asText());
		long expiresIn = node.get("expiresIn").asLong();
		vince.expiresAt(DateTime.now().plus(expiresIn));
		assertTrue(expiresIn < (1000 * 60 * 60 * 24));

		// vince can access backend again with its brand new access token
		SpaceRequest.get("/1/data").bearerAuth(vince).go(200);

		// if vince logs in again with access token
		// its access token is not reset
		String vinceOldAccessToken = vince.accessToken().get();
		SpaceRequest.get("/1/login").bearerAuth(vince).go(200)//
				.assertEquals(vinceOldAccessToken, "accessToken");

		// vince can access backend with its old token
		// since it has not changed
		SpaceRequest.get("/1/data").bearerAuth("test", vinceOldAccessToken).go(200);

		// if vince logs in again with its password
		// he gets a new access token
		vince.login("hi vince");
		assertNotEquals(vince.accessToken().get(), vinceOldAccessToken);

		// vince can access data with its new token
		// but can also access data with its old token
		SpaceRequest.get("/1/data").bearerAuth("test", vince.accessToken().get()).go(200);
		SpaceRequest.get("/1/data").bearerAuth("test", vinceOldAccessToken).go(200);

		// vince logs out of his newest session
		SpaceRequest.get("/1/logout").bearerAuth(vince).go(200);

		// vince can no longer access data with its new token
		// but can still access data with its old token
		SpaceRequest.get("/1/data").bearerAuth("test", vince.accessToken().get()).go(401);
		SpaceRequest.get("/1/data").bearerAuth("test", vinceOldAccessToken).go(200);

		// vince logs out of his oldest session
		SpaceRequest.get("/1/logout").bearerAuth("test", vinceOldAccessToken).go(200);

		// vince can no longer access data with both tokens
		SpaceRequest.get("/1/data").bearerAuth("test", vince.accessToken().get()).go(401);
		SpaceRequest.get("/1/data").bearerAuth("test", vinceOldAccessToken).go(401);

		// vince logs in with token expiration of 2 seconds
		node = SpaceRequest.get("/1/login").basicAuth(vince)//
				.queryParam("lifetime", "2") // seconds
				.go(200).objectNode();

		vince.accessToken(node.get("accessToken").asText());
		expiresIn = node.get("expiresIn").asLong();
		vince.expiresAt(DateTime.now().plus(expiresIn));

		assertTrue(expiresIn <= 2);
	}

	@Test
	public void accessTokenMaximumLifetime() throws InterruptedException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = SpaceDog.backend("test").username("fred").password("hi fred").signUp();

		// admin saves settings with token max lifetime set to 3s
		CredentialsSettings settings = new CredentialsSettings();
		settings.sessionMaximumLifetime = 3; // seconds
		test.settings().save(settings);

		// fred fails to login with a token lifetime of 4s
		// since max token lifetime is 3s
		SpaceRequest.get("/1/login").basicAuth(fred).queryParam(PARAM_LIFETIME, "4").go(403);

		// fred logs in with a token lifetime of 2s
		// since max token lifetime is 3s
		fred.login(2);
		SpaceRequest.get("/1/data").bearerAuth(fred).go(200);

		// after lifetime, token expires
		Thread.sleep(2000); // in milliseconds
		SpaceRequest.get("/1/data").bearerAuth(fred).go(401);
	}

	@Test
	public void deleteCredentials() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = SpaceDog.backend("test").username("fred").signUp("hi fred");
		SpaceDog vince = SpaceDog.backend("test").username("vince").signUp("hi vince");

		// vince and fred can login
		vince.login("hi vince");
		fred.login("hi fred");

		// anonymous fails to deletes vince credentials
		SpaceRequest.delete("/1/credentials/" + vince.id()).backend(test).go(403);

		// fred fails to delete vince credentials
		SpaceRequest.delete("/1/credentials/" + vince.id()).userAuth(fred).go(403);

		// fred deletes his own credentials
		SpaceRequest.delete("/1/credentials/" + fred.id()).userAuth(fred).go(200);

		// fred fails to login from now on
		SpaceRequest.get("/1/login").userAuth(fred).go(401);

		// admin deletes vince credentials
		SpaceRequest.delete("/1/credentials/" + vince.id()).adminAuth(test).go(200);

		// vince fails to login from now on
		SpaceRequest.get("/1/login").userAuth(vince).go(401);
	}

	@Test
	public void searchAndDeleteCredentials() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = signUp(test, "fred", "hi fred");
		SpaceDog vince = signUp(test, "vince", "hi vince");

		// vince searches for all credentials
		SpaceRequest.get("/1/credentials").userAuth(vince).go(200)//
				.assertEquals(3, "total")//
				.assertSizeEquals(3, "results")//
				.assertContainsValue("test", "username")//
				.assertContainsValue("vince", "username")//
				.assertContainsValue("fred", "username");

		// super admin searches for user credentials
		SpaceRequest.get("/1/credentials?level=USER").adminAuth(test).go(200)//
				.assertEquals(2, "total")//
				.assertSizeEquals(2, "results")//
				.assertContainsValue("vince", "username")//
				.assertContainsValue("fred", "username");

		// fred searches for credentials with a specified email
		SpaceRequest.get("/1/credentials")//
				.queryParam("email", "platform@spacedog.io")//
				.userAuth(fred).go(200)//
				.assertEquals(3, "total")//
				.assertSizeEquals(3, "results")//
				.assertContainsValue("test", "username")//
				.assertContainsValue("vince", "username")//
				.assertContainsValue("fred", "username");

		// vince searches for credentials with specified username
		SpaceRequest.get("/1/credentials?username=fred").userAuth(vince).go(200)//
				.assertSizeEquals(1, "results")//
				.assertContainsValue("fred", "username")//
				.assertContainsValue("platform@spacedog.io", "email")//
				.assertContainsValue("USER", "level");

		// super admin deletes credentials with specified username
		SpaceRequest.delete("/1/credentials?username=fred").adminAuth(test).go(200);

		SpaceRequest.get("/1/credentials").adminAuth(test).go(200)//
				.assertSizeEquals(2, "results")//
				.assertContainsValue("test", "username")//
				.assertContainsValue("vince", "username");

		// super admin deletes credentials with specified level
		SpaceRequest.delete("/1/credentials?level=USER").adminAuth(test).go(200);

		SpaceRequest.get("/1/credentials").adminAuth(test).go(200)//
				.assertSizeEquals(1, "results")//
				.assertContainsValue("test", "username");

		// super admin deletes all credentials but himself
		SpaceRequest.delete("/1/credentials").adminAuth(test).go(200);

		SpaceRequest.get("/1/credentials").adminAuth(test).go(200)//
				.assertSizeEquals(1, "results")//
				.assertContainsValue("test", "username");
	}

	@Test
	public void setAndResetPassword() {

		// prepare

		prepareTest();
		SpaceDog test = resetTestBackend();
		signUp(test, "toto", "hi toto");

		// sign up without password should succeed

		ObjectNode node = SpaceRequest.post("/1/credentials/").backend(test)//
				.body("username", "titi", "email", "titi@dog.com").go(201)//
				.assertNotNull("passwordResetCode")//
				.objectNode();

		String titiId = node.get("id").asText();
		String passwordResetCode = node.get("passwordResetCode").asText();

		// no password user login should fail
		// I can not pass a null password anyway to the basicAuth method

		SpaceRequest.get("/1/login").basicAuth("test", "titi", "XXX").go(401);

		// no password user trying to create password with empty reset code
		// should fail

		SpaceRequest.post("/1/credentials/" + titiId + "/password").backend(test)//
				.queryParam("passwordResetCode", "")//
				.formField("password", "hi titi").go(400);

		SpaceRequest.get("/1/login").basicAuth("test", "titi", "hi titi").go(401);

		// no password user setting password with wrong reset code should fail

		SpaceRequest.post("/1/credentials/" + titiId + "/password").backend(test)//
				.queryParam("passwordResetCode", "XXX")//
				.formField("password", "hi titi").go(400);

		SpaceRequest.get("/1/login").basicAuth("test", "titi", "hi titi").go(401);

		// titi inits its own password with right reset code should succeed

		SpaceRequest.post("/1/credentials/" + titiId + "/password").backend(test)//
				.formField("passwordResetCode", passwordResetCode)//
				.formField("password", "hi titi")//
				.go(200);

		SpaceRequest.get("/1/login").basicAuth("test", "titi", "hi titi").go(200);

		// toto user changes titi password should fail

		SpaceRequest.put("/1/credentials/" + titiId + "/password")//
				.basicAuth("test", "toto", "hi toto")//
				.formField("password", "XXX")//
				.go(403);

		SpaceRequest.get("/1/login").basicAuth("test", "titi", "XXX").go(401);

		// titi changes its password should fail since password size < 6

		SpaceRequest.put("/1/credentials/" + titiId + "/password")//
				.basicAuth("test", "titi", "hi titi")//
				.formField("password", "XXX")//
				.go(400);

		// titi changes its password should succeed

		SpaceRequest.put("/1/credentials/" + titiId + "/password")//
				.basicAuth("test", "titi", "hi titi")//
				.formField("password", "hi titi 2")//
				.go(200);

		SpaceRequest.get("/1/login").basicAuth("test", "titi", "hi titi 2").go(200);

		// login with old password should fail

		SpaceRequest.get("/1/login").basicAuth("test", "titi", "hi titi").go(401);

		// admin user changes titi user password should succeed

		SpaceRequest.put("/1/credentials/" + titiId + "/password")//
				.basicAuth("test", "test", "hi test")//
				.formField("password", "hi titi 3")//
				.go(200);

		SpaceRequest.get("/1/login").basicAuth("test", "titi", "hi titi 3").go(200);

		// login with old password should fail

		SpaceRequest.get("/1/login").basicAuth("test", "titi", "hi titi 2").go(401);

		// admin deletes titi password should succeed

		String newPasswordResetCode = SpaceRequest//
				.delete("/1/credentials/" + titiId + "/password")//
				.basicAuth("test", "test", "hi test")//
				.go(200)//
				.getString("passwordResetCode");

		// titi login should fail

		SpaceRequest.get("/1/login").basicAuth("test", "titi", "hi titi 3").go(401);

		// titi inits its password with old reset code should fail

		SpaceRequest.post("/1/credentials/" + titiId + "/password").backend(test)//
				.queryParam("passwordResetCode", passwordResetCode)//
				.formField("password", "hi titi")//
				.go(400);

		// titi inits its password with new reset code should fail

		SpaceRequest.post("/1/credentials/" + titiId + "/password").backend(test)//
				.queryParam("passwordResetCode", newPasswordResetCode)//
				.formField("password", "hi titi")//
				.go(200);

		SpaceRequest.get("/1/login").basicAuth("test", "titi", "hi titi").go(200);
	}

	@Test
	public void credentialsAreDeletedWhenBackendIsDeleted() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = signUp(test, "fred", "hi fred");

		// fred can login
		SpaceRequest.get("/1/login").userAuth(fred).go(200);

		// admin deletes backend
		test.backend().delete();

		// fred fails to login since backend is no more
		SpaceRequest.get("/1/login").userAuth(fred).go(401);

		// admin creates backend with the same name
		test.signUpBackend();

		// fred fails to login since backend is brand new
		SpaceRequest.get("/1/login").userAuth(fred).go(401);
	}

	@Test
	public void getPutAndDeleteUserCredentialsRoles() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = signUp(test, "fred", "hi fred");

		// fred gets his credentials roles
		SpaceRequest.get("/1/credentials/" + fred.id() + "/roles")//
				.userAuth(fred).go(200)//
				.assertSizeEquals(1)//
				.assertEquals("user", "0");

		// fred fails to set a role since he is no admin
		SpaceRequest.put("/1/credentials/" + fred.id() + "/roles/silver").userAuth(fred).go(403);

		// admin sets fred's roles
		SpaceRequest.put("/1/credentials/" + fred.id() + "/roles/silver").adminAuth(test).go(200);
		SpaceRequest.put("/1/credentials/" + fred.id() + "/roles/gold").adminAuth(test).go(200);
		SpaceRequest.get("/1/credentials/" + fred.id() + "/roles").adminAuth(test).go(200)//
				.assertSizeEquals(3)//
				.assertContains(TextNode.valueOf("user"))//
				.assertContains(TextNode.valueOf("silver"))//
				.assertContains(TextNode.valueOf("gold"));

		// fred fails to delete one of his roles since he is no admin
		SpaceRequest.delete("/1/credentials/" + fred.id() + "/roles/silver").userAuth(fred).go(403);

		// admin deletes one of fred's roles
		SpaceRequest.delete("/1/credentials/" + fred.id() + "/roles/gold").adminAuth(test).go(200);
		SpaceRequest.get("/1/credentials/" + fred.id() + "/roles").adminAuth(test).go(200)//
				.assertSizeEquals(2)//
				.assertContains(TextNode.valueOf("user"))//
				.assertContains(TextNode.valueOf("silver"));

		// admin deletes all fred's roles
		SpaceRequest.delete("/1/credentials/" + fred.id() + "/roles").adminAuth(test).go(200);
		SpaceRequest.get("/1/credentials/" + fred.id() + "/roles").userAuth(fred).go(200)//
				.assertSizeEquals(1)//
				.assertEquals("user", "0");
	}

	@Test
	public void disableGuestSignUp() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = signUp(test, "fred", "hi fred");

		// admin disables guest sign up
		CredentialsSettings settings = new CredentialsSettings();
		settings.disableGuestSignUp = true;
		test.settings().save(settings);

		// guest can not create credentials
		SpaceRequest.post("/1/credentials").backend(test)//
				.body("username", "vince", "password", "hi vince", "email", "vince@dog.com")//
				.go(403);

		// fred fails to create credentials if no email
		SpaceRequest.post("/1/credentials").userAuth(fred)//
				.body("username", "vince")//
				.go(400);

		// fred can create credentials for someone
		ObjectNode node = SpaceRequest.post("/1/credentials").userAuth(fred)//
				.body("username", "vince", "email", "vince@dog.com")//
				.go(201).objectNode();

		String vinceId = node.get("id").asText();
		String resetCode = node.get("passwordResetCode").asText();

		// someone can set password if he receives a password reset code
		SpaceRequest.post("/1/credentials/" + vinceId + "/password")//
				.backend(test)//
				.queryParam("passwordResetCode", resetCode)//
				.formField("password", "hi vince")//
				.go(200);
	}

	@Test
	public void testUsernameAndPasswordRegexSettings() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// get default username and password settings
		CredentialsSettings settings = test.settings().get(CredentialsSettings.class);
		assertNotNull(settings.usernameRegex);
		assertNotNull(settings.passwordRegex);

		// invalid username considering default username regex
		SpaceRequest.post("/1/credentials").backend(test)//
				.body("username", "vi", "password", "hi", "email", "vince@dog.com")//
				.go(400);

		// invalid password considering default password regex
		SpaceRequest.post("/1/credentials").backend(test)//
				.body("username", "vince", "password", "hi", "email", "vince@dog.com")//
				.go(400);

		// valid username and password considering default credentials settings
		SpaceRequest.post("/1/credentials").backend(test)//
				.body("username", "vince", "password", "hi vince", "email", "vince@dog.com")//
				.go(201);

		// set username and password specific regex
		settings.usernameRegex = "[a-zA-Z]{3,}";
		settings.passwordRegex = "[a-zA-Z]{3,}";
		test.settings().save(settings);

		// invalid username considering new username regex
		SpaceRequest.post("/1/credentials").backend(test)//
				.body("username", "nath.lopez", "password", "hi nath", "email", "nath@dog.com")//
				.go(400);

		// invalid password considering new password regex
		SpaceRequest.post("/1/credentials").backend(test)//
				.body("username", "nathlopez", "password", "hi nath", "email", "nath@dog.com")//
				.go(400);

		// valid username and password considering credentials settings
		SpaceRequest.post("/1/credentials").backend(test)//
				.body("username", "nathlopez", "password", "hinath", "email", "nath@dog.com")//
				.go(201);
	}

	@Test
	public void adminCreatesOthersAdminCredentials() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = signUp(test, "fred", "hi fred");

		// fred fails to create admin credentials
		SpaceRequest.post("/1/credentials").userAuth(fred)
				.body("username", "vince", "password", "hi vince", "email", "vince@dog.com", "level", "ADMIN")//
				.go(403);

		// test (backend default superadmin) creates credentials for new
		// superadmin
		SpaceRequest.post("/1/credentials").adminAuth(test)
				.body("username", "superadmin", "password", "hi superadmin", //
						"email", "superadmin@dog.com", "level", "SUPER_ADMIN")//
				.go(201);

		// superadmin creates credentials for new admin
		SpaceRequest.post("/1/credentials")//
				.basicAuth("test", "superadmin", "hi superadmin")//
				.body("username", "admin1", "password", "hi admin1", //
						"email", "admin1@dog.com", "level", "ADMIN")//
				.go(201);
	}

	@Test
	public void passwordIsChangedOnlyIfPasswordHasBeenChecked() {

		// TODO move these tests to the setAndResetPasswords test

		// prepare
		prepareTest();
		resetTestBackend();
		SpaceDog fred = SpaceDog.backend("test").username("fred").password("hi fred").signUp();

		// fred fails to update his password
		// since his password is not challenged
		// because authentication is done with access token
		SpaceRequest.put("/1/credentials/" + fred.id() + "/password")//
				.userAuth(fred)//
				.formField("password", "hello fred")//
				.go(403)//
				.assertEquals("unchallenged-password", "error.code");

		// fred updates his password since his password is challenged
		// because authentication is done with username and password
		SpaceRequest.put("/1/credentials/" + fred.id() + "/password")//
				.basicAuth(fred)//
				.formField("password", "hello fred")//
				.go(200);
	}

	@Test
	public void updateUsernameEmailAndPassword() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = SpaceDog.backend("test").username("fred").password("hi fred").signUp();

		// fred fails to updates his username
		// since password must be challenged
		SpaceRequest.put("/1/credentials/" + fred.id()).bearerAuth(fred)//
				.body(Json.object("username", "fred2")).go(403)//
				.assertEquals("unchallenged-password", "error.code");

		// fred updates his username with a challenged password
		SpaceRequest.put("/1/credentials/" + fred.id()).basicAuth(fred)//
				.body(Json.object("username", "fred2")).go(200);

		// fred can no longer login with old username
		SpaceRequest.get("/1/login")//
				.basicAuth("test", "fred", "hi fred").go(401);

		// fred can login with his new username
		fred.username("fred2").login();

		// superadmin updates fred's email
		SpaceRequest.put("/1/credentials/" + fred.id()).basicAuth(test)//
				.body(Json.object("email", "fred2@dog.com")).go(200);

		SpaceRequest.get("/1/credentials/" + fred.id()).userAuth(fred)//
				.go(200).assertEquals("fred2@dog.com", "email");

		// fred fails to updates his password
		// since principal password must be challenged
		SpaceRequest.put("/1/credentials/" + fred.id()).bearerAuth(fred)//
				.body(Json.object("password", "hi fred2")).go(403)//
				.assertEquals("unchallenged-password", "error.code");

		// fred updates his password
		SpaceRequest.put("/1/credentials/" + fred.id()).basicAuth(fred)//
				.body(Json.object("password", "hi fred2")).go(200);

		// fred's old access token is not valid anymore
		SpaceRequest.get("/1/credentials/" + fred.id()).userAuth(fred).go(401);

		// fred's old password is not valid anymore
		fred.get("/1/login").go(401);

		// fred can login with his new password
		fred.password("hi fred2").login();
	}

	@Test
	public void disableCredentials() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = SpaceDog.backend("test").username("fred").password("hi fred").signUp();

		// fred logs in
		SpaceRequest.get("/1/login").userAuth(fred).go(200);

		// anonymous fails to disable fred's credentials
		SpaceRequest.put("/1/credentials/" + fred.id() + "/enabled")//
				.body(Json.toNode(false)).backend(test).go(403);

		// fred fails to disable his credentials
		SpaceRequest.put("/1/credentials/" + fred.id() + "/enabled")//
				.body(Json.toNode(false)).userAuth(fred).go(403);

		// admin fails to disable fred's credentials because body not a boolean
		SpaceRequest.put("/1/credentials/" + fred.id() + "/enabled")//
				.body(Json.toNode("false")).adminAuth(test).go(400);

		// only admin can disable fred's credentials
		SpaceRequest.put("/1/credentials/" + fred.id() + "/enabled")//
				.body(Json.toNode(false)).adminAuth(test).go(200);

		// fred fails to login from now on
		SpaceRequest.get("/1/login").userAuth(fred).go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// fred fails to access any resources from now on
		// with basic authentication scheme
		SpaceRequest.get("/1/data").basicAuth(fred).go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// fred fails to access any other resources from now on
		// with bearer authentication scheme
		SpaceRequest.get("/1/data").bearerAuth(fred).go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// fred fails to update his credentials from now on
		SpaceRequest.put("/1/credentials/" + fred.id()).userAuth(fred)//
				.body("username", "fredy").go(401);

		// anonymous fails to enable fred's credentials
		SpaceRequest.put("/1/credentials/" + fred.id() + "/enabled")//
				.body(Json.toNode(true)).backend(test).go(403);

		// fred fails to enable his credentials
		SpaceRequest.put("/1/credentials/" + fred.id() + "/enabled")//
				.body(Json.toNode(true)).userAuth(fred).go(401);

		// only admin can enable fred's credentials
		SpaceRequest.put("/1/credentials/" + fred.id() + "/enabled")//
				.body(Json.toNode(true)).adminAuth(test).go(200);

		// fred logs in again normally
		SpaceRequest.get("/1/login").userAuth(fred).go(200);
	}

	@Test
	public void userCanNotAccessAnotherBackend() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog test2 = resetTest2Backend();

		String testAdminUserToken = test.login().accessToken().get();
		String test2AdminUserToken = test2.login().accessToken().get();

		// user of a backend can not access another backend
		SpaceRequest.get("/1/data").backend(test2).bearerAuth(testAdminUserToken).go(401);
		SpaceRequest.get("/1/data").backend(test).bearerAuth(test2AdminUserToken).go(401);
	}

	@Test
	public void expiredSessionsArePurged() throws InterruptedException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// superadmin logs in with session lifetime of 1 second
		String firstToken = test.login(1).accessToken().get();

		// wait for session to expire
		Thread.sleep(1000);

		// check session has expired
		SpaceRequest.get("/1/data").backend(test).bearerAuth(firstToken).go(401)//
				.assertEquals("expired-access-token", "error.code");

		// check session has expired a second time
		// this means expired session has not yet been purged
		SpaceRequest.get("/1/data").backend(test).bearerAuth(firstToken).go(401)//
				.assertEquals("expired-access-token", "error.code");

		// superadmin logs in again and create a second session
		// this means superadmin credentials is updated
		// this means expired session token are purged
		test.login();

		// check expired session token is "invalid"
		// this means expired session has been purged
		// and its token is no longer present in spacedog
		SpaceRequest.get("/1/data").backend(test).bearerAuth(firstToken).go(401)//
				.assertEquals("invalid-access-token", "error.code");

	}

}
