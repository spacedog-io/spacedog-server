/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.joda.time.DateTime;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceRequestException;
import io.spacedog.model.CredentialsSettings;
import io.spacedog.model.EmailTemplate;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Json;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Passwords;

public class CredentialsServiceTest extends SpaceTest {

	@Test
	public void deleteSuperAdminCredentials() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog test2 = resetTest2Backend();
		SpaceDog superdog = superdog(test);

		test.login();
		test2.login();

		// forbidden to delete superadmin if last superadmin of backend
		test.delete("/1/credentials/" + test.id()).go(403);
		superdog.delete("/1/credentials/" + test.id()).go(403);

		// superadmin test can create another superadmin (test1)
		SpaceDog superfred = test.credentials()//
				.create("superfred", "hi superfred", "superfred@test.com", "superadmin")//
				.login("hi superfred");

		// superadmin test can delete superadmin superfred
		test.credentials().delete(superfred.id());

		// superfred can no longer login
		superfred.get("/1/login").go(401);

		// superdog can not be deleted
		superdog.delete("/1/credentials/me").go(404);

		// superadmin test fails to delete superadmin of another backend
		test.delete("/1/credentials/" + test2.id()).go(404);

		// superadmin test2 fails to delete superadmin of another backend
		test2.delete("/1/credentials/" + test.id()).go(404);
	}

	@Test
	public void superdogCanLoginAndAccessAllBackends() {

		// prepare
		prepareTest();
		resetTestBackend();

		// superdog with root backend id
		SpaceDog apiSuperdog = superdog();

		// superdog can access anything in root backend
		apiSuperdog.credentials().getByUsername("fred");
		apiSuperdog.settings().get(CredentialsSettings.class);

		// superdog can access any backend
		apiSuperdog.get("/1/credentials").backend("test").go(200);
		apiSuperdog.get("/1/settings/credentials").backend("test").go(200);

		// superdog with "test" backend id
		SpaceDog testSuperdog = superdog("test");

		// "test" superdog can access anything in test backend
		testSuperdog.credentials().getByUsername("fred");
		testSuperdog.settings().get(CredentialsSettings.class);

		// "test" superdog can also access any backend
		testSuperdog.get("/1/credentials").backend("api").go(200);
		testSuperdog.get("/1/settings/credentials").backend("api").go(200);
	}

	@Test
	public void testEnableAfterAndDisableAfter() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// fred logs in
		fred.login();

		// fred gets data
		fred.data().getAll().get();

		// only admins are allowed to update credentials enable after date
		try {
			fred.credentials().prepareUpdate()//
					.enableAfter(Optional7.of(DateTime.now())).go();
			fail();
		} catch (SpaceRequestException e) {
			assertEquals(403, e.httpStatus());
		}

		// only admins are allowed to update credentials disable after date
		try {
			fred.credentials().prepareUpdate()//
					.disableAfter(Optional7.of(DateTime.now())).go();
			fail();
		} catch (SpaceRequestException e) {
			assertEquals(403, e.httpStatus());
		}

		// only admins are allowed to update credentials enabled status
		try {
			fred.credentials().prepareUpdate().enabled(true).go();
			fail();
		} catch (SpaceRequestException e) {
			assertEquals(403, e.httpStatus());
		}

		// superadmin can update fred's credentials disable after date
		// before now so fred's credentials are disabled
		superadmin.credentials().prepareUpdate(fred.id())//
				.disableAfter(Optional7.of(DateTime.now().minus(100000))).go();

		// fred's credentials are disabled so he fails to gets any data
		fred.get("/1/data").go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// fred's credentials are disabled so he fails to log in
		fred.get("/1/login").go(401);

		// superadmin can update fred's credentials enable after date
		// before now and after disable after date so fred's credentials
		// are enabled again
		superadmin.credentials().prepareUpdate(fred.id())//
				.enableAfter(Optional7.of(DateTime.now().minus(100000))).go();

		// fred's credentials are enabled again so he gets data
		// with his old access token from first login
		SpaceRequest.get("/1/data").bearerAuth(fred).go(200);

		// fred's credentials are enabled again so he can log in
		fred.login();

		// superadmin updates fred's credentials disable after date
		// before now but after enable after date so fred's credentials
		// are disabled again
		superadmin.credentials().prepareUpdate(fred.id())//
				.disableAfter(Optional7.of(DateTime.now().minus(100000))).go();

		// fred's credentials are disabled so he fails to gets any data
		fred.get("/1/data").go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// fred's credentials are disabled so he fails to log in
		fred.get("/1/login").go(401);

		// superadmin updates fred's credentials to remove enable and
		// disable after dates so fred's credentials are enabled again
		superadmin.credentials().prepareUpdate(fred.id())//
				.enableAfter(Optional7.empty()).disableAfter(Optional7.empty()).go();

		// fred's credentials are enabled again so he gets data
		// with his old access token from first login
		SpaceRequest.get("/1/data").bearerAuth(fred).go(200);

		// fred's credentials are enabled again so he can log in
		fred.login();

		// superadmin fails to update fred's credentials enable after date
		// since invalid format
		superadmin.put("/1/credentials/{id}").routeParam("id", fred.id())//
				.bodyJson(ENABLE_AFTER_FIELD, "XXX").go(400);
	}

	@Test
	public void changePasswordInvalidatesAllTokens() {

		// prepare
		prepareTest();
		SpaceDog test = SpaceDog.backendId("test");
		SpaceDog superadmin = resetTestBackend();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// fred logs in
		fred.login();

		// fred logs in again creating a second session
		SpaceDog fred2 = SpaceDog.backend(test.backend())//
				.username(fred.username()).login(fred.password().get());

		// fred can access data with his first token
		fred.data().getAll().get();

		// fred can access data with his second token
		fred2.data().getAll().get();

		// superadmin updates fred's password
		String newPassword = Passwords.random();
		superadmin.credentials().prepareUpdate(fred.id())//
				.newPassword(newPassword).go();

		// fred can no longer access data with his first token now invalid
		try {
			fred.data().getAll().get();
			fail();
		} catch (SpaceRequestException e) {
			assertEquals(401, e.httpStatus());
		}

		// fred can no longer access data with his second token now invalid
		try {
			fred2.data().getAll().get();
			fail();
		} catch (SpaceRequestException e) {
			assertEquals(401, e.httpStatus());
		}

		// but fred can log in with his new password
		fred.login(newPassword);
	}

	@Test
	public void multipleInvalidPasswordChallengesDisableCredentials() {

		// prepare
		prepareTest();
		SpaceDog test = SpaceDog.backendId("test");
		SpaceDog superadmin = resetTestBackend();
		SpaceDog fred = createTempDog(superadmin, "fred");

		Credentials credentials = superadmin.credentials().get(fred.id());//
		assertEquals(0, credentials.invalidChallenges());
		assertNull(credentials.lastInvalidChallengeAt());

		// fred tries to log in with an invalid password
		SpaceRequest.get("/1/login").backend(test)//
				.basicAuth(fred.username(), "XXX").go(401);

		// fred's invalid challenges count is still zero
		// since no maximum invalid challenges set in credentials settings
		credentials = superadmin.credentials().get(fred.id());//
		assertEquals(0, credentials.invalidChallenges());
		assertNull(credentials.lastInvalidChallengeAt());

		// superadmin sets maximum invalid challenges to 2
		CredentialsSettings settings = new CredentialsSettings();
		settings.maximumInvalidChallenges = 2;
		settings.resetInvalidChallengesAfterMinutes = 1;
		superadmin.settings().save(settings);

		// fred tries to log in with an invalid password
		SpaceRequest.get("/1/login").backend(test)//
				.basicAuth(fred.username(), "XXX").go(401);

		// superadmin gets fred's credentials
		// fred has 1 invalid password challenge
		credentials = superadmin.credentials().get(fred.id());//
		assertEquals(1, credentials.invalidChallenges());
		assertNotNull(credentials.lastInvalidChallengeAt());

		// fred tries to log in with an invalid password
		SpaceRequest.get("/1/login").backend(test)//
				.basicAuth(fred.username(), "XXX").go(401);

		// superadmin gets fred's credentials; fred has 2 invalid password
		// challenge; his credentials has been disabled since equal to settings
		// max
		credentials = superadmin.credentials().get(fred.id());//
		assertFalse(credentials.enabled());
		assertEquals(2, credentials.invalidChallenges());
		assertNotNull(credentials.lastInvalidChallengeAt());

		// fred's credentials are disabled since too many invalid
		// password challenges in a period of time of 1 minutes
		// he can no longer login
		fred.get("/1/login").go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// superadmin enables fred's credentials
		superadmin.credentials().prepareUpdate(fred.id()).enabled(true).go();

		// fred can log in again
		credentials = fred.login().credentials().me();
		assertTrue(credentials.enabled());
		assertEquals(0, credentials.invalidChallenges());
		assertNull(credentials.lastInvalidChallengeAt());
	}

	@Test
	public void passwordMustChange() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog fred = createTempDog(superadmin, "fred").login();

		// fred can get data objects
		SpaceRequest.get("/1/data").basicAuth(fred).go(200);

		// superadmin forces fred to change his password
		superadmin.credentials().passwordMustChange(fred.id());

		// fred can no longer get data objects with his token
		// because he must first change his password
		fred.get("/1/data").bearerAuth(fred).go(403)//
				.assertEquals("password-must-change", "error.code");

		// fred can no longer get data objects with his password
		// because he must first change his password
		SpaceRequest.get("/1/data").basicAuth(fred).go(403)//
				.assertEquals("password-must-change", "error.code");

		// fred can change his password
		String newPassword = Passwords.random();
		fred.credentials().updateMyPassword(fred.password().get(), newPassword);
		fred.password(newPassword);

		// fred can get data objects again with basic auth
		SpaceRequest.get("/1/data").basicAuth(fred).go(200);

		// fred can log in
		fred.login();

		// fred can get data objects again with bearer auth
		SpaceRequest.get("/1/data").bearerAuth(fred).go(200);
	}

	@Test
	public void forgotPassword() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// fred can get data objects
		fred.get("/1/data").go(200);

		// to declare that you forgot your password
		// you need to pass your username
		superadmin.post("/1/credentials/forgotPassword").go(400);

		// if invalid username, you get a 404
		superadmin.post("/1/credentials/forgotPassword")//
				.bodyJson(USERNAME_PARAM, "XXX").go(404);

		// fred fails to declare "forgot password" if no
		// forgotPassword template set in mail settings
		fred.post("/1/credentials/forgotPassword")//
				.bodyJson(USERNAME_PARAM, fred.username()).go(400);

		// superadmin saves the forgotPassword email template
		EmailTemplate template = new EmailTemplate();
		template.name = "forgotPassword";
		template.from = "no-reply@api.spacedog.io";
		template.to = Lists.newArrayList("{{to}}");
		template.subject = "Password forgotten request";
		template.text = "{{passwordResetCode}}";
		superadmin.emails().saveTemplate(template);

		// fred declares he's forgot his password
		fred.credentials().forgotMyPassword();

		// fred can not pass any parameter unless they
		// are registered in the template model
		fred.post("/1/credentials/forgotPassword")//
				.bodyJson(USERNAME_PARAM, fred.username(), //
						"url", "http://localhost:8080")
				.go(400);

		// add an url parameter to the template model
		template.model = Maps.newHashMap();
		template.model.put("url", "string");
		template.text = "{{url}}?code={{passwordResetCode}}";
		superadmin.emails().saveTemplate(template);

		// fred declares he's forgot his password
		// passing an url parameter
		fred.credentials().forgotMyPassword(//
				Json.object("url", "http://localhost:8080"));

		// fred can still access services if he remembers his password
		// or if he's got a valid token
		fred.get("/1/data").go(200);
	}
}
