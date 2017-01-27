/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.joda.time.DateTime;
import org.junit.Test;

import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.CredentialsSettings;
import io.spacedog.utils.Passwords;

public class CredentialsResourceTest extends SpaceTest {

	@Test
	public void deleteSuperAdminCredentials() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog test2 = resetTest2Backend();

		test.login();
		test2.login();
		SpaceDog superdog = SpaceDog.backend("api")//
				.username(SpaceRequest.env().get("spacedog.superdog.username")) //
				.login(SpaceRequest.env().get("spacedog.superdog.password"));

		// forbidden to delete superadmin if last superadmin of backend
		SpaceRequest.delete("/1/credentials/" + test.id()).adminAuth(test).go(403);
		SpaceRequest.delete("/1/credentials/" + test.id()).superdogAuth().go(403);

		// superadmin test can create another superadmin (test1)
		SpaceRequest.post("/1/credentials").adminAuth(test)//
				.body("username", "test1", "password", "hi test1", //
						"email", "test1@test.com", "level", "SUPER_ADMIN")//
				.go(201);

		SpaceDog test1 = SpaceDog.backend("test").username("test1").login("hi test1");

		// superadmin test can delete superadmin test1
		test.credentials().delete(test1.id());

		// test1 can no longer login
		test1.get("/1/login").go(401);

		// superadmin test fails to delete superdog
		SpaceRequest.delete("/1/credentials/" + superdog.id()).adminAuth(test).go(403);

		// superadmin test fails to delete superadmin of another backend
		SpaceRequest.delete("/1/credentials/" + test2.id()).adminAuth(test).go(403);

