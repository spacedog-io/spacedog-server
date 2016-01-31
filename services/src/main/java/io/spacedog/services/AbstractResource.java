/**
 * © David Attias 2015
 */
package io.spacedog.services;

import com.google.common.base.Strings;

import io.spacedog.utils.Check;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;

public abstract class AbstractResource {

	public static final String SLASH = "/";

	public static StringBuilder spaceUrl(String uri, String type, String id) {
		return spaceUrl(uri).append(SLASH).append(type).append(SLASH).append(id);
	}

	public static StringBuilder spaceUrl(String uri) {
		Check.notNullOrEmpty(uri, "URI");
		Check.isTrue(uri.startsWith(SLASH), "URI must start with a /");
		return spaceRootUrl().append(uri);
	}

	public static StringBuilder spaceRootUrl() {
		return new StringBuilder(Start.get().configuration().sslUrl());
	}

	protected static String getReferenceType(String reference) {
		return Utils.splitBySlash(reference)[0];
	}

	protected static String getReferenceId(String reference) {
		return Utils.splitBySlash(reference)[1];
	}

	protected static boolean isTest(Context context) {
		String header = context.header(SpaceHeaders.SPACEDOG_TEST);
		return Strings.isNullOrEmpty(header) ? false : Boolean.parseBoolean(header);
	}

	protected static String get(Context context, String name, String defaultValue) {
		String value = context.get(name);
		return value == null ? defaultValue : value;
	}

	public static String getBucketName(String bucketSuffix) {
		return Start.get().configuration().getAwsBucketPrefix() + bucketSuffix;

	}
}
