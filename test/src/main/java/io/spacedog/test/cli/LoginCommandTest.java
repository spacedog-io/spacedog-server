package io.spacedog.test.cli;

import java.io.IOException;

import org.junit.Test;

import io.spacedog.cli.LoginCommand;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class LoginCommandTest extends SpaceTest {

	@Test
	public void test() throws IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		LoginCommand command = LoginCommand.get().verbose(true);

		try {
			command.login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		try {
			command.backend(superadmin.backendId()).login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		SpaceDog session = command.backend(superadmin.backendId())//
				.username(superadmin.username()).login();

		assertEquals("test", session.backendId());
		assertEquals("test", session.username());

		SpaceDog session2 = command.clearCache().session();

		assertEquals(session.backendId(), session2.backendId());
		assertEquals(session.accessToken(), session2.accessToken());
	}
}