		// superadmin test2 fails to delete superadmin of another backend
		SpaceRequest.delete("/1/credentials/" + test.id()).adminAuth(test2).go(403);
	}

	@Test
	public void superdogCanLoginAndAccessAllBackends() {

		// prepare
		prepareTest();
		resetTestBackend();
		SpaceEnv env = SpaceRequest.env();

		// superdog logs in with the root backend
		SpaceDog apiSuperdog = SpaceDog.backend("api")//
				.username(env.get("spacedog.superdog.username")) //
				.login(env.get("spacedog.superdog.password"));
		String apiToken = apiSuperdog.accessToken().get();

		// superdog can access anything in any backend
		SpaceRequest.get("/1/backend").backendId("api").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/backend").backendId("test").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/data").backendId("api").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/data").backendId("test").bearerAuth(apiToken).go(200);

		// superdog logs with the "test" backend
		SpaceDog testSuperdog = SpaceDog.backend("test")//
				.username(env.get("spacedog.superdog.username")) //
				.login(env.get("spacedog.superdog.password"));
		String testToken = testSuperdog.accessToken().get();
		assertNotEquals(testToken, apiToken);

		// superdog credentials backendId is not changed
		// by login into "test" backend
		SpaceRequest.get("/1/credentials/" + testSuperdog.id())//
				.superdogAuth().go(200)//
				.assertEquals("api", "backendId");

		// superdog can still access anything in any backend with the old token
		SpaceRequest.get("/1/backend").backendId("api").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/backend").backendId("test").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/data").backendId("api").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/data").backendId("test").bearerAuth(apiToken).go(200);

		// superdog can also access anything in any backend with the new token
		SpaceRequest.get("/1/backend").backendId("api").bearerAuth(testToken).go(200);
		SpaceRequest.get("/1/backend").backendId("test").bearerAuth(testToken).go(200);
		SpaceRequest.get("/1/data").backendId("api").bearerAuth(testToken).go(200);
		SpaceRequest.get("/1/data").backendId("test").bearerAuth(testToken).go(200);

		// superdog logout from his "test" session
		testSuperdog.logout();

		// superdog can not access anything from his "test" token
		SpaceRequest.get("/1/data").backendId("api").bearerAuth(testToken).go(401);
		SpaceRequest.get("/1/backend").backendId("api").bearerAuth(testToken).go(401);

		// superdog can still access anything from his "api" token
		SpaceRequest.get("/1/data").backendId("api").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/backend").backendId("api").bearerAuth(apiToken).go(200);

		// superdog logout from his "api" session
		// and can not access anything this token
		apiSuperdog.logout();
		SpaceRequest.get("/1/data").backendId("api").bearerAuth(apiToken).go(401);
		SpaceRequest.get("/1/backend").backendId("api").bearerAuth(apiToken).go(401);
	}

	@Test
	public void testEnableAfterAndDisableAfter() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = createTempUser(test, "fred");

		// fred logs in
		fred.login();

		// fred gets data
		SpaceRequest.get("/1/data").userAuth(fred).go(200);

		// fred is not allowed to update his credentials enable after date
		SpaceRequest.put("/1/credentials/{id}")//
				.routeParam("id", fred.id()).basicAuth(fred)//
				.body(FIELD_ENABLE_AFTER, DateTime.now())//
				.go(403);

		// fred is not allowed to update his credentials enable after date
		SpaceRequest.put("/1/credentials/{id}")//
				.routeParam("id", fred.id()).basicAuth(fred)//
				.body(FIELD_DISABLE_AFTER, DateTime.now())//
				.go(403);

		// fred is not allowed to update his credentials enabled status
		SpaceRequest.put("/1/credentials/{id}")//
				.routeParam("id", fred.id()).basicAuth(fred)//
				.body(FIELD_ENABLED, true)//
				.go(403);

		// superadmin can update fred's credentials disable after date
		// before now so fred's credentials are disabled
		SpaceRequest.put("/1/credentials/{id}")//
				.routeParam("id", fred.id())//
				.adminAuth(test)//
				.body(FIELD_DISABLE_AFTER, DateTime.now().minus(100000))//
				.go(200);

		// fred's credentials are disabled so he fails to gets any data
		SpaceRequest.get("/1/data").userAuth(fred).go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// fred's credentials are disabled so he fails to log
		fred.get("/1/login").go(401);

		// superadmin can update fred's credentials enable after date
		// before now and after disable after date so fred's credentials
		// are enabled again
		SpaceRequest.put("/1/credentials/{id}")//
				.routeParam("id", fred.id())//
				.adminAuth(test)//
				.body(FIELD_ENABLE_AFTER, DateTime.now().minus(100000))//
				.go(200);

		// fred's credentials are enabled again so he gets data
		// with his old access token from first login
		SpaceRequest.get("/1/data").bearerAuth(fred).go(200);

		// fred's credentials are enabled again so he can log in
		fred.login();

		// superadmin updates fred's credentials disable after date
		// before now but after enable after date so fred's credentials
		// are disabled again
		SpaceRequest.put("/1/credentials/{id}")//
				.routeParam("id", fred.id())//
				.adminAuth(test)//
				.body(FIELD_DISABLE_AFTER, DateTime.now().minus(100000))//
				.go(200);

		// fred's credentials are disabled so he fails to gets any data
		SpaceRequest.get("/1/data").userAuth(fred).go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// fred's credentials are disabled so he fails to log in
		fred.get("/1/login").go(401);

		// superadmin updates fred's credentials to remove enable and
		// disable after dates so fred's credentials are enabled again
		SpaceRequest.put("/1/credentials/{id}")//
				.routeParam("id", fred.id())//
				.adminAuth(test)//
				.body(FIELD_DISABLE_AFTER, null, FIELD_ENABLE_AFTER, null)//
				.go(200);

		// fred's credentials are enabled again so he gets data
		// with his old access token from first login
		SpaceRequest.get("/1/data").bearerAuth(fred).go(200);

		// fred's credentials are enabled again so he can log in
		fred.login();

		// superadmin fails to update fred's credentials enable after date
		// since invalid format
		SpaceRequest.put("/1/credentials/{id}")//
				.routeParam("id", fred.id())//
				.adminAuth(test)//
				.body(FIELD_ENABLE_AFTER, "XXX")//
				.go(400);
	}

	@Test
	public void changePasswordInvalidatesAllTokens() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = createTempUser(test, "fred");

		// fred logs in
		fred.login();

		// fred logs in again creating a second session
		SpaceDog fred2 = SpaceDog.backend(fred.backendId())//
				.username(fred.username()).login(fred.password().get());

		// fred can access data with his first token
		SpaceRequest.get("/1/data").bearerAuth(fred).go(200);

		// fred can access data with his second token
		SpaceRequest.get("/1/data").bearerAuth(fred2).go(200);

		// superadmin updates fred's password
		String newPassword = Passwords.random();
		SpaceRequest.put("/1/credentials/{id}")//
				.routeParam("id", fred.id())//
				.adminAuth(test)//
				.body(FIELD_PASSWORD, newPassword)//
				.go(200);

		// fred can no longer access data with his first token now invalid
		SpaceRequest.get("/1/data").bearerAuth(fred).go(401);

		// fred can no longer access data with his second token now invalid
		SpaceRequest.get("/1/data").bearerAuth(fred2).go(401);

		// but fred can log in with his new password
		fred.login(newPassword);
	}

	@Test
	public void multipleInvalidPasswordChallengesDisableCredentials() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = createTempUser(test, "fred");

		test.get("/1/credentials/{id}").routeParam("id", fred.id()).go(200)//
				.assertEquals(0, FIELD_INVALID_CHALLENGES)//
				.assertNotPresent(FIELD_LAST_INVALID_CHALLENGE_AT);

		// fred tries to log in with an invalid password
		SpaceRequest.get("/1/login")//
				.basicAuth(fred.backendId(), fred.username(), "XXX").go(401);

		// fred's invalid challenges count is still zero
		// since no maximum invalid challenges set in credentials settings
		test.get("/1/credentials/{id}").routeParam("id", fred.id()).go(200)//
				.assertEquals(0, FIELD_INVALID_CHALLENGES)//
				.assertNotPresent(FIELD_LAST_INVALID_CHALLENGE_AT);

		// superadmin sets maximum invalid challenges to 2
		CredentialsSettings settings = new CredentialsSettings();
		settings.maximumInvalidChallenges = 2;
		settings.resetInvalidChallengesAfterMinutes = 1;
		test.settings().save(settings);

		// fred tries to log in with an invalid password
		SpaceRequest.get("/1/login")//
				.basicAuth(fred.backendId(), fred.username(), "XXX").go(401);

		// superadmin gets fred's credentials
		// fred has 1 invalid password challenge
		test.get("/1/credentials/{id}").routeParam("id", fred.id()).go(200)//
				.assertEquals(1, FIELD_INVALID_CHALLENGES)//
				.assertPresent(FIELD_LAST_INVALID_CHALLENGE_AT);

		// fred tries to log in with an invalid password
		SpaceRequest.get("/1/login")//
				.basicAuth(fred.backendId(), fred.username(), "XXX").go(401);

		// superadmin gets fred's credentials; fred has 2 invalid password
		// challenge; his credentials has been disabled since equal to settings
		// max
		test.get("/1/credentials/{id}").routeParam("id", fred.id()).go(200)//
				.assertEquals(false, FIELD_ENABLED)//
				.assertEquals(2, FIELD_INVALID_CHALLENGES)//
				.assertPresent(FIELD_LAST_INVALID_CHALLENGE_AT);

		// fred's credentials are disabled since too many invalid
		// password challenges in a period of time of 1 minutes
		// he can no longer login
		fred.get("/1/login").go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// superadmin enables fred's credentials
		test.put("/1/credentials/{id}").routeParam("id", fred.id())//
				.body(FIELD_ENABLED, true).go(200);

		// fred can log in again
		fred.get("/1/login").go(200)//
				.assertEquals(true, "credentials." + FIELD_ENABLED)//
				.assertEquals(0, "credentials." + FIELD_INVALID_CHALLENGES)//
				.assertNotPresent("credentials." + FIELD_LAST_INVALID_CHALLENGE_AT);
	}
}
