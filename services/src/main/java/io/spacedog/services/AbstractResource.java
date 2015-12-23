/**
 * © David Attias 2015
 */
package io.spacedog.services;

import io.spacedog.utils.Utils;

public abstract class AbstractResource {

	public static final String BASE_URL = "https://spacedog.io";

	protected static String toUrl(String uri, String type, String id) {
		return new StringBuilder(BASE_URL).append(uri).append('/').append(type).append('/').append(id).toString();
	}

	protected static String getReferenceType(String reference) {
		return Utils.splitBySlash(reference)[0];
	}

	protected static String getReferenceId(String reference) {
		return Utils.splitBySlash(reference)[1];
	}
}
