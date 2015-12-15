package io.spacedog.services;

import java.util.Collection;

import com.google.common.base.Strings;

public class Check {

	public static void notNullOrEmpty(String value, String paramName) {
		if (Strings.isNullOrEmpty(value))
			throw new IllegalArgumentException(String.format("parameter [%s] is null or empty", paramName));
	}

	public static void notEmpty(Collection<?> value, String paramName) {
		if (value.isEmpty())
			throw new IllegalArgumentException(String.format("parameter [%s] is empty", paramName));
	}

}
