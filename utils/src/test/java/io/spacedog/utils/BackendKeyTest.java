package io.spacedog.utils;

import org.junit.Assert;
import org.junit.Test;

public class BackendKeyTest extends Assert {

	@Test
	public void checkBackendIdValidity() {

		// valid backend ids
		assertTrue(BackendKey.isIdValid("a1234"));
		assertTrue(BackendKey.isIdValid("abcd"));
		assertTrue(BackendKey.isIdValid("1a2b34d"));

		// invalid backend ids
		assertFalse(BackendKey.isIdValid("a"));
		assertFalse(BackendKey.isIdValid("bb"));
		assertFalse(BackendKey.isIdValid("ccc"));
		assertFalse(BackendKey.isIdValid("dd-dd"));
		assertFalse(BackendKey.isIdValid("/eeee"));
		assertFalse(BackendKey.isIdValid("eeee:"));
		assertFalse(BackendKey.isIdValid("abcdE"));
		assertFalse(BackendKey.isIdValid("spacedog"));
		assertFalse(BackendKey.isIdValid("spacedog123"));
		assertFalse(BackendKey.isIdValid("123spacedog"));
		assertFalse(BackendKey.isIdValid("123spacedog123"));
	}
}
