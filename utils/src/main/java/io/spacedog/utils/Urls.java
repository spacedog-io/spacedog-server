package io.spacedog.utils;

public class Urls {

	public static String trimSlash(String uri) {
		return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
	}

}
