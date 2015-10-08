package io.spacedog.services;

import org.junit.Test;

public class UserTest extends AbstractTest {

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
		assertEquals(User.hashPassword(password), User.hashPassword(password));
	}

	private void checkHashAreDifferent(String password, String other) {
		assertNotEquals(User.hashPassword(password), User.hashPassword(other));
	}

}
