/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

public class UserUtilsTest extends Assert {

	@Test
	public void shouldValidatePasswords() {
		checkHashIsStable("1234567890azerty");
		checkHashIsStable("@#°)!§'`£%*$*/:;.");
		checkHashIsStable("i love you");
		checkHashAreDifferent("123 ", "123");
		checkHashAreDifferent(" 123", "123");
		checkHashAreDifferent(" 123 ", "123");
	}

	private void checkHashIsStable(String password) {
		assertEquals(UserUtils.hashPassword(password), UserUtils.hashPassword(password));
	}

	private void checkHashAreDifferent(String password, String other) {
		assertNotEquals(UserUtils.hashPassword(password), UserUtils.hashPassword(other));
	}

}
