package io.spacedog.test;

import org.junit.Test;

import io.spacedog.utils.Exceptions;

public class SpaceTestTest extends SpaceTest {

	@Test
	public void shouldPass() {
		assertThrow(IllegalArgumentException.class, () -> {
			throw Exceptions.illegalArgument("error");
		});
	}

	@Test
	public void shouldFailSinceNoException() {
		try {
			assertThrow(RuntimeException.class, () -> System.currentTimeMillis());
			fail();

		} catch (AssertionError e) {
		}
	}

	@Test
	public void shouldFailSinceUnexpectedException() {
		try {
			assertThrow(IllegalArgumentException.class, () -> fail());
			fail();

		} catch (AssertionError e) {
		}
	}

}
