package io.spacedog.http;

public interface SpaceFields {

	String ID_FIELD = "id";
	String VERSION_FIELD = "version";
	String TYPE_FIELD = "type";
	String SORT_FIELD = "sort";
	String SCORE_FIELD = "score";
	String BACKEND_ID_FIELD = "backendId";
	String PASSWORD_FIELD = "password";
	String INVALID_CHALLENGES_FIELD = "invalidChallenges";
	String LAST_INVALID_CHALLENGE_AT_FIELD = "lastInvalidChallengeAt";
	String EMAIL_FIELD = "email";
	String ENABLED_FIELD = "enabled";
	String USERNAME_FIELD = "username";
	String OWNER_FIELD = "owner";
	String GROUP_FIELD = "group";
	String UPDATED_AT_FIELD = "updatedAt";
	String CREATED_AT_FIELD = "createdAt";
	String SESSIONS_FIELD = "sessions";
	String STASH_FIELD = "stash";
	String ACCESS_TOKEN_FIELD = "accessToken";
	String ACCESS_TOKEN_EXPIRES_AT_FIELD = "accessTokenExpiresAt";
	String SESSIONS_ACCESS_TOKEN_FIELD = SESSIONS_FIELD + '.' + ACCESS_TOKEN_FIELD;
	String EXPIRES_IN_FIELD = "expiresIn";
	String HASHED_PASSWORD_FIELD = "hashedPassword";
	String PASSWORD_RESET_CODE_FIELD = "passwordResetCode";
	String PASSWORD_MUST_CHANGE_FIELD = "passwordMustChange";
	String CREDENTIALS_FIELD = "credentials";
	String ROLES_FIELD = "roles";
	String ERROR_FIELD = "error";
	String RECEIVED_AT_FIELD = "receivedAt";
	String DISABLE_AFTER_FIELD = "disableAfter";
	String ENABLE_AFTER_FIELD = "enableAfter";
}
