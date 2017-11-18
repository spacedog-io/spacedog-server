package io.spacedog.utils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.io.Resources;

public class Utils {

	public static final Charset UTF8 = Charset.forName("UTF-8");

	//
	// Object utils
	//

	public static <K> K instantiate(Class<K> objectClass) {
		try {
			return objectClass.newInstance();

		} catch (InstantiationException | IllegalAccessException e) {
			throw Exceptions.runtime(e, "error instantiating [%s] object class", //
					objectClass.getSimpleName());
		}
	}

	//
	// Collections utils
	//

	public static boolean containsIgnoreCase(Collection<String> strings, String value) {
		for (String string : strings)
			if (value.equalsIgnoreCase(string))
				return true;
		return false;
	}

	//
	// Exceptions utils
	//

	public static StackTraceElement getStackTraceElement() {
		return new Exception().getStackTrace()[1];
	}

	public static StackTraceElement getParentStackTraceElement() {
		return new Exception().getStackTrace()[2];
	}

	public static StackTraceElement getGrandParentStackTraceElement() {
		return new Exception().getStackTrace()[3];
	}

	//
	// Strings utils
	//

	public static String join(CharSequence delimiter, CharSequence... elements) {
		Objects.requireNonNull(delimiter);
		Objects.requireNonNull(elements);
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (CharSequence cs : elements) {
			if (!first)
				builder.append(delimiter);
			builder.append(cs);
			first = false;
		}
		return builder.toString();
	}

	public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
		Objects.requireNonNull(delimiter);
		Objects.requireNonNull(elements);
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (CharSequence cs : elements) {
			if (!first)
				builder.append(delimiter);
			builder.append(cs);
			first = false;
		}
		return builder.toString();
	}

	public static String concat(Object... objects) {
		StringBuilder builder = new StringBuilder();
		for (Object object : objects)
			builder.append(object.toString());
		return builder.toString();
	}

	public static String[] splitByDot(String propertyPath) {
		propertyPath = propertyPath.trim();
		if (propertyPath.startsWith("."))
			propertyPath = propertyPath.substring(1);
		if (propertyPath.endsWith("."))
			propertyPath = propertyPath.substring(0, propertyPath.length() - 1);
		return propertyPath.split("\\.");
	}

	public static String[] splitBySlash(String propertyPath) {
		propertyPath = propertyPath.trim();
		if (propertyPath.startsWith("/"))
			propertyPath = propertyPath.substring(1);
		if (propertyPath.endsWith("/"))
			propertyPath = propertyPath.substring(0, propertyPath.length() - 1);
		return propertyPath.split("/");
	}

	public static String removePreffix(String s, String preffix) {
		if (Strings.isNullOrEmpty(preffix))
			return s;
		return s.startsWith(preffix) ? s.substring(preffix.length()) : s;
	}

	public static String removeSuffix(String s, String suffix) {
		if (Strings.isNullOrEmpty(suffix))
			return s;
		return s.endsWith(suffix) ? s.substring(0, s.length() - suffix.length()) : s;
	}

	//
	// Others
	//

	public static String toUri(String[] uriTerms) {
		StringBuilder builder = new StringBuilder();
		for (String term : uriTerms)
			builder.append('/').append(term);
		return builder.toString();
	}

	public static boolean isDigit(char c) {
		return c == '1' || c == '2' || c == '3' || c == '4' || c == '5' //
				|| c == '6' || c == '7' || c == '8' || c == '9';
	}

	public static <T> boolean isNullOrEmpty(Collection<T> collection) {
		return collection == null || collection.isEmpty();
	}

	public static <T> boolean isNullOrEmpty(T[] array) {
		return array == null || array.length == 0;
	}

	public static void info() {
		System.out.println();
	}

	public static void info(String message, Object... arguments) {
		System.out.println(String.format(message, arguments));
	}

	public static void infoNoLn(String message, Object... arguments) {
		System.out.print(String.format(message, arguments));
	}

	public static void info(String nodeName, JsonNode node) {
		Utils.info("%s = %s", nodeName, Json.toPrettyString(node));
	}

	public static void warn(String message, Object... arguments) {
		System.err.println("[SpaceDog][Warning] " + String.format(message, arguments));
	}

	public static void warn(String message, Throwable t) {
		System.err.println("[SpaceDog][Warning] " + message);
		t.printStackTrace();
	}

	//
	// Array utils
	//

	public static String[] toArray(String... parts) {
		return parts;
	}

	public static Object[] toArray(Object... parts) {
		return parts;
	}

	//
	// Resource Utils
	//

	public static byte[] readResource(String path) {
		try {
			return Resources.toByteArray(Resources.getResource(path));
		} catch (IOException e) {
			throw Exceptions.runtime(e, "error reading resource [%s]", path);
		}
	}

	public static byte[] readResource(Class<?> contextClass, String resourceName) {
		try {
			return Resources.toByteArray(Resources.getResource(contextClass, resourceName));
		} catch (IOException e) {
			throw Exceptions.runtime(e, "error reading resource [%s][%s]", //
					contextClass.getSimpleName(), resourceName);
		}
	}

	public static void closeSilently(Closeable closeable) {
		try {
			if (closeable != null)
				closeable.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
