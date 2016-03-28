/**
 * © David Attias 2015
 */
package io.spacedog.services;

import io.spacedog.utils.Check;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;

public abstract class Resource {

	public static final String SLASH = "/";

	public static final int SHARDS_DEFAULT = 1;
	public static final int REPLICAS_DEFAULT = 0;

	public static final String SPACEDOG_BACKEND = "spacedog";

	// Field names
	public static final String ID = "id";
	public static final String BACKEND_ID = "backendId";
	public static final String PASSWORD = "password";
	public static final String EMAIL = "email";
	public static final String USERNAME = "username";
	public static final String UPDATED_AT = "updatedAt";
	public static final String CREATED_AT = "createdAt";
	public static final String HASHED_PASSWORD = "hashedPassword";
	public static final String PASSWORD_RESET_CODE = "passwordResetCode";
	public static final String CREDENTIALS_LEVEL = "level";

	public static StringBuilder spaceUrl(String backendId, String uri, String type, String id) {
		return spaceUrl(backendId, uri).append(SLASH).append(type).append(SLASH).append(id);
	}

	public static StringBuilder spaceUrl(String backendId, String uri) {
		Check.notNullOrEmpty(uri, "URI");
		Check.isTrue(uri.startsWith(SLASH), "URI must start with a /");
		return spaceRootUrl(backendId).append(uri);
	}

	public static StringBuilder spaceRootUrl(String backendId) {
		return new StringBuilder(Start.get().configuration().apiUrl(backendId));
	}

	protected static String getReferenceType(String reference) {
		return Utils.splitBySlash(reference)[0];
	}

	protected static String getReferenceId(String reference) {
		return Utils.splitBySlash(reference)[1];
	}

	protected static String get(Context context, String name, String defaultValue) {
		String value = context.get(name);
		return value == null ? defaultValue : value;
	}

	public static String getBucketName(String bucketSuffix) {
		return Start.get().configuration().getAwsBucketPrefix() + bucketSuffix;

	}
}
