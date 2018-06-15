package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.services.PebbleTemplating;

public class PebbleTemplatingTest extends Assert {

	@Test
	public void checkValueSimpleAndValid() {
		assertTrue(PebbleTemplating.checkValueSimpleAndValid("param", "a", "string"));
		assertTrue(PebbleTemplating.checkValueSimpleAndValid("param", 1, "integer"));
		assertTrue(PebbleTemplating.checkValueSimpleAndValid("param", 2l, "long"));
		assertTrue(PebbleTemplating.checkValueSimpleAndValid("param", 3f, "float"));
		assertTrue(PebbleTemplating.checkValueSimpleAndValid("param", 4d, "double"));
		assertTrue(PebbleTemplating.checkValueSimpleAndValid("param", true, "boolean"));
		assertTrue(PebbleTemplating.checkValueSimpleAndValid("param", Lists.newArrayList(), "array"));
		assertTrue(PebbleTemplating.checkValueSimpleAndValid("param", Maps.newHashMap(), "object"));

		try {
			assertFalse(PebbleTemplating.checkValueSimpleAndValid("param", 1, "string"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(PebbleTemplating.checkValueSimpleAndValid("param", 1d, "integer"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(PebbleTemplating.checkValueSimpleAndValid("param", 1, "long"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(PebbleTemplating.checkValueSimpleAndValid("param", 1, "float"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(PebbleTemplating.checkValueSimpleAndValid("param", 1, "double"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(PebbleTemplating.checkValueSimpleAndValid("param", 1, "boolean"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(PebbleTemplating.checkValueSimpleAndValid("param", Maps.newHashMap(), "array"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(PebbleTemplating.checkValueSimpleAndValid("param", Lists.newArrayList(), "object"));
			fail();
		} catch (Exception e) {
		}

	}
}
