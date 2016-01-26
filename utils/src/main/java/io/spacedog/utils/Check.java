package io.spacedog.utils;

import java.util.Collection;

import com.google.common.base.Strings;

public class Check {

	public static void notNullOrEmpty(String value, String paramName) {
		if (Strings.isNullOrEmpty(value))
			throw new IllegalArgumentException(String.format("parameter [%s] must not be null or empty", paramName));
	}

	public static void notNullOrEmpty(Collection<?> value, String paramName) {
		if (Utils.isNullOrEmpty(value))
			throw new IllegalArgumentException(String.format("parameter [%s] must not be empty", paramName));
	}

	public static void isTrue(boolean condition, String message, Object... arguments) {
		if (!condition)
			throw new AssertionError(String.format(message, arguments));
	}

}
