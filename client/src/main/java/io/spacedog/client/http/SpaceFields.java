package io.spacedog.client.http;

public interface SpaceFields {

	String ID_FIELD = "id";
	String VERSION_FIELD = "version";
	String TYPE_FIELD = "type";
	String SORT_FIELD = "sort";
	String SCORE_FIELD = "score";
	String BACKEND_ID_FIELD = "backendId";
	String OWNER_FIELD = "owner";
	String GROUP_FIELD = "group";
	String UPDATED_AT_FIELD = "updatedAt";
	String CREATED_AT_FIELD = "createdAt";
	String TAGS_FIELD = "tags";
	String CREDENTIALS_FIELD = "credentials";
	String ERROR_FIELD = "error";
	String RECEIVED_AT_FIELD = "receivedAt";
	String NAME_FIELD = "name";

	/// Files

	String PATH_FIELD = "path";
	String KEY_FIELD = "key";
	String ENCRYPTION_FIELD = "encryption";
	String HASH_FIELD = "hash";
	String SNAPSHOT_FIELD = "snapshot";
	String CONTENT_TYPE_FIELD = "contentType";
	String LENGTH_FIELD = "length";

	/// Credentials

	String PASSWORD_FIELD = "password";
	String INVALID_CHALLENGES_FIELD = "invalidChallenges";
	String LAST_INVALID_CHALLENGE_AT_FIELD = "lastInvalidChallengeAt";
	String EMAIL_FIELD = "email";
	String EMAIL_TEXT_FIELD = "email.text";
	String ENABLED_FIELD = "enabled";
	String USERNAME_FIELD = "username";
	String USERNAME_TEXT_FIELD = "username.text";
	String ACCESS_TOKEN_FIELD = "accessToken";
	String ACCESS_TOKEN_EXPIRES_AT_FIELD = "accessTokenExpiresAt";
	String EXPIRES_IN_FIELD = "expiresIn";
	String HASHED_PASSWORD_FIELD = "hashedPassword";
	String PASSWORD_RESET_CODE_FIELD = "passwordResetCode";
	String PASSWORD_MUST_CHANGE_FIELD = "passwordMustChange";
	String DISABLE_AFTER_FIELD = "disableAfter";
	String ENABLE_AFTER_FIELD = "enableAfter";
	String ROLES_FIELD = "roles";
	String GROUPS_FIELD = "groups";
	String SESSIONS_FIELD = "sessions";
	String SESSIONS_ACCESS_TOKEN_FIELD = SESSIONS_FIELD + '.' + ACCESS_TOKEN_FIELD;
}
