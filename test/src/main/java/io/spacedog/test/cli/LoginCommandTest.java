package io.spacedog.test.cli;

import java.io.IOException;

import org.junit.Test;

import io.spacedog.cli.LoginCommand;
import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequestException;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Roles;

public class LoginCommandTest extends SpaceTest {

	@Test
	public void test() throws IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog fred = createTempDog(superadmin, "fred", Roles.superadmin);

		// login without fails
		try {
			new LoginCommand().verbose(true).login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		// login without backend fails
		try {
			new LoginCommand().verbose(true).username("test").login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		// login without username fails
		try {
			new LoginCommand().verbose(true)//
					.backend(superadmin.backendId())//
					.login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		// login with invalid username fails
		try {
			new LoginCommand().verbose(true)//
					.backend(superadmin.backendId())//
					.username("XXX")//
					.password(superadmin.password().get())//
					.login();
			fail();

		} catch (SpaceRequestException e) {
			if (e.httpStatus() != 401)
				fail();
		}

		// login with invalid backend fails
		try {
			new LoginCommand().verbose(true)//
					.backend("XXXX")//
					.username(superadmin.username())//
					.password(superadmin.password().get())//
					.login();
			fail();

		} catch (SpaceRequestException e) {
			if (e.httpStatus() != 401)
				fail();
		}

		// superadmin fails to login with fred's password
		try {
			new LoginCommand().verbose(true)//
					.backend(superadmin.backendId())//
					.username(superadmin.username())//
					.password(fred.password().get())//
					.login();
			fail();

		} catch (SpaceRequestException e) {
			if (e.httpStatus() != 401)
				fail();
		}

		// fred fails to login with superadmin's password
		try {
			new LoginCommand().verbose(true)//
					.backend(fred.backendId())//
					.username(fred.username())//
					.password(superadmin.password().get())//
					.login();
			fail();

		} catch (SpaceRequestException e) {
			if (e.httpStatus() != 401)
				fail();
		}

		// superadmin succeeds to login
		SpaceDog session = new LoginCommand().verbose(true)//
				.backend(superadmin.backendId())//
				.username(superadmin.username())//
				.password(superadmin.password().get())//
				.login();

		assertEquals("test", session.backendId());
		assertEquals("test", session.username());

		// clears cache to force login command to load session from file
		LoginCommand.clearCache();
		SpaceDog fromFile = LoginCommand.session();

		assertEquals("test", fromFile.backendId());
		assertEquals(session.accessToken(), fromFile.accessToken());

		// login to test backend with full backend url succeeds
		session = new LoginCommand().verbose(true)//
				.backend("http://test.lvh.me:8443")//
				.username(fred.username())//
				.password(fred.password().get())//
				.login();

		assertEquals("http://test.lvh.me:8443", session.backendId());
		assertEquals("fred", session.username());

		// clears cache to force login command to load session from file
		LoginCommand.clearCache();
		fromFile = LoginCommand.session();

		assertEquals("http://test.lvh.me:8443", fromFile.backendId());
		assertEquals(session.accessToken(), fromFile.accessToken());

	}
}
