package io.spacedog.utils;

import org.junit.Assert;
import org.junit.Test;

public class BackendsTest extends Assert {

	@Test
	public void checkBackendIdValidity() {

		// valid backend ids
		assertTrue(Backends.isIdValid("a1234"));
		assertTrue(Backends.isIdValid("abcd"));
		assertTrue(Backends.isIdValid("1a2b34d"));

		// invalid backend ids
		assertFalse(Backends.isIdValid("a"));
		assertFalse(Backends.isIdValid("bb"));
		assertFalse(Backends.isIdValid("ccc"));
		assertFalse(Backends.isIdValid("dd-dd"));
		assertFalse(Backends.isIdValid("/eeee"));
		assertFalse(Backends.isIdValid("eeee:"));
		assertFalse(Backends.isIdValid("abcdE"));
		assertFalse(Backends.isIdValid("spacedog"));
		assertFalse(Backends.isIdValid("spacedog123"));
		assertFalse(Backends.isIdValid("123spacedog"));
		assertFalse(Backends.isIdValid("123spacedog123"));
	}
}
