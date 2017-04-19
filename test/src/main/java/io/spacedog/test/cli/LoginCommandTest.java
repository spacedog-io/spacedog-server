package io.spacedog.test.cli;

import java.io.IOException;

import org.junit.Test;

import io.spacedog.cli.LoginCommand;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Credentials.Level;

public class LoginCommandTest extends SpaceTest {

	@Test
	public void test() throws IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		superadmin.credentials().create("fred", //
				"hi fred", "platform@spacedog.io", Level.SUPER_ADMIN);
		LoginCommand command = LoginCommand.get().verbose(true);

		// login without fails
		try {
			command.login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		// login without backend fails
		try {
			command.username("test").login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		// login without username fails
		try {
			command.backend("test").login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		// login with invalid username fails
		try {
			command.backend("test").username("XXX").password("hi test").login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		// login with invalid backend fails
		try {
			command.backend("XXXX").username("test").password("hi test").login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		// login with invalid password fails
		try {
			command.backend("test").username("test").password("hi fred").login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		// login with invalid password fails
		try {
			command.backend("test").username("fred").password("hi test").login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		// login to test backend with backend id only succeeds
		SpaceDog session = command.backend("test")//
				.username("test").password("hi test").login();

		assertEquals("test", session.backendId());
		assertEquals("test", session.username());

		// clears cache to force login command to load session from file
		SpaceDog fromFile = command.clearCache().session();

		assertEquals("test", fromFile.backendId());
		assertEquals(session.accessToken(), fromFile.accessToken());

		// login to test backend with full backend url succeeds
		session = command.backend("http://test.lvh.me:8443")//
				.username("fred").password("hi fred").login();

		assertEquals("test", session.backendId());
		assertEquals("fred", session.username());

		// clears cache to force login command to load session from file
		fromFile = command.clearCache().session();

		assertEquals("http://test.lvh.me:8443", fromFile.backendId());
		assertEquals(session.accessToken(), fromFile.accessToken());

	}
}
