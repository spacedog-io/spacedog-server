package io.spacedog.utils;

public interface SpaceFields {

	String FIELD_ID = "id";
	String FIELD_BACKEND_ID = "backendId";
	String FIELD_PASSWORD = "password";
	String FIELD_INVALID_CHALLENGES = "invalidChallenges";
	String FIELD_LAST_INVALID_CHALLENGE_AT = "lastInvalidChallengeAt";
	String FIELD_EMAIL = "email";
	String FIELD_LEVEL = "level";
	String FIELD_ENABLED = "enabled";
	String FIELD_USERNAME = "username";
	String FIELD_UPDATED_AT = "updatedAt";
	String FIELD_CREATED_AT = "createdAt";
	String FIELD_SESSIONS = "sessions";
	String FIELD_STASH = "stash";
	String FIELD_ACCESS_TOKEN = "accessToken";
	String FIELD_ACCESS_TOKEN_EXPIRES_AT = "accessTokenExpiresAt";
	String FIELD_SESSIONS_ACCESS_TOKEN = FIELD_SESSIONS + '.' + FIELD_ACCESS_TOKEN;
	String FIELD_EXPIRES_IN = "expiresIn";
	String FIELD_HASHED_PASSWORD = "hashedPassword";
	String FIELD_PASSWORD_RESET_CODE = "passwordResetCode";
	String FIELD_PASSWORD_MUST_CHANGE = "passwordMustChange";
	String FIELD_CREDENTIALS_LEVEL = "level";
	String FIELD_CREDENTIALS = "credentials";
	String FIELD_ROLES = "roles";
	String FIELD_ERROR = "error";
	String FIELD_RECEIVED_AT = "receivedAt";
	public String FIELD_DISABLE_AFTER = "disableAfter";
	public String FIELD_ENABLE_AFTER = "enableAfter";

}
