/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.regex.Pattern;

import com.google.common.base.Strings;

public class Backends {

	private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9]{4,}");

	public static boolean isRootApi(String backendId) {
		return rootApi().equals(backendId);
	}

	public static String rootApi() {
		return "api";
	}

	public static boolean isIdValid(String backendId) {

		if (!ID_PATTERN.matcher(backendId).matches())
			return false;

		if (backendId.indexOf("spacedog") > -1)
			return false;

		if (backendId.startsWith(rootApi()))
			return false;

		return true;
	}

	public static void checkIfIdIsValid(String backendId) {

		if (Strings.isNullOrEmpty(backendId))
			throw new IllegalArgumentException("backend id must not be null or empty");

		if (!isIdValid(backendId))
			throw new IllegalArgumentException("backend id must comply with these rules: "//
					+ "is at least 4 characters long, "//
					+ "is only composed of a-z and 0-9 characters, "//
					+ "is lowercase,  "//
					+ "does not start with 'api',  "//
					+ "does not contain 'spacedog'");
	}
}