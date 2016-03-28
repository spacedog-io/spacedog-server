/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.UUID;
import java.util.regex.Pattern;

import org.joda.time.DateTime;

import com.google.common.base.Strings;

public class BackendKey {

	public static final String ROOT_API = "api";

	public static final String DEFAULT_BACKEND_KEY_NAME = "default";

	public String name;
	public String secret;
	public DateTime generatedAt;

	public BackendKey() {
		this(DEFAULT_BACKEND_KEY_NAME);
	}

	public BackendKey(String name) {
		this.name = name;
		this.secret = UUID.randomUUID().toString();
		this.generatedAt = DateTime.now();
	}

	//
	// backend id utils
	//

	private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9]{4,}");

	public static boolean isIdValid(String backendId) {

		if (!ID_PATTERN.matcher(backendId).matches())
			return false;

		if (backendId.indexOf("spacedog") > -1)
			return false;

		if (backendId.startsWith(ROOT_API))
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

	public static String extractBackendId(String backendKey) {
		int indexOf = backendKey.indexOf(':');
		// if invalid backendKey, do not extract and return the whole key
		return indexOf > 0 ? backendKey.substring(0, indexOf) : backendKey;
	}

}