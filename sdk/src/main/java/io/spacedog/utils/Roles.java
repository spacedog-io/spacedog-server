/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.regex.Pattern;

import com.google.common.base.Strings;

public class Roles {

	private static final Pattern ROLE_PATTERN = Pattern.compile("[a-z]{3,}");

	public static boolean isValid(String role) {
		if (Strings.isNullOrEmpty(role))
			return false;
		return ROLE_PATTERN.matcher(role).matches();
	}

	public static void checkIfValid(String role) {
		if (!isValid(role))
			throw Exceptions.illegalArgument("roles must be at least 3 characters long and "//
					+ "composed of a-z characters");
	}
}
