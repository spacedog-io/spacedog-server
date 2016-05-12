package io.spacedog.utils;

import java.util.Collection;

import com.google.common.base.Strings;

public class Check {

	public static void notNull(Object value, String paramName) {
		if (value == null)
			throw Exceptions.illegalArgument("parameter [%s] must not be null", paramName);
	}

	public static void notNullOrEmpty(String value, String paramName) {
		if (Strings.isNullOrEmpty(value))
			throw Exceptions.illegalArgument("parameter [%s] must not be null or empty", paramName);
	}

	public static void notNullOrEmpty(Collection<?> value, String paramName) {
		if (Utils.isNullOrEmpty(value))
			throw Exceptions.illegalArgument("parameter [%s] must not be null or empty", paramName);
	}

	public static void isTrue(boolean condition, String message, Object... arguments) {
		if (!condition)
			throw new AssertionError(String.format(message, arguments));
	}
}
