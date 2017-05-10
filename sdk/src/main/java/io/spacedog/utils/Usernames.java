/**
 * © David Attias 2015
 */
package io.spacedog.utils;

public class Usernames {

	public static final String USERNAME_DEFAULT_REGEX = "[a-zA-Z0-9_%@+\\-\\.]{3,}";

	public static void checkValid(String username) {
		checkValid(username, USERNAME_DEFAULT_REGEX);
	}

	public static void checkValid(String username, String regex) {
		Check.matchRegex(regex, username, "username");
	}

}
