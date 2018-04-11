package io.spacedog.test.cli;

import java.io.IOException;

import org.junit.Test;

import io.spacedog.cli.LoginCommand;
import io.spacedog.rest.SpaceRequestException;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class LoginCommandTest extends SpaceTest {

	@Test
	public void test() throws IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog fred = createTempUser(superadmin, "fred");

		// login with nothing fails
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
			new LoginCommand().verbose(true).backend("test").login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		// login with invalid username fails
		try {
			new LoginCommand().verbose(true).backend("test").username("XXX")//
					.password(superadmin.password().get()).login();
			fail();

		} catch (SpaceRequestException e) {
			if (e.httpStatus() != 401)
				fail();
		}

		// login with invalid backend fails
		try {
			new LoginCommand().verbose(true).backend("XXXX").username("test")//
					.password(superadmin.password().get()).login();
			fail();

		} catch (SpaceRequestException e) {
			if (e.httpStatus() != 401)
				fail();
		}

		// superadmin fails to login with fred's password
		try {
			new LoginCommand().verbose(true).backend("test").username("test")//
					.password(fred.password().get()).login();
			fail();

		} catch (SpaceRequestException e) {
			if (e.httpStatus() != 401)
				fail();
		}

		// fred fails to login with superadmin's password
		try {
			new LoginCommand().verbose(true).backend("test").username("fred")//
					.password(superadmin.password().get()).login();
			fail();

		} catch (SpaceRequestException e) {
			if (e.httpStatus() != 401)
				fail();
		}

		// login to test backend with backend id only succeeds
		SpaceDog session = new LoginCommand().verbose(true).backend("test")//
				.username("test").password(superadmin.password().get()).login();

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
				.username("fred")//
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
