package io.spacedog.test;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

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

	@Test
	public void shouldAssertSourceAlmostEqual() {
		ObjectNode node1 = Json.object("text", "hi", "size", 5, "other", Json.array(1, 2));
		ObjectNode node2 = Json.object("text", "hi", "size", 5, "other", Json.array(1, 3));
		assertNotEquals(node1, node2);
		assertThrow(AssertionError.class, () -> assertAlmostEquals(node1, node2));
		assertAlmostEquals(node1, node2, "other");
	}

}
