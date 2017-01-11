package io.spacedog.utils;

public interface SpaceFields {

	String ID = "id";
	String BACKEND_ID = "backendId";
	String PASSWORD = "password";
	String EMAIL = "email";
	String LEVEL = "level";
	String ENABLED = "enabled";
	String USERNAME = "username";
	String UPDATED_AT = "updatedAt";
	String CREATED_AT = "createdAt";
	String SESSIONS = "sessions";
	String STASH = "stash";
	String ACCESS_TOKEN = "accessToken";
	String ACCESS_TOKEN_EXPIRES_AT = "accessTokenExpiresAt";
	String SESSIONS_ACCESS_TOKEN = SESSIONS + '.' + ACCESS_TOKEN;
	String EXPIRES_IN = "expiresIn";
	String HASHED_PASSWORD = "hashedPassword";
	String PASSWORD_RESET_CODE = "passwordResetCode";
	String CREDENTIALS_LEVEL = "level";
	String CREDENTIALS = "credentials";
	String ROLES = "roles";
	String ERROR = "error";
	String RECEIVED_AT = "receivedAt";

}
