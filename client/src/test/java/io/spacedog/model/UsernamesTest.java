/**
 * © David Attias 2015
 */
package io.spacedog.model;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.credentials.Usernames;

public class UsernamesTest extends Assert {

	@Test
	public void theseUsernamesAreValid() {
		Usernames.checkValid("123");
		Usernames.checkValid("___");
		Usernames.checkValid("---");
		Usernames.checkValid("%%%");
		Usernames.checkValid("@@@");
		Usernames.checkValid("+++");
		Usernames.checkValid("1234");
		Usernames.checkValid("abcde");
		Usernames.checkValid("1aBcDe");
		Usernames.checkValid("aBcDe3");
		Usernames.checkValid("_kj-ljn@lkj%lkj+kkj");
		Usernames.checkValid("vince.mira@toto.fr");
		Usernames.checkValid("vince-mira@toto.fr");
	}

	@Test
	public void theseUsernamesAreNotValid() {
		checkNotValid(null);
		checkNotValid("");
		checkNotValid("a");
		checkNotValid("ab");
		checkNotValid("la copine");
		checkNotValid("la,copine");
		checkNotValid("la/copine");
	}

	private void checkNotValid(String username) {
		try {
			Usernames.checkValid(username);
			fail(String.format("username [%s] shouldn't be valid", username));
		} catch (IllegalArgumentException e) {
		}
	}
}
