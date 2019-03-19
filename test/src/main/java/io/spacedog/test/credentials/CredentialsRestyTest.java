/**
 * © David Attias 2015
 */
package io.spacedog.test.credentials;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.CredentialsSettings;
import io.spacedog.client.credentials.Passwords;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.email.EmailTemplate;
import io.spacedog.client.http.SpaceException;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Json;
import okhttp3.OkHttpClient;

public class CredentialsRestyTest extends SpaceTest {

	@Test
	public void getAndDeleteByUsername() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		// superadmin creates fred's credentials
		SpaceDog fred = createTempDog(superadmin, "fred");

		// superadmin searches for fred's credentials by username
		Optional<Credentials> opt = superadmin.credentials().getByUsername(fred.username());
		assertEquals(fred.credentials().me(), opt.get());

		// fred doesn't have permission to search for credentials since not an admin
		assertHttpError(403, () -> fred.credentials().getByUsername(fred.username()));

		// superadmin deletes fred's credentials
		superadmin.credentials().deleteByUsername(fred.username());
		assertHttpError(401, () -> fred.login());
	}

	@Test
	public void deleteSuperAdminCredentials() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer().login();
		SpaceDog superdog = superdog();

		// forbidden to delete superadmin if last superadmin of backend
		superadmin.delete("/2/credentials/" + superadmin.id()).go(403).asVoid();
		superdog.delete("/2/credentials/" + superadmin.id()).go(403).asVoid();

		// superadmin test can create another superadmin (test1)
		SpaceDog superfred = createTempDog(superadmin, "superfred", Roles.superadmin);
		superfred.login();

		// superadmin test can delete superadmin superfred
		superadmin.credentials().delete(superfred.id());

		// superfred can no longer login
		assertHttpError(401, () -> superfred.login());

		// superdog can not be deleted
		assertHttpError(404, () -> superdog.credentials().delete("me"));
	}

	@Test
	public void superdogCanDoAnything() {

		// prepare
		prepareTest();
		clearServer();

		// superdog with root backend id
		SpaceDog superdog = superdog();

		// superdog can access anything in root backend
		superdog.credentials().getByUsername("fred");
		superdog.settings().get(CredentialsSettings.class);
	}

	@Test
	public void enableDisableAfter() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// fred logs in
		fred.login();

		// fred gets data
		fred.data().prepareGetAll().go();

		// only admins are allowed to update credentials enable disable after dates
		assertHttpError(403, () -> fred.credentials()//
				.enableDisableAfter(fred.id(), DateTime.now(), null));

		// only admins are allowed to update credentials enabled status
		assertHttpError(403, () -> fred.credentials().enable(fred.id()));

		// superadmin updates fred's credentials disable after date
		// before now so fred's credentials are disabled
		DateTime enableAfter = null;
		DateTime disableAfter = DateTime.now().minus(100000);
		superadmin.credentials().enableDisableAfter(fred.id(), enableAfter, disableAfter);

		// fred's credentials are disabled so he fails to gets any data
		SpaceException e = assertHttpError(401, () -> fred.data().prepareGetAll().go());
		assertEquals("disabled-credentials", e.code());

		// fred's credentials are disabled so he fails to log in
		e = assertHttpError(401, () -> fred.login());
		assertEquals("disabled-credentials", e.code());

		// superadmin update fred's credentials enable after date
		// before now and after disable after date so fred's credentials
		// are enabled again
		enableAfter = DateTime.now().minus(100000);
		superadmin.credentials().enableDisableAfter(fred.id(), enableAfter, disableAfter);

		// fred's credentials are enabled again so he gets data
		// with his old access token from first login
		SpaceRequest.get("/2/data").bearerAuth(fred).go(200).asVoid();

		// fred's credentials are enabled again so he can log in
		fred.login();

		// superadmin updates fred's credentials disable after date
		// before now but after enable after date so fred's credentials
		// are disabled again
		disableAfter = DateTime.now().minus(100000);
		superadmin.credentials().enableDisableAfter(fred.id(), enableAfter, disableAfter);

		// fred's credentials are disabled so he fails to gets any data
		e = assertHttpError(401, () -> fred.data().prepareGetAll().go());
		assertEquals("disabled-credentials", e.code());

		// fred's credentials are disabled so he fails to log in
		e = assertHttpError(401, () -> fred.login());
		assertEquals("disabled-credentials", e.code());

		// superadmin updates fred's credentials to remove enable and
		// disable after dates so fred's credentials are enabled again
		enableAfter = null;
		disableAfter = null;
		superadmin.credentials().enableDisableAfter(fred.id(), enableAfter, disableAfter);

		// fred's credentials are enabled again so he gets data
		// with his old access token from first login
		fred.get("/2/data").bearerAuth(fred).go(200).asVoid();

		// fred's credentials are enabled again so he can log in
		fred.login();

		// superadmin fails to update fred's credentials enable after date
		// since invalid format
		superadmin.post("/2/credentials/{id}/_enable_disable_after")//
				.routeParam("id", fred.id()).bodyJson(ENABLE_AFTER_FIELD, "XXX").go(400).asVoid();
	}

	@Test
	public void changePasswordInvalidatesAllTokens() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// fred logs in
		fred.login();

		// fred logs in again creating a second session
		SpaceDog fred2 = SpaceDog.dog().username(fred.username())//
				.login(fred.password().get());

		// fred can access data with his first token
		fred.data().prepareGetAll().go();

		// fred can access data with his second token
		fred2.data().prepareGetAll().go();

		// superadmin updates fred's password
		String newPassword = Passwords.random();
		superadmin.credentials().setPassword(fred.id(), //
				superadmin.password().get(), newPassword);

		// fred can no longer access data with his first token now invalid
		assertHttpError(401, () -> fred.data().prepareGetAll().go());

		// fred can no longer access data with his second token now invalid
		assertHttpError(401, () -> fred2.data().prepareGetAll().go());

		// but fred can log in with his new password
		fred.login(newPassword);
	}

	@Test
	public void multipleInvalidPasswordChallengesDisableCredentials() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();
		SpaceDog fred = createTempDog(superadmin, "fred");

		Credentials credentials = superadmin.credentials().get(fred.id());//
		assertEquals(0, credentials.invalidChallenges());
		assertNull(credentials.lastInvalidChallengeAt());

		// fred tries to log in with an invalid password
		SpaceRequest.get("/2/login")//
				.backend(guest.backend())//
				.basicAuth(fred.username(), "XXX")//
				.go(401).asVoid();

		// fred's invalid challenges count is still zero
		// since no maximum invalid challenges set in credentials settings
		credentials = superadmin.credentials().get(fred.id());//
		assertEquals(0, credentials.invalidChallenges());
		assertNull(credentials.lastInvalidChallengeAt());

		// superadmin sets maximum invalid challenges to 2
		CredentialsSettings settings = new CredentialsSettings();
		settings.maximumInvalidChallenges = 2;
		settings.resetInvalidChallengesAfterMinutes = 1;
		superadmin.credentials().settings(settings);

		// fred tries to log in with an invalid password
		SpaceRequest.get("/2/login")//
				.backend(guest.backend())//
				.basicAuth(fred.username(), "XXX")//
				.go(401).asVoid();

		// superadmin gets fred's credentials
		// fred has 1 invalid password challenge
		credentials = superadmin.credentials().get(fred.id());//
		assertEquals(1, credentials.invalidChallenges());
		assertNotNull(credentials.lastInvalidChallengeAt());

		// fred tries to log in with an invalid password
		SpaceRequest.get("/2/login")//
				.backend(guest.backend())//
				.basicAuth(fred.username(), "XXX")//
				.go(401).asVoid();

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
		fred.get("/2/login").go(401)//
				.assertEquals("disabled-credentials", "error.code");

		// superadmin enables fred's credentials
		superadmin.credentials().enable(fred.id());

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
		SpaceDog superadmin = clearServer();
		SpaceDog fred = createTempDog(superadmin, "fred").login();

		// fred can get data objects
		fred.data().prepareGetAll().go();

		// superadmin forces fred to change his password
		superadmin.credentials().passwordMustChange(fred.id());

		// fred can no longer get data objects with his token
		// because he must first change his password
		SpaceRequest.get("/2/data").bearerAuth(fred).go(403)//
				.assertEquals("password-must-change", "error.code");

		// fred can no longer get data objects with his password
		// because he must first change his password
		SpaceRequest.get("/2/data").basicAuth(fred).go(403)//
				.assertEquals("password-must-change", "error.code");

		// fred can change his password
		String newPassword = Passwords.random();
		fred.credentials().setMyPassword(fred.password().get(), newPassword);
		fred.password(newPassword);

		// fred fails to get data objects
		// since old access token is no more valid
		SpaceRequest.get("/2/data").bearerAuth(fred).go(401).asVoid();

		// but fred gets data with his new password
		SpaceRequest.get("/2/data").basicAuth(fred).go(200).asVoid();

		// fred logs in with his new password
		fred.login(newPassword);

		// fred can get data objects again
		SpaceRequest.get("/2/data").bearerAuth(fred).go(200).asVoid();
	}

	@Test
	public void forgotPassword() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// fred can get data objects
		fred.get("/2/data").go(200).asVoid();

		// to declare password is forgotten
		// you need to pass its username
		assertHttpError(400, () -> superadmin.credentials()//
				.sendPasswordResetEmail(""));

		// if invalid username, you get a 404
		assertHttpError(404, () -> superadmin.credentials()//
				.sendPasswordResetEmail("XXX"));

		// guest fails to declare that his password is forgotten
		// if no forgot password template set in mail settings
		assertHttpError(400, () -> guest.credentials()//
				.sendPasswordResetEmail("fred"));

		// superadmin saves the forgotPassword email template
		EmailTemplate template = new EmailTemplate();
		template.name = "password_reset_email_template";
		template.from = "no-reply@api.spacedog.io";
		template.to = Lists.newArrayList("{{credentials.email}}");
		template.subject = "You've forgotten your password!";
		template.text = "{{passwordResetCode}}";
		superadmin.emails().saveTemplate(template);

		// guest declares fred's forgot his password
		assertHttpError(401, () -> guest.credentials().sendPasswordResetEmail("fred"));

		// superadmin updates forgot password email template
		// to authorize all
		template.authorizedRoles = Sets.newHashSet(Roles.all);
		superadmin.emails().saveTemplate(template);

		// guest declares fred's forgot his password
		guest.credentials().sendPasswordResetEmail("fred");

		// guest can not pass any parameter unless they
		// are registered in the template model
		assertHttpError(400, () -> guest.credentials().sendPasswordResetEmail("fred", //
				Json.object("url", "http://localhost:8080")));

		// add an url parameter to the template model
		template.model = Maps.newHashMap();
		template.model.put("url", "string");
		template.text = "{{url}}?code={{passwordResetCode}}";
		superadmin.emails().saveTemplate(template);

		// guest declares fred's forgot his password
		// passing an url parameter
		guest.credentials().sendPasswordResetEmail("fred", //
				Json.object("url", "http://localhost:8080"));

		// fred can still access services if he remembers his password
		// or if he's got a valid token
		fred.data().prepareGetAll().go();
	}

	@Test
	public void updateUsername() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog fred = createTempDog(superadmin, "fred");
		SpaceDog nath = createTempDog(superadmin, "nath");

		// fred fails to set his username to 'nath'
		SpaceException exception = assertHttpError(400, () -> fred.credentials()//
				.updateMyUsername(nath.username(), fred.password().get()));

		assertEquals("already-exists", exception.code());

		// fred logs in to get an access token
		fred.login();

		// fred fails to updates his username since password must be challenged
		SpaceRequest.put("/2/credentials/me/username")//
				.bearerAuth(fred)//
				.bodyPojo("fred2")//
				.go(403)//
				.assertEquals("unchallenged-password", "error.code");

		// fred updates his username to 'fred2'
		fred.credentials().updateMyUsername("fred2", fred.password().get());

		// fred old username is no more valid
		assertHttpError(401, () -> fred.login());

		// fred new username is valid
		fred.username("fred2");
		fred.login();
		assertEquals("fred2", fred.credentials().me().username());
	}

	@Test
	public void updateEmail() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// fred updates his email to 'fred@dog.ch'
		fred.credentials().updateMyEmail("fred@dog.ch", fred.password().get());

		// fred checks his new email address
		assertEquals("fred@dog.ch", fred.credentials().me(true).email().get());
	}

	@Test
	public void credentialsCreatedWitOrWithoutRoleShouldBeUsers() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog fred = createTempDog(superadmin, "fred");
		SpaceDog nath = createTempDog(superadmin, "nath", "superwoman");
		SpaceDog vince = createTempDog(superadmin, "vince", "admin");

		assertTrue(fred.credentials().me().isUser());
		assertTrue(nath.credentials().me().isUser());
		assertFalse(vince.credentials().me().isUser());
		assertTrue(vince.credentials().me().isAdmin());
	}

	@Test
	public void superdogAndGuestAreReservedUsernamePrefixes() {
		Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		assertHttpError(400, () -> superadmin.credentials()//
				.create("superdog", "hi toto", "toto@dog.com"));

		assertHttpError(400, () -> superadmin.credentials()//
				.create("guest", "hi toto", "toto@dog.com"));
	}

}
