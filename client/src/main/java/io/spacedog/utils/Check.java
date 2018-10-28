package io.spacedog.utils;

import java.util.Collection;
import java.util.regex.Pattern;

import io.spacedog.client.http.SpaceStatus;

public class Check {

	public static <T> T notNull(T value, String valueName) {
		if (value == null)
			throw Exceptions.illegalArgument("[%s] is null", valueName);
		return value;
	}

	public static void isNull(Object value, String valueName) {
		if (value != null)
			throw Exceptions.illegalArgument("[%s] is not null", valueName);
	}

	public static String notNullOrEmpty(String value, String valueName) {
		if (Utils.isNullOrEmpty(value))
			throw Exceptions.illegalArgument("[%s] is null or empty", valueName);
		return value;
	}

	public static Collection<?> notNullOrEmpty(Collection<?> value, String valueName) {
		if (Utils.isNullOrEmpty(value))
			throw Exceptions.illegalArgument("[%s] is null or empty", valueName);
		return value;
	}

	public static <T> T[] notNullOrEmpty(T[] value, String valueName) {
		if (Utils.isNullOrEmpty(value))
			throw Exceptions.illegalArgument("[%s] is null or empty", valueName);
		return value;
	}

	public static void isTrue(boolean condition, String message, Object... arguments) {
		isTrue(condition, SpaceStatus.BAD_REQUEST, message, arguments);
	}

	public static void isTrue(boolean condition, int httpStatus, String message, Object... arguments) {
		if (!condition)
			throw Exceptions.space(httpStatus, message, arguments);
	}

	public static void matchRegex(String regex, String value, String valueName) {
		notNull(value, valueName);
		if (!Pattern.matches(regex, value))
			throw Exceptions.illegalArgument(//
					"[%s][%s] is invalid: doesn't comply [%s] regex", valueName, value, regex);
	}
}
