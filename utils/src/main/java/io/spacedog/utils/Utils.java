package io.spacedog.utils;

import java.nio.charset.Charset;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public class Utils {

	public static final Charset UTF8 = Charset.forName("UTF-8");

	public static StackTraceElement getStackTraceElement() {
		return new Exception().getStackTrace()[1];
	}

	public static StackTraceElement getParentStackTraceElement() {
		return new Exception().getStackTrace()[2];
	}

	public static StackTraceElement getGrandParentStackTraceElement() {
		return new Exception().getStackTrace()[3];
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

	public static <E> boolean isNullOrEmpty(Collection<E> c) {
		return c == null || c.isEmpty();
	}

	public static void info() {
		System.out.println();
	}

	public static void info(String message, Object... objects) {
		System.out.println(String.format(message, objects));
	}

	public static void info(String nodeName, JsonNode node) throws JsonProcessingException {
		Utils.info("%s = %s", nodeName, //
				Json.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(node));
	}

	public static void warn(String message, Throwable t) {
		System.err.println("[SpaceDog Warning] " + message);
		t.printStackTrace();
	}
}
