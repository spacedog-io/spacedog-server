/**
 * © David Attias 2015
 */
package io.spacedog.client.credentials;

import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;

public class Usernames {

	public static final String USERNAME_DEFAULT_REGEX = "[a-zA-Z0-9_%@+\\-\\.]{3,}";

	public static void checkValid(String username) {
		checkValid(username, USERNAME_DEFAULT_REGEX);
	}

	public static void checkValid(String username, String regex) {
		Check.matchRegex(regex, username, "username");

		if (username.startsWith(Roles.superdog))
			throw Exceptions.illegalArgument(//
					"[%s] is a reserved username prefix", Roles.superdog);

		if (username.startsWith(Roles.guest))
			throw Exceptions.illegalArgument(//
					"[%s] is a reserved username prefix", Roles.guest);
	}

}
