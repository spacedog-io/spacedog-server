/**
 * © David Attias 2015
 */
package io.spacedog.test;

import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Credentials.Results;
import io.spacedog.client.credentials.CredentialsSettings;
import io.spacedog.client.credentials.Passwords;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.client.http.SpaceRequestException;
import io.spacedog.utils.Json;

public class CredentialsServiceTest2 extends SpaceTest {

	@Test
	public void userIsSigningUpAndMore() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		superadmin.credentials().enableGuestSignUp(true);

		// fails since empty user body
		guest.post("/1/credentials/").bodyJson(Json.object()).go(400);

		// fails since no username
		guest.post("/1/credentials/")//
				.bodyJson("password", "hi titi", "email", "titi@dog.com").go(400);

		// fails since no email
		guest.post("/1/credentials/")//
				.bodyJson("username", "titi", "password", "hi titi").go(400);

		// fails since username too small
		guest.post("/1/credentials/")//
				.bodyJson("username", "ti", "password", "hi titi").go(400);

		// fails since password too small
		guest.post("/1/credentials/")//
				.bodyJson("username", "titi", "password", "hi", "email", "titi@dog.com")//
				.go(400);

		// vince signs up
		String vinceId = guest.post("/1/credentials")
				.bodyJson("username", "vince", "password", "hi vince", "email", "vince@dog.com")//
				.go(201).getString("id");

		// vince fails to sign up again since his credentials already exixts
		guest.post("/1/credentials")//
				.bodyJson("username", "vince", "password", "hello boby", "email", "vince@dog.com")//
				.go(400)//
				.assertEquals("already-exists", "error.code");

		// vince logs in
		ObjectNode node = SpaceRequest.get("/1/login")//
				.backend(guest).basicAuth("vince", "hi vince").go(200)//
				.assertPresent("accessToken")//
				.assertPresent("expiresIn")//
				.assertEquals(vinceId, "credentials.id")//
				.assertEquals("vince", "credentials.username")//
				.assertEquals("vince@dog.com", "credentials.email")//
				.assertEquals(true, "credentials.enabled")//
				.assertSizeEquals(1, "credentials.roles")//
				.assertEquals("user", "credentials.roles.0")//
				.assertPresent("credentials.createdAt")//
				.assertPresent("credentials.updatedAt")//
				.asJsonObject();

		SpaceDog vince = SpaceDog.dog().username("vince")//
				.password("hi vince").email("vince@dog.com")//
				.accessToken(node.get("accessToken").asText()) //
				.expiresAt(DateTime.now().plus(node.get("expiresIn").asLong()));

		// vince gets his credentials
		vince.get("/1/credentials/" + vince.id()).go(200)//
				.assertEquals("vince", "username")//
				.assertEquals("vince@dog.com", "email")//
				.assertSizeEquals(1, "roles")//
				.assertDateIsRecent("createdAt")//
				.assertDateIsRecent("updatedAt");

		// vince fails to get his credentials if wrong username
		SpaceRequest.get("/1/credentials/" + vince.id())//
				.backend(superadmin).basicAuth("XXX", "hi vince").go(401);

		// vince fails to get his credentials if wrong password
		SpaceRequest.get("/1/credentials/" + vince.id())//
				.backend(superadmin).basicAuth("vince", "XXX").go(401);

		// anonymous fails to get vince credentials
		guest.get("/1/credentials/" + vince.id()).go(401);

		// another user fails to get vince credentials
		SpaceDog fred = signUpTempDog(superadmin.backend(), "fred");
		fred.get("/1/credentials/" + vince.id()).go(403);

		// vince succeeds to login
		vince.login();

