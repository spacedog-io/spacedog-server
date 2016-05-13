package io.spacedog.utils;

import com.google.common.base.Strings;

public class Uris {

	public static final String SLASH = "/";
	public static final String[] ROOT = new String[0];

	public static String trimSlash(String uri) {
		return uri.endsWith(SLASH) ? uri.substring(0, uri.length() - 1) : uri;
	}

	public static String[] split(String uri) {
		if (Strings.isNullOrEmpty(uri) || uri.equals(SLASH))
			return ROOT;
		if (uri.startsWith(SLASH))
			uri = uri.substring(1);
		if (uri.endsWith(SLASH))
			uri = uri.substring(0, uri.length() - 1);
		return uri.split(SLASH);
	}

	public static String join(String... parts) {
		return SLASH + String.join(SLASH, parts);
	}

	public static String[] toPath(String... parts) {
		return parts;
	}

	public static boolean isRoot(String... parts) {
		return parts == null || parts.length == 0;
	}
}
