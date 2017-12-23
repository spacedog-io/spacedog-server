/**
 * © David Attias 2015
 */
package io.spacedog.model;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.model.Passwords;

public class PasswordsTest extends Assert {

	@Test
	public void thesePasswordsAreValid() {
		Passwords.check("123456");
		Passwords.check("abcdefg");
		Passwords.check("_-)!ç!!è§(");
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
			Passwords.check(password);
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
