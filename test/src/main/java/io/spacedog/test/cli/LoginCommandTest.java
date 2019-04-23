package io.spacedog.test.cli;

import java.io.IOException;

import org.junit.Test;

import io.spacedog.cli.LoginCommand;
import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.http.SpaceEnv;
import io.spacedog.client.http.SpaceException;
import io.spacedog.test.SpaceTest;

public class LoginCommandTest extends SpaceTest {

	@Test
	public void testLoginCommand() throws IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog fred = createTempDog(superadmin, "fred", Roles.superadmin);

		// login without any argument fails
		assertThrow(SpaceException.class, //
				() -> new LoginCommand().verbose(true).login());

		// login without backend fails
		assertThrow(SpaceException.class, //
				() -> new LoginCommand().verbose(true)//
						.username(superadmin.username()).login());

		// login without username fails
		assertThrow(SpaceException.class, //
				() -> new LoginCommand().verbose(true)//
						.backend(superadmin.backend()).login());

		// login with invalid username fails
		assertHttpError(401, () -> new LoginCommand().verbose(true)//
				.backend(superadmin.backend())//
				.username("XXX")//
				.password(superadmin.password().get())//
				.login());

		// login with invalid backend fails
		assertHttpError(404, () -> new LoginCommand().verbose(true)//
				.backend(SpaceEnv.env().apiBackend().fromBackendId("XXXX"))//
				.username(superadmin.username())//
				.password(superadmin.password().get())//
				.login());

		// superadmin fails to login with fred's password
		assertHttpError(401, () -> new LoginCommand().verbose(true)//
				.backend(superadmin.backend())//
				.username(superadmin.username())//
				.password(fred.password().get())//
				.login());

		// fred fails to login with superadmin's password
		assertHttpError(401, () -> new LoginCommand().verbose(true)//
				.backend(fred.backend())//
				.username(fred.username())//
				.password(superadmin.password().get())//
				.login());

		// superadmin succeeds to login
		SpaceDog session = new LoginCommand().verbose(true)//
				.backend(superadmin.backend())//
				.username(superadmin.username())//
				.password(superadmin.password().get())//
				.login();

		assertEquals(superadmin.backend(), session.backend());
		assertEquals(superadmin.username(), session.username());

		SpaceDog fromFile = LoginCommand.session();

		assertEquals(superadmin.backend(), fromFile.backend());
		assertEquals(session.accessToken(), fromFile.accessToken());

		// login to test backend with full backend url succeeds
		session = new LoginCommand().verbose(true)//
				.backend("http://api.lvh.me:8443")//
				.username(fred.username())//
				.password(fred.password().get())//
				.login();

		assertEquals("http://api.lvh.me:8443", session.backend().url());
		assertEquals(fred.username(), session.username());

		fromFile = LoginCommand.session();

		assertEquals("http://api.lvh.me:8443", fromFile.backend().url());
		assertEquals(session.accessToken(), fromFile.accessToken());
	}

	@Test
	public void loginCommandShouldLogoutFromAnyPreviousSession() throws IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		// superadmin logs in
		SpaceDog session1 = new LoginCommand().verbose(true)//
				.backend(superadmin.backend())//
				.username(superadmin.username())//
				.password(superadmin.password().get())//
				.login();

		assertTrue(session1.isTokenStillValid());

		// superadmin logs in again
		SpaceDog session2 = new LoginCommand().verbose(true)//
				.backend(superadmin.backend())//
				.username(superadmin.username())//
				.password(superadmin.password().get())//
				.login();

		assertFalse(session1.isTokenStillValid());
		assertTrue(session2.isTokenStillValid());

		// invalid attempt to login
		assertHttpError(401, () -> new LoginCommand().verbose(true)//
				.backend(superadmin.backend())//
				.username("XXX")//
				.password("XXX")//
				.login());

		assertFalse(session1.isTokenStillValid());
		assertFalse(session2.isTokenStillValid());
	}

	@Test
	public void loginSessionIsStoredAndCanBeCleared() throws IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		// superadmin logs in
		SpaceDog session = new LoginCommand().verbose(true)//
				.backend(superadmin.backend())//
				.username(superadmin.username())//
				.password(superadmin.password().get())//
				.login();

		// check session retrieved from file is correct
		SpaceDog fromFile = LoginCommand.session();
		assertEquals(session.backend(), fromFile.backend());
		assertEquals(session.accessToken(), fromFile.accessToken());

		// if session file is cleared, check session is no more
		LoginCommand.clearSession();
		RuntimeException exception = assertThrow(RuntimeException.class, () -> LoginCommand.session());
		assertEquals(exception.getMessage(), "you must login first");
	}
}
