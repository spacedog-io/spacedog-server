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
import io.spacedog.utils.CredentialsSettings;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class CredentialsResourceTestOften extends Assert {

	@Test
	public void deleteSuperAdminCredentials() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		Backend test2 = SpaceClient.resetTest2Backend();

		test.adminUser = SpaceClient.login(test.adminUser);
		test2.adminUser = SpaceClient.login(test2.adminUser);
		User superdog = SpaceClient.login("api", SpaceRequest.configuration().superdogName(), //
				SpaceRequest.configuration().superdogPassword());

		// forbidden to delete superadmin if last superadmin of backend
		SpaceRequest.delete("/1/credentials/" + test.adminUser.id).adminAuth(test).go(403);
		SpaceRequest.delete("/1/credentials/" + test.adminUser.id).superdogAuth().go(403);

		// superadmin test can create another superadmin (test1)
		SpaceRequest.post("/1/credentials").adminAuth(test)//
				.body("username", "test1", "password", "hi test1", //
						"email", "test1@test.com", "level", "SUPER_ADMIN")//
				.go(201);

		User test1 = SpaceClient.login("test", "test1", "hi test1");

		// superadmin test can delete superadmin test1
		SpaceRequest.delete("/1/credentials/" + test1.id).adminAuth(test).go(200);

		// test1 can no longer login
		SpaceClient.login("test", "test1", "hi test1", 401);

		// superadmin test fails to delete superdog david
		SpaceRequest.delete("/1/credentials/" + superdog.id).adminAuth(test).go(403);

		// superadmin test fails to delete superadmin of another backend
		SpaceRequest.delete("/1/credentials/" + test2.adminUser.id).adminAuth(test).go(403);

		// superadmin test2 fails to delete superadmin of another backend
		SpaceRequest.delete("/1/credentials/" + test.adminUser.id).adminAuth(test2).go(403);
	}

	@Test
	public void userIsSigningUpAndMore() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

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
				.basicAuth(test.backendId, "vince", "hi vince").go(200)//
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

		User vince = new User("test", vinceId, "vince", "hi vince", "vince@dog.com", //
				node.get("accessToken").asText(), //
				DateTime.now().plus(node.get("expiresIn").asLong()));

		// vince gets his credentials
		SpaceRequest.get("/1/credentials/" + vince.id).userAuth(vince).go(200)//
				.assertEquals("vince", "username")//
				.assertEquals("vince@dog.com", "email")//
				.assertEquals("USER", "level")//
				.assertSizeEquals(1, "roles")//
				.assertDateIsRecent("createdAt")//
				.assertDateIsRecent("updatedAt");

		// vince fails to get his credentials if wrong username
		SpaceRequest.get("/1/credentials/" + vince.id).basicAuth(test, "XXX", "hi vince").go(401);

		// vince fails to get his credentials if wrong password
		SpaceRequest.get("/1/credentials/" + vince.id).basicAuth(test, "vince", "XXX").go(401);

		// vince fails to get his credentials if wrong backend id
		SpaceRequest.get("/1/credentials/" + vince.id)//
				.basicAuth("XXX", vince.username, vince.password).go(401);

		// anonymous fails to get vince credentials
		SpaceRequest.get("/1/credentials/" + vince.id).backend(test).go(403);

		// another user fails to get vince credentials
		User fred = SpaceClient.signUp(test, "fred", "hi fred");
		SpaceRequest.get("/1/credentials/" + vince.id).userAuth(fred).go(403);

		// vince succeeds to login
		SpaceRequest.get("/1/login").userAuth(vince).go(200);

		// vince fails to login if wrong password
		SpaceRequest.get("/1/login").basicAuth(test, "vince", "XXX").go(401);
	}

	@Test
	public void testAccessTokenAndExpiration() throws InterruptedException {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		Backend test2 = SpaceClient.resetTest2Backend();

		// fails to access backend if unknown token
		SpaceRequest.get("/1/data").bearerAuth(test, "XXX").go(401);

		// vince signs up and get a brand new access token
		User vince = SpaceClient.signUp(test, "vince", "hi vince");

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
		ObjectNode node = SpaceRequest.get("/1/login").basicAuth(vince).go(200).objectNode();
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
				.assertEquals(vinceOldAccessToken, "accessToken");

		// vince can access backend with its old token
		// since it has not changed
		SpaceRequest.get("/1/data").bearerAuth(test, vinceOldAccessToken).go(200);

		// if vince logs in again with its password
		// he gets a new access token
		vince = SpaceClient.login(vince);
		assertNotEquals(vince.accessToken, vinceOldAccessToken);

		// vince can access data with its new token
		// but can also access data with its old token
		SpaceRequest.get("/1/data").bearerAuth(test, vince.accessToken).go(200);
		SpaceRequest.get("/1/data").bearerAuth(test, vinceOldAccessToken).go(200);

		// vince logs out of his newest session
		SpaceRequest.get("/1/logout").bearerAuth(vince).go(200);

		// vince can no longer access data with its new token
		// but can still access data with its old token
		SpaceRequest.get("/1/data").bearerAuth(test, vince.accessToken).go(401);
		SpaceRequest.get("/1/data").bearerAuth(test, vinceOldAccessToken).go(200);

		// vince logs out of his oldest session
		SpaceRequest.get("/1/logout").bearerAuth(test, vinceOldAccessToken).go(200);

		// vince can no longer access data with both tokens
		SpaceRequest.get("/1/data").bearerAuth(test, vince.accessToken).go(401);
		SpaceRequest.get("/1/data").bearerAuth(test, vinceOldAccessToken).go(401);

		// vince logs in with token expiration of 2 seconds
		node = SpaceRequest.get("/1/login").basicAuth(vince)//
				.queryParam("lifetime", "2") // seconds
				.go(200).objectNode();

		vince.accessToken = node.get("accessToken").asText();
		expiresIn = node.get("expiresIn").asLong();
		vince.expiresAt = DateTime.now().plus(expiresIn);

		assertTrue(expiresIn <= 2);
	}

	@Test
	public void accessTokenMaximumLifetime() throws InterruptedException {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.createCredentials(test.backendId, "fred", "hi fred");

		// admin saves settings with token max lifetime set to 2s
		CredentialsSettings settings = new CredentialsSettings();
		settings.sessionMaximumLifetime = 2; // seconds
		SpaceClient.saveSettings(test, settings);

		// fred fails to login with a token lifetime of 3s
		// since max token lifetime is 2s
		SpaceClient.login(3, fred, 403);

		// fred logs in with a token lifetime of 1s
		// since max token lifetime is 2s
		fred = SpaceClient.login(1, fred);
		SpaceRequest.get("/1/data").bearerAuth(fred).go(200);

		// after lifetime, token expires
		Thread.sleep(1000); // in milliseconds
		SpaceRequest.get("/1/data").bearerAuth(fred).go(401);
	}

	@Test
	public void deleteCredentials() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.signUp(test, "fred", "hi fred");
		User vince = SpaceClient.signUp(test, "vince", "hi vince");

		// vince and fred can login
		SpaceRequest.get("/1/login").userAuth(vince).go(200);
		SpaceRequest.get("/1/login").userAuth(fred).go(200);

		// anonymous fails to deletes vince credentials
		SpaceRequest.delete("/1/credentials/" + vince.id).backend(test).go(403);

		// fred fails to delete vince credentials
		SpaceRequest.delete("/1/credentials/" + vince.id).userAuth(fred).go(403);

		// fred deletes his own credentials
		SpaceRequest.delete("/1/credentials/" + fred.id).userAuth(fred).go(200);

		// fred fails to login from now on
		SpaceRequest.get("/1/login").userAuth(fred).go(401);

		// admin deletes vince credentials
		SpaceRequest.delete("/1/credentials/" + vince.id).adminAuth(test).go(200);

		// vince fails to login from now on
		SpaceRequest.get("/1/login").userAuth(vince).go(401);
	}

	@Test
	public void searchAndDeleteCredentials() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.signUp(test, "fred", "hi fred");
		User vince = SpaceClient.signUp(test, "vince", "hi vince");

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
				.queryParam("email", "david@spacedog.io")//
				.userAuth(fred).go(200)//
				.assertEquals(2, "total")//
				.assertSizeEquals(2, "results")//
				.assertContainsValue("vince", "username")//
				.assertContainsValue("fred", "username");

		// vince searches for credentials with specified username
		SpaceRequest.get("/1/credentials?username=fred").userAuth(vince).go(200)//
				.assertSizeEquals(1, "results")//
				.assertContainsValue("fred", "username")//
				.assertContainsValue("david@spacedog.io", "email")//
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

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		SpaceClient.signUp(test, "toto", "hi toto");

		// sign up without password should succeed

		ObjectNode node = SpaceRequest.post("/1/credentials/").backend(test)//
				.body("username", "titi", "email", "titi@dog.com").go(201)//
				.assertNotNull("passwordResetCode")//
				.objectNode();

		String titiId = node.get("id").asText();
		String passwordResetCode = node.get("passwordResetCode").asText();

		// no password user login should fail
		// I can not pass a null password anyway to the basicAuth method

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "XXX").go(401);

		// no password user trying to create password with empty reset code
		// should fail

		SpaceRequest.post("/1/credentials/" + titiId + "/password").backend(test)//
				.queryParam("passwordResetCode", "")//
				.formField("password", "hi titi").go(400);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(401);

		// no password user setting password with wrong reset code should fail

		SpaceRequest.post("/1/credentials/" + titiId + "/password").backend(test)//
				.queryParam("passwordResetCode", "XXX")//
				.formField("password", "hi titi").go(400);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(401);

		// titi inits its own password with right reset code should succeed

		SpaceRequest.post("/1/credentials/" + titiId + "/password").backend(test)//
				.formField("passwordResetCode", passwordResetCode)//
				.formField("password", "hi titi")//
				.go(200);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(200);

		// toto user changes titi password should fail

		SpaceRequest.put("/1/credentials/" + titiId + "/password")//
				.basicAuth(test, "toto", "hi toto")//
				.formField("password", "XXX")//
				.go(403);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "XXX").go(401);

		// titi changes its password should fail since password size < 6

		SpaceRequest.put("/1/credentials/" + titiId + "/password")//
				.basicAuth(test, "titi", "hi titi")//
				.formField("password", "XXX")//
				.go(400);

		// titi changes its password should succeed

		SpaceRequest.put("/1/credentials/" + titiId + "/password")//
				.basicAuth(test, "titi", "hi titi")//
				.formField("password", "hi titi 2")//
				.go(200);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi 2").go(200);

		// login with old password should fail

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(401);

		// admin user changes titi user password should succeed

		SpaceRequest.put("/1/credentials/" + titiId + "/password")//
				.basicAuth(test, "test", "hi test")//
				.formField("password", "hi titi 3")//
				.go(200);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi 3").go(200);

		// login with old password should fail

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi 2").go(401);

		// admin deletes titi password should succeed

		String newPasswordResetCode = SpaceRequest//
				.delete("/1/credentials/" + titiId + "/password")//
				.basicAuth(test, "test", "hi test")//
				.go(200)//
				.getString("passwordResetCode");

		// titi login should fail

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi 3").go(401);

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

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(200);
	}

	@Test
	public void credentialsAreDeletedWhenBackendIsDeleted() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.signUp(test, "fred", "hi fred");

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
	public void getPutAndDeleteUserCredentialsRoles() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.signUp(test, "fred", "hi fred", "fred@dog.com");

		// fred gets his credentials roles
		SpaceRequest.get("/1/credentials/" + fred.id + "/roles")//
				.userAuth(fred).go(200)//
				.assertSizeEquals(1)//
				.assertEquals("user", "0");

		// fred fails to set a role since he is no admin
		SpaceRequest.put("/1/credentials/" + fred.id + "/roles/silver").userAuth(fred).go(403);

		// admin sets fred's roles
		SpaceRequest.put("/1/credentials/" + fred.id + "/roles/silver").adminAuth(test).go(200);
		SpaceRequest.put("/1/credentials/" + fred.id + "/roles/gold").adminAuth(test).go(200);
		SpaceRequest.get("/1/credentials/" + fred.id + "/roles").adminAuth(test).go(200)//
				.assertSizeEquals(3)//
				.assertContains(TextNode.valueOf("user"))//
				.assertContains(TextNode.valueOf("silver"))//
				.assertContains(TextNode.valueOf("gold"));

		// fred fails to delete one of his roles since he is no admin
		SpaceRequest.delete("/1/credentials/" + fred.id + "/roles/silver").userAuth(fred).go(403);

		// admin deletes one of fred's roles
		SpaceRequest.delete("/1/credentials/" + fred.id + "/roles/gold").adminAuth(test).go(200);
		SpaceRequest.get("/1/credentials/" + fred.id + "/roles").adminAuth(test).go(200)//
				.assertSizeEquals(2)//
				.assertContains(TextNode.valueOf("user"))//
				.assertContains(TextNode.valueOf("silver"));

		// admin deletes all fred's roles
		SpaceRequest.delete("/1/credentials/" + fred.id + "/roles").adminAuth(test).go(200);
		SpaceRequest.get("/1/credentials/" + fred.id + "/roles").userAuth(fred).go(200)//
				.assertSizeEquals(1)//
				.assertEquals("user", "0");
	}

	@Test
	public void disableGuestSignUp() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.signUp(test, "fred", "hi fred");

		// admin disables guest sign up
		CredentialsSettings settings = new CredentialsSettings();
		settings.disableGuestSignUp = true;
		SpaceClient.saveSettings(test, settings);

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
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// get default username and password settings
		CredentialsSettings settings = SpaceClient.loadSettings(test, CredentialsSettings.class);
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
		SpaceClient.saveSettings(test, settings);

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
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.signUp(test, "fred", "hi fred");

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
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.signUp(test, "fred", "hi fred");

		// fred fails to update his password
		// since his password is not challenged
		// because authentication is done with access token
		SpaceRequest.put("/1/credentials/" + fred.id + "/password")//
				.userAuth(fred)//
				.formField("password", "hello fred")//
				.go(403)//
				.assertEquals("unchallenged-password", "error.code");

		// fred updates his password since his password is challenged
		// because authentication is done with username and password
		SpaceRequest.put("/1/credentials/" + fred.id + "/password")//
				.basicAuth(fred.backendId, fred.username, fred.password)//
				.formField("password", "hello fred")//
				.go(200);
	}

	@Test
	public void updateUsernameEmailAndPassword() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.signUp(test, "fred", "hi fred");

		// fred fails to updates his username
		// since password must be challenged
		SpaceRequest.put("/1/credentials/" + fred.id).bearerAuth(fred)//
				.body(Json.object("username", "fred2")).go(403)//
				.assertEquals("unchallenged-password", "error.code");

		// fred updates his username with a challenged password
		SpaceRequest.put("/1/credentials/" + fred.id).basicAuth(fred)//
				.body(Json.object("username", "fred2")).go(200);

		SpaceClient.login("test", "fred", "hi fred", 401);
		fred = SpaceClient.login("test", "fred2", "hi fred");

		// superadmin updates fred's email
		SpaceRequest.put("/1/credentials/" + fred.id).basicAuth(test)//
				.body(Json.object("email", "fred2@dog.com")).go(200);

		SpaceRequest.get("/1/credentials/" + fred.id).userAuth(fred)//
				.go(200).assertEquals("fred2@dog.com", "email");

		// fred fails to updates his password
		// since principal password must be challenged
		SpaceRequest.put("/1/credentials/" + fred.id).bearerAuth(fred)//
				.body(Json.object("password", "hi fred2")).go(403)//
				.assertEquals("unchallenged-password", "error.code");

		// fred updates his password
		SpaceRequest.put("/1/credentials/" + fred.id).basicAuth(fred)//
				.body(Json.object("password", "hi fred2")).go(200);

		// fred's old access token is still active
		SpaceRequest.get("/1/credentials/" + fred.id).userAuth(fred).go(200);

		// but fred's old password is not valid anymore
		SpaceClient.login("test", "fred2", "hi fred", 401);

		// fred logs in with his new username and password
		fred = SpaceClient.login("test", "fred2", "hi fred2");
	}

	@Test
	public void disableCredentials() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.signUp(test, "fred", "hi fred");

		// fred logs in
		SpaceRequest.get("/1/login").userAuth(fred).go(200);

		// anonymous fails to disable fred's credentials
		SpaceRequest.put("/1/credentials/" + fred.id + "/enabled")//
				.body(Json.toNode(false)).backend(test).go(403);

		// fred fails to disable his credentials
		SpaceRequest.put("/1/credentials/" + fred.id + "/enabled")//
				.body(Json.toNode(false)).userAuth(fred).go(403);

		// admin fails to disable fred's credentials because body not a boolean
		SpaceRequest.put("/1/credentials/" + fred.id + "/enabled")//
				.body(Json.toNode("false")).adminAuth(test).go(400);

		// only admin can disable fred's credentials
		SpaceRequest.put("/1/credentials/" + fred.id + "/enabled")//
				.body(Json.toNode(false)).adminAuth(test).go(200);

		// fred fails to login from now on
		SpaceRequest.get("/1/login").userAuth(fred).go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// fred fails to access any other resources from now on
		SpaceRequest.get("/1/data").userAuth(fred).go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// fred fails to update his credentials from now on
		SpaceRequest.put("/1/credentials/" + fred.id).userAuth(fred)//
				.body("username", "fredy").go(401);

		// anonymous fails to enable fred's credentials
		SpaceRequest.put("/1/credentials/" + fred.id + "/enabled")//
				.body(Json.toNode(true)).backend(test).go(403);

		// fred fails to enable his credentials
		SpaceRequest.put("/1/credentials/" + fred.id + "/enabled")//
				.body(Json.toNode(true)).userAuth(fred).go(401);

		// only admin can enable fred's credentials
		SpaceRequest.put("/1/credentials/" + fred.id + "/enabled")//
				.body(Json.toNode(true)).adminAuth(test).go(200);

		// fred logs in again normally
		SpaceRequest.get("/1/login").userAuth(fred).go(200);
	}
}
