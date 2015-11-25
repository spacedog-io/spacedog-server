package io.spacedog.services;

public class Utils {

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
}
