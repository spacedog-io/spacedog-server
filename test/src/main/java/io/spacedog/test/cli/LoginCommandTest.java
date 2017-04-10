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

		try {
			LoginCommand.get().login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		try {
			LoginCommand.get().backend(superadmin.backendId()).login();
			fail();

		} catch (IllegalArgumentException ignore) {
		}

		SpaceDog session = LoginCommand.get().backend(superadmin.backendId())//
				.username(superadmin.username()).login();

		assertEquals("test", session.backendId());
		assertEquals("test", session.username());

		SpaceDog session2 = LoginCommand.get().clearCache().session();

		assertEquals(session.backendId(), session2.backendId());
		assertEquals(session.accessToken(), session2.accessToken());
	}
}