		// vince fails to login if wrong password
		SpaceRequest.get("/1/login").backend(superadmin).basicAuth("vince", "XXX").go(401);
	}

	@Test
	public void testAccessTokenAndExpiration() throws InterruptedException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();

		// fails to access backend if unknown token
		SpaceRequest.get("/1/data").backend(superadmin).bearerAuth("XXX").go(401);

		// superadmin creates vince
		SpaceDog vince = createTempDog(superadmin, "vince");

		// vince logs in and gets a brand new access token
		vince.login();

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
		ObjectNode node = SpaceRequest.get("/1/login")//
				.basicAuth(vince).go(200).asJsonObject();
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
		SpaceRequest.get("/1/data").backend(superadmin).bearerAuth(vinceOldAccessToken).go(200);

		// if vince logs in again with its password
		// he gets a new access token
		vince.login();
		assertNotEquals(vince.accessToken().get(), vinceOldAccessToken);

		// vince can access data with its new token
		// but can also access data with its old token
		SpaceRequest.get("/1/data").backend(superadmin).bearerAuth(vince.accessToken().get()).go(200);
		SpaceRequest.get("/1/data").backend(superadmin).bearerAuth(vinceOldAccessToken).go(200);

		// vince logs out of his newest session
		SpaceRequest.get("/1/logout").bearerAuth(vince).go(200);

		// vince can no longer access data with its new token
		// but can still access data with its old token
		SpaceRequest.get("/1/data").backend(superadmin).bearerAuth(vince.accessToken().get()).go(401);
		SpaceRequest.get("/1/data").backend(superadmin).bearerAuth(vinceOldAccessToken).go(200);

		// vince logs out of his oldest session
		SpaceRequest.get("/1/logout").backend(superadmin).bearerAuth(vinceOldAccessToken).go(200);

		// vince can no longer access data with both tokens
		SpaceRequest.get("/1/data").backend(superadmin).bearerAuth(vince.accessToken().get()).go(401);
		SpaceRequest.get("/1/data").backend(superadmin).bearerAuth(vinceOldAccessToken).go(401);

		// vince logs in with token expiration of 2 seconds
		node = SpaceRequest.get("/1/login").basicAuth(vince)//
				.queryParam("lifetime", 2) // seconds
				.go(200).asJsonObject();

		vince.accessToken(node.get("accessToken").asText());
		expiresIn = node.get("expiresIn").asLong();
		vince.expiresAt(DateTime.now().plus(expiresIn));

		assertTrue(expiresIn <= 2);
	}

	@Test
	public void accessTokenMaximumLifetime() throws InterruptedException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// admin saves settings with token max lifetime set to 3s
		CredentialsSettings settings = new CredentialsSettings();
		settings.sessionMaximumLifetime = 3; // seconds
		superadmin.settings().save(settings);

		// fred fails to login with a token lifetime of 4s
		// since max token lifetime is 3s
		SpaceRequest.get("/1/login").basicAuth(fred)//
				.queryParam(LIFETIME_PARAM, 4).go(403);

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
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog fred = createTempDog(superadmin, "fred");
		SpaceDog vince = createTempDog(superadmin, "vince");

		// vince and fred can login
		vince.login();
		fred.login();

		// anonymous fails to deletes vince credentials
		assertHttpError(401, () -> guest.credentials().delete(vince.id()));

		// fred fails to delete vince credentials
		assertHttpError(403, () -> fred.credentials().delete(vince.id()));

		// fred deletes his own credentials
		fred.credentials().delete();

		// fred fails to login from now on
		fred.get("/1/login").go(401);

		// admin deletes vince credentials
		superadmin.credentials().delete(vince.id());

		// vince fails to login from now on
		vince.get("/1/login").go(401);
	}

	@Test
	public void searchAndDeleteCredentials() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		createTempDog(superadmin, "fred");
		SpaceDog vince = createTempDog(superadmin, "vince");

		// vince not authaurized to search for credentials
		assertHttpError(403, () -> vince.credentials().getAll());

		// superadmin searches for all credentials
		Results results = superadmin.credentials().getAll();

		assertEquals(3, results.total);
		assertEquals(3, results.results.size());
		assertContainsCredsOf("superadmin", results.results);
		assertContainsCredsOf("vince", results.results);
		assertContainsCredsOf("fred", results.results);

		// superadmin searches for user credentials
		results = superadmin.credentials().getAll("user");
		assertEquals(2, results.total);
		assertEquals(2, results.results.size());
		assertContainsCredsOf("vince", results.results);
		assertContainsCredsOf("fred", results.results);

		// superadmin searches for credentials with a specified email
		results = superadmin.credentials().getAll("platform@spacedog.io");
		assertEquals(3, results.total);
		assertEquals(3, results.results.size());
		assertContainsCredsOf("superadmin", results.results);
		assertContainsCredsOf("vince", results.results);
		assertContainsCredsOf("fred", results.results);

		// superadmin searches for credentials with specified username
		results = superadmin.credentials().getAll("fred");
		assertEquals(1, results.total);
		assertEquals(1, results.results.size());
		assertContainsCredsOf("fred", results.results);

		// superadmin deletes all credentials but superadmins
		superadmin.credentials().deleteAllButSuperAdmins();

		results = superadmin.credentials().getAll();
		assertEquals(1, results.total);
		assertEquals(1, results.results.size());
		assertContainsCredsOf("superadmin", results.results);
	}

	private void assertContainsCredsOf(String username, List<Credentials> results) {
		for (Credentials credentials : results)
			if (credentials.username().equals(username))
				return;

		fail("credentials [" + username + "] not found");
	}

	@Test
	public void setAndResetPassword() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		superadmin.credentials().enableGuestSignUp(true);
		SpaceDog fred = createTempDog(superadmin, "fred");
		SpaceDog titi = SpaceDog.dog().username("titi");

		// sign up without password should succeed
		ObjectNode node = guest.post("/1/credentials/")//
				.bodyJson("username", titi.username(), "email", "titi@dog.com")//
				.go(201)//
				.assertNotNull("passwordResetCode")//
				.asJsonObject();

		String titiId = node.get("id").asText();
		String passwordResetCode = node.get("passwordResetCode").asText();

		// no password user login should fail
		// I can not pass a null password anyway to the basicAuth method
		assertHttpError(401, () -> titi.login("XXX"));

		// guest fails to set password with empty reset code
		assertHttpError(401, () -> guest.credentials()//
				.setPasswordWithCode(titiId, "hi titi", ""));

		// titi fails to login since no password set yet
		assertHttpError(401, () -> titi.login("hi titi"));

		// guest fails to set password with wrong reset
		assertHttpError(403, () -> guest.credentials()//
				.setPasswordWithCode(titiId, "hi titi", "XXX"));

		// titi fails to login since no password set yet
		assertHttpError(401, () -> titi.login("hi titi"));

		// titi inits its own password with right reset code should succeed
		guest.credentials().setPasswordWithCode(//
				titiId, "hi titi", passwordResetCode);

		// titi logs in successfully
		titi.login("hi titi");

		// fred fails to changes titi password since not allowed
		assertHttpError(403, () -> fred.credentials()//
				.setPassword(titiId, fred.password().get(), "XXX"));

		// titi fails to login with fred's password since not set
		assertHttpError(401, () -> titi.login("XXX"));

		// titi fails to change his password since password size < 6
		assertHttpError(400, () -> titi.credentials()//
				.setMyPassword("hi titi", "XXX"));

		// titi changes its password
		titi.credentials().setMyPassword("hi titi", "hi titi 2");

		// titi logs in with new password
		titi.login("hi titi 2");

		// titi fails to login with old password
		assertHttpError(401, () -> titi.login("hi titi"));

		// superadmin sets titi with new password
		superadmin.credentials().setPassword(titi.id(), //
				superadmin.password().get(), "hi titi 3");

		// titi logs in with new password
		titi.login("hi titi 3");

		// titi fails to log in with old password
		assertHttpError(401, () -> titi.login("hi titi 2"));

		// superadmin deletes titi password should succeed
		String newPasswordResetCode = superadmin.credentials()//
				.resetPassword(titi.id());

		// titi login should fail
		assertHttpError(401, () -> titi.login("hi titi 3"));

		// titi fails to set his password with old reset code
		assertHttpError(403, () -> guest.credentials()//
				.setPasswordWithCode(titi.id(), "hi titi", passwordResetCode));

		// titi sets his password with new reset code
		guest.credentials().setPasswordWithCode(//
				titi.id(), "hi titi", newPasswordResetCode);

		// titi logs in with new password
		titi.login("hi titi");
	}

	@Test
	public void getPutAndDeleteUserCredentialsRoles() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend().login();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// test gets his credentials roles
		Set<String> roles = superadmin.credentials().getAllRoles(superadmin.id());
		assertEquals(Sets.newHashSet("superadmin"), roles);

		// fred gets his credentials roles
		roles = fred.credentials().getAllRoles(fred.id());
		assertEquals(Sets.newHashSet("user"), roles);

		// fred fails to set a role since he is no admin
		fred.put("/1/credentials/{id}/roles/silver")//
				.routeParam("id", fred.id()).go(403);

		// admin sets fred's roles
		superadmin.credentials().setRole(fred.id(), "silver");
		superadmin.credentials().setRole(fred.id(), "gold");
		roles = fred.credentials().getAllRoles(fred.id());
		assertEquals(Sets.newHashSet("user", "silver", "gold"), roles);

		// fred fails to delete one of his roles since he is no admin
		fred.delete("/1/credentials/{id}/roles/silver")//
				.routeParam("id", fred.id()).go(403);

		// admin deletes one of fred's roles
		superadmin.credentials().unsetRole(fred.id(), "gold");
		roles = fred.credentials().getAllRoles(fred.id());
		assertEquals(Sets.newHashSet("user", "silver"), roles);

		// admin deletes all fred's roles
		superadmin.credentials().unsetAllRoles(fred.id());
		roles = superadmin.credentials().getAllRoles(fred.id());
		assertTrue(roles.isEmpty());

		// fred can not access user authorized services
		// anymore since he's got no roles
		fred.get("/1/credentials/me").go(403);

		// superadmin gives fred admin role
		superadmin.credentials().setRole(fred.id(), "admin");
		roles = fred.credentials().getAllRoles(fred.id());
		assertEquals(Sets.newHashSet("admin"), roles);

		// fred fails to give himself superadmin role
		// since he is only admin
		fred.put("/1/credentials/{id}/roles/superadmin")//
				.routeParam("id", fred.id()).go(403);

		// fred can now give himself user role
		fred.credentials().setRole(fred.id(), "user");
		roles = fred.credentials().getAllRoles(fred.id());
		assertEquals(Sets.newHashSet("user", "admin"), roles);

		// test super admin fails to give himself superdog role
		// since he is only super admin
		superadmin.put("/1/credentials/{id}/roles/superdog")//
				.routeParam("id", superadmin.id()).go(403);
	}

	@Test
	public void disableGuestSignUp() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// admin disables guest sign up
		superadmin.credentials().enableGuestSignUp(false);

		// guest can not create credentials
		guest.post("/1/credentials")//
				.bodyJson("username", "vince", "password", "hi vince", "email", "vince@dog.com")//
				.go(401);

		// fred fails to create credentials if no email
		fred.post("/1/credentials").bodyJson("username", "vince").go(400);

		// fred can create credentials for someone
		ObjectNode node = fred.post("/1/credentials")//
				.bodyJson("username", "vince", "email", "vince@dog.com")//
				.go(201).asJsonObject();

		String vinceId = node.get("id").asText();
		String resetCode = node.get("passwordResetCode").asText();

		// guest can set password if he receives a password reset code
		guest.credentials().setPasswordWithCode(vinceId, "hi vince", resetCode);
	}

	@Test
	public void testUsernameAndPasswordRegexSettings() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		superadmin.credentials().enableGuestSignUp(true);

		// get default username and password settings
		CredentialsSettings settings = superadmin.settings().get(CredentialsSettings.class);
		assertNotNull(settings.usernameRegex);
		assertNotNull(settings.passwordRegex);

		// invalid username considering default username regex
		SpaceRequest.post("/1/credentials").backend(superadmin)//
				.bodyJson("username", "vi", "password", "hi", "email", "vince@dog.com")//
				.go(400);

		// invalid password considering default password regex
		SpaceRequest.post("/1/credentials").backend(superadmin)//
				.bodyJson("username", "vince", "password", "hi", "email", "vince@dog.com")//
				.go(400);

		// valid username and password considering default credentials settings
		SpaceRequest.post("/1/credentials").backend(superadmin)//
				.bodyJson("username", "vince", "password", "hi vince", "email", "vince@dog.com")//
				.go(201);

		// set username and password specific regex
		settings.usernameRegex = "[a-zA-Z]{3,}";
		settings.passwordRegex = "[a-zA-Z]{3,}";
		superadmin.settings().save(settings);

		// invalid username considering new username regex
		SpaceRequest.post("/1/credentials").backend(superadmin)//
				.bodyJson("username", "nath.lopez", "password", "hi nath", "email", "nath@dog.com")//
				.go(400);

		// invalid password considering new password regex
		SpaceRequest.post("/1/credentials").backend(superadmin)//
				.bodyJson("username", "nathlopez", "password", "hi nath", "email", "nath@dog.com")//
				.go(400);

		// valid username and password considering credentials settings
		SpaceRequest.post("/1/credentials").backend(superadmin)//
				.bodyJson("username", "nathlopez", "password", "hinath", "email", "nath@dog.com")//
				.go(201);
	}

	@Test
	public void adminCreatesOthersAdminCredentials() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// fred fails to create admin credentials
		fred.post("/1/credentials")//
				.bodyJson("username", "vince", "password", "hi vince", //
						"email", "vince@dog.com", "roles", Json.array("admin"))//
				.go(403);

		// test (backend default superadmin) creates credentials for new
		// superadmin
		SpaceDog superadmin2 = superadmin.credentials().create("superadmin2", //
				Passwords.random(), "superadmin2@dog.com", Roles.superadmin);

		Credentials credentials = superadmin2.credentials().me();

		assertTrue(credentials.isSuperAdmin());
		assertEquals("superadmin2", credentials.username());
		assertEquals("superadmin2@dog.com", credentials.email().get());
		assertEquals(Sets.newHashSet(Roles.superadmin), credentials.roles());

		// superadmin2 creates credentials for new admin
		SpaceDog admin1 = superadmin2.credentials().create("admin1", //
				Passwords.random(), "admin1@dog.com", Roles.admin);

		credentials = admin1.credentials().me();

		assertTrue(credentials.isAdmin());
		assertEquals("admin1", credentials.username());
		assertEquals("admin1@dog.com", credentials.email().get());
		assertEquals(Sets.newHashSet(Roles.admin), credentials.roles());
	}

	@Test
	public void passwordIsChangedOnlyIfPasswordHasBeenChecked() {

		// TODO move these tests to the setAndResetPasswords test

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog fred = createTempDog(superadmin, "fred").login();

		// fred fails to set his password
		// since his password is not challenged
		// because authentication is done with access token
		SpaceRequest.post("/1/credentials/" + fred.id() + "/_set_password")//
				.bearerAuth(fred)//
				.bodyJson("password", "hello fred")//
				.go(403)//
				.assertEquals("unchallenged-password", "error.code");

		// fred updates his password since his password is challenged
		fred.credentials().setMyPassword(fred.password().get(), "hello fred");
	}

	@Test
	public void updateUsernameEmailAndPassword() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog fred = createTempDog(superadmin, "fred").login();

		// fred fails to updates his username
		// since password must be challenged
		SpaceRequest.put("/1/credentials/" + fred.id()).bearerAuth(fred)//
				.bodyJson("username", "fred2").go(403)//
				.assertEquals("unchallenged-password", "error.code");

		// fred updates his username with a challenged password
		SpaceRequest.put("/1/credentials/" + fred.id()).basicAuth(fred)//
				.bodyJson("username", "fred2").go(200);

		// fred can no longer login with old username
		SpaceRequest.get("/1/login").backend(superadmin).basicAuth("fred", "hi fred").go(401);

		// fred can login with his new username
		fred.username("fred2").login();

		// superadmin updates fred's email
		superadmin.put("/1/credentials/" + fred.id())//
				.bodyJson("email", "fred2@dog.com").go(200);

		fred.get("/1/credentials/" + fred.id())//
				.go(200).assertEquals("fred2@dog.com", "email");

		// fred fails to updates his password
		// since principal password must be challenged
		SpaceRequest.put("/1/credentials/" + fred.id()).bearerAuth(fred)//
				.bodyJson("password", "hi fred2").go(403)//
				.assertEquals("unchallenged-password", "error.code");

		// fred updates his password
		fred.credentials().setMyPassword(fred.password().get(), "hi fred2");

		// fred's old access token is not valid anymore
		SpaceRequest.get("/1/credentials/" + fred.id()).bearerAuth(fred).go(401);

		// fred's old password is not valid anymore
		SpaceRequest.get("/1/login").basicAuth(fred).go(401);

		// fred can login with his new password
		fred.password("hi fred2").login();
	}

	@Test
	public void disableCredentials() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// fred logs in
		fred.login();

		// anonymous fails to disable fred's credentials
		assertHttpError(401, () -> guest.credentials().disable(fred.id()));

		// fred fails to disable his credentials
		assertHttpError(403, () -> fred.credentials().disable(fred.id()));

		// only superadmins can disable fred's credentials
		superadmin.credentials().disable(fred.id());

		// fred fails to login from now on
		SpaceRequestException sre = assertHttpError(401, () -> fred.login());
		assertEquals("disabled-credentials", sre.serverErrorCode());

		// fred fails to access any resources from now on
		// with basic authentication scheme
		SpaceRequest.get("/1/data").basicAuth(fred).go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// fred fails to access any other resources from now on
		// with bearer authentication scheme
		SpaceRequest.get("/1/data").bearerAuth(fred).go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// fred fails to update his credentials from now on
		assertHttpError(401, () -> fred.credentials()//
				.prepareUpdate(fred.id()).username("freddy").go());

		// anonymous fails to enable fred's credentials
		assertHttpError(401, () -> guest.credentials().enable(fred.id()));

		// fred fails to enable his credentials
		assertHttpError(401, () -> fred.credentials().enable(fred.id()));

		// only superadmin can enable fred's credentials
		superadmin.credentials().enable(fred.id());

		// fred logs in again normally
		fred.login();
	}

	@Test
	public void expiredSessionsArePurged() throws InterruptedException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();

		// superadmin logs in with session lifetime of 1 second
		String firstToken = superadmin.login(1).accessToken().get();

		// wait for session to expire
		Thread.sleep(1000);

		// check session has expired
		SpaceRequest.get("/1/data").backend(superadmin).bearerAuth(firstToken).go(401)//
				.assertEquals("expired-access-token", "error.code");

		// check session has expired a second time
		// this means expired session has not yet been purged
		SpaceRequest.get("/1/data").backend(superadmin).bearerAuth(firstToken).go(401)//
				.assertEquals("expired-access-token", "error.code");

		// superadmin logs in again 9 times
		// this means superadmin has 10 sessions
		superadmin.login();
		superadmin.login();
		superadmin.login();
		superadmin.login();
		superadmin.login();
		superadmin.login();
		superadmin.login();
		superadmin.login();
		superadmin.login();

		// this means old session are not yet purged
		// since oinly 10 latest sessions are kept
		SpaceRequest.get("/1/data").backend(superadmin).bearerAuth(firstToken).go(401)//
				.assertEquals("expired-access-token", "error.code");

		// superadmin logs in again an eleventh time
		superadmin.login();

		// first superadmin session has been purged
		// since only 10 latest sessions are kept
		// this means first session token is no longer present
		SpaceRequest.get("/1/data").backend(superadmin).bearerAuth(firstToken).go(401)//
				.assertEquals("invalid-access-token", "error.code");
	}

	@Test
	public void authenticateRequestsViaAccessTokenAsQueryParam() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();

		// superadmin logs in to get access token
		superadmin.login();

		// superadmin fails to get his credentials
		// via an invalid access token as query param
		guest.get("/1/credentials")//
				.queryParam("accessToken", "XXX").go(401);

		// superadmin gets his credentials
		// via valid access token as query param
		guest.get("/1/credentials")//
				.queryParam("accessToken", superadmin.accessToken().get())//
				.go(200)//
				.assertEquals(superadmin.id(), "results.0.id");
	}
}
