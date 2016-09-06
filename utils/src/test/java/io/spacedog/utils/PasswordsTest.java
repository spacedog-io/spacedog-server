/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import org.junit.Assert;
import org.junit.Test;

public class PasswordsTest extends Assert {

	@Test
	public void thesePasswordsAreValid() {
		Passwords.checkValid("123456");
		Passwords.checkValid("abcdefg");
		Passwords.checkValid("_-)!ç!!è§(");
	}

	@Test
	public void thesePasswordsAreNotValid() {
		checkNotValid(null);
		checkNotValid("");
		checkNotValid("1");
		checkNotValid("12345");
	}

	private void checkNotValid(String password) {
		try {
			Passwords.checkValid(password);
			fail(String.format("password [%s] shouldn't be valid", password));
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void byHashingPasswordMultipleTimesYouGetTheSameHash() {
		checkHashIsStable("1234567890azerty");
		checkHashIsStable("@#°)!§'`£%*$*/:;.");
		checkHashIsStable("i love you");
	}

	private void checkHashIsStable(String password) {
		assertEquals(Passwords.checkAndHash(password), Passwords.checkAndHash(password));
	}

	@Test
	public void byHashingDifferentPasswordsYouGetDifferentHashes() {
		checkHashAreDifferent("123456 ", "123456");
		checkHashAreDifferent(" 123456", "123456");
		checkHashAreDifferent(" 123456 ", "123456");
	}

	private void checkHashAreDifferent(String password, String other) {
		assertNotEquals(Passwords.checkAndHash(password), Passwords.checkAndHash(other));
	}
}
