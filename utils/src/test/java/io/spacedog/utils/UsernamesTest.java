/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import org.junit.Assert;
import org.junit.Test;

public class UsernamesTest extends Assert {

	@Test
	public void theseUsernamesAreValid() {
		Usernames.checkIfValid("123");
		Usernames.checkIfValid("1234");
		Usernames.checkIfValid("abcde");
		Usernames.checkIfValid("1aBcDe");
		Usernames.checkIfValid("aBcDe3");
	}

	@Test
	public void theseUsernamesAreNotValid() {
		assertFalse(Usernames.isValid(null));
		assertFalse(Usernames.isValid(""));
		assertFalse(Usernames.isValid("a"));
		assertFalse(Usernames.isValid("ab"));
		assertFalse(Usernames.isValid("la copine"));
		assertFalse(Usernames.isValid("la,copine"));
		assertFalse(Usernames.isValid("la/copine"));
	}
}
