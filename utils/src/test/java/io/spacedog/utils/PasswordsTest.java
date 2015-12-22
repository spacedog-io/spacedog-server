/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import org.junit.Assert;
import org.junit.Test;

public class PasswordsTest extends Assert {

	@Test
	public void thesePasswordsAreValid() {
		Passwords.checkIfValid("123456");
		Passwords.checkIfValid("abcdefg");
	}

	@Test
	public void thesePasswordsAreNotValid() {
		assertFalse(Passwords.isValid(null));
		assertFalse(Passwords.isValid(""));
		assertFalse(Passwords.isValid("1"));
		assertFalse(Passwords.isValid("12345"));
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
