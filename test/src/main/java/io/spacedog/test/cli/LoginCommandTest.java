package io.spacedog.test.cli;

import java.io.IOException;

import org.junit.Test;

import io.spacedog.cli.LoginCommand;
import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.http.SpaceEnv;
import io.spacedog.test.SpaceTest;

public class LoginCommandTest extends SpaceTest {

	@Test
	public void test() throws IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog fred = createTempDog(superadmin, "fred", Roles.superadmin);

		// login without any argument fails
		assertThrow(IllegalArgumentException.class, //
				() -> new LoginCommand().verbose(true).login());

		// login without backend fails
		assertThrow(IllegalArgumentException.class, //
				() -> new LoginCommand().verbose(true)//
						.username(superadmin.username()).login());

		// login without username fails
		assertThrow(IllegalArgumentException.class, //
				() -> new LoginCommand().verbose(true)//
						.backend(superadmin.backend()).login());

		// login with invalid username fails
		assertHttpError(401, () -> new LoginCommand().verbose(true)//
				.backend(superadmin.backend())//
				.username("XXX")//
				.password(superadmin.password().get())//
				.login());

		// login with invalid backend fails
		assertHttpError(401, () -> new LoginCommand().verbose(true)//
				.backend(SpaceEnv.env().apiBackend().instanciate("XXXX"))//
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

		// clears cache to force login command to load session from file
		LoginCommand.clearCache();
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

		// clears cache to force login command to load session from file
		LoginCommand.clearCache();
		fromFile = LoginCommand.session();

		assertEquals("http://api.lvh.me:8443", fromFile.backend().url());
		assertEquals(session.accessToken(), fromFile.accessToken());
	}
}
