package io.spacedog.utils;

import java.util.Collection;

import org.apache.http.HttpStatus;

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
		isTrue(condition, HttpStatus.SC_BAD_REQUEST, message, arguments);
	}

	public static void isTrue(boolean condition, int httpStatus, String message, Object... arguments) {
		if (!condition)
			throw Exceptions.space(httpStatus, message, arguments);
	}
}
