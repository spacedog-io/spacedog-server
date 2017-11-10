package io.spacedog.test;

import org.junit.Test;

import io.spacedog.client.SpaceDog;

public class SpaceTestTest extends SpaceTest {

	@Test
	public void shouldCheckRequestFails() {
		assertHttpError(403, //
				() -> SpaceDog.backendId("test").data().deleteAll("toto"));
	}

	@Test
	public void shouldFailSinceNoException() {
		try {
			assertRequestException(() -> System.currentTimeMillis());
			fail();

		} catch (AssertionError e) {
		}
	}

	@Test
	public void shouldFailSinceUnexpectedException() {
		// try {
		// assertRequestException(() -> fail());
		// fail();
		//
		// } catch (AssertionError e) {
		// }
	}

}
