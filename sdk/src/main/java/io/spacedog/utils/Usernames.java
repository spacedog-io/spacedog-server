/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Optional;

public class Usernames {

	public static final String USERNAME_DEFAULT_REGEX = "[a-zA-Z0-9_%@+\\-\\.]{3,}";

	public static void checkValid(String username) {
		checkValid(username, Optional.empty());
	}

	public static void checkValid(String username, Optional<String> regex) {
		Check.matchRegex(regex.orElse(USERNAME_DEFAULT_REGEX), username, "username");
	}

}
