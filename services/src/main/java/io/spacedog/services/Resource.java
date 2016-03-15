/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import io.spacedog.utils.Check;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;

public abstract class Resource {

	public static final String SLASH = "/";
	public static final int SHARDS_DEFAULT = 1;
	public static final int REPLICAS_DEFAULT = 0;

	public static StringBuilder spaceUrl(Optional<String> backendId, String uri, String type, String id) {
		return spaceUrl(backendId, uri).append(SLASH).append(type).append(SLASH).append(id);
	}

	public static StringBuilder spaceUrl(Optional<String> backendId, String uri) {
		Check.notNullOrEmpty(uri, "URI");
		Check.isTrue(uri.startsWith(SLASH), "URI must start with a /");
		return spaceRootUrl(backendId).append(uri);
	}

	public static StringBuilder spaceRootUrl(Optional<String> backendId) {
		return new StringBuilder(Start.get().configuration().sslUrl(backendId));
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

	public static final String SPACEDOG_BACKEND = "spacedog";
	public static final Set<String> INTERNAL_BACKENDS = Sets.newHashSet(SPACEDOG_BACKEND);
	public static final String BACKEND_ID = "backendId";
}
