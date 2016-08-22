/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.regex.Pattern;

import com.google.common.base.Strings;

public class Usernames {

	private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-zA-Z0-9@\\.]{3,}");

	public static boolean isValid(String username) {
		if (Strings.isNullOrEmpty(username))
			return false;
		return USERNAME_PATTERN.matcher(username).matches();
	}

	public static void checkIfValid(String username) {
		if (!isValid(username))
			throw Exceptions.illegalArgument("invalid username: does not comply %s regex", //
					USERNAME_PATTERN.pattern());
	}
}
