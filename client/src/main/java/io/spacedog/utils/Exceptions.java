package io.spacedog.utils;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.http.SpaceException;

public class Exceptions {

	public static final String ALREADY_EXISTS = "already-exists";
	public static final String UNCHALLENGED_PASSWORD = "unchallenged-password";
	public static final String PASSWORD_MUST_CHANGE = "password-must-change";
	public static final String INVALID_AUTHORIZATION_HEADER = "invalid-authorization-header";
	public static final String DISABLED_CREDENTIALS = "disabled-credentials";
	public static final String INVALID_ACCESS_TOKEN = "invalid-access-token";
	public static final String EXPIRED_ACCESS_TOKEN = "expired-access-token";
	public static final String INVALID_CREDENTIALS = "invalid-credentials";

	public static RuntimeException runtime(String message, Object... args) {
		return new RuntimeException(String.format(message, args));
	}

	public static RuntimeException runtime(Throwable t) {
		return new RuntimeException(t);
	}

	public static RuntimeException runtime(Throwable t, String message, Object... args) {
		return new RuntimeException(String.format(message, args), t);
	}

	public static UnsupportedOperationException unsupportedOperation(String message, Object... args) {
		return new UnsupportedOperationException(String.format(message, args));
	}

	public static IllegalArgumentException illegalArgument(String message, Object... args) {
		return new IllegalArgumentException(String.format(message, args));
	}

	public static IllegalArgumentException illegalArgument(Throwable t) {
		return new IllegalArgumentException(t);
	}

	public static IllegalArgumentException illegalArgument(Throwable t, String message, Object... args) {
		return new IllegalArgumentException(String.format(message, args), t);
	}

	public static IllegalArgumentException invalidFieldPath(JsonNode json, String fieldPath) {
		return illegalArgument("field path [%s] invalid for [%s]", fieldPath, json);
	}

	public static NotFoundException notFound(String type, String id) {
		return new NotFoundException("[%s][%s] not found", type, id);
	}

	public static SpaceException space(int httpStatus, String message, Object... args) {
		return new SpaceException(httpStatus, message, args);
	}

	public static SpaceException space(int httpStatus, Throwable cause, String message, Object... args) {
		return new SpaceException(httpStatus, cause, message, args);
	}

	public static SpaceException illegalState(String message, Object... args) {
		return new SpaceException(409, message, args);
	}

	public static SpaceException illegalState(Throwable t, String message, Object... args) {
		return new SpaceException(409, message, args);
	}

	public static ForbiddenException forbidden(String message, Object... args) {
		return new ForbiddenException(message, args);
	}

	public static ForbiddenException insufficientCredentials(Credentials credentials) {
		return forbidden("[%s][%s] has insufficient credentials", //
				credentials.type(), credentials.username());
	}

	public static SpaceException alreadyExists(String type, String value) {
		return new SpaceException(ALREADY_EXISTS, 400, "[%s][%s] already exists", //
				type, value);
	}

	public static SpaceException passwordMustBeChallenged() {
		return new SpaceException(UNCHALLENGED_PASSWORD, 403, "password must be challenged");
	}

	public static SpaceException passwordMustChange(Credentials credentials) {
		return new SpaceException(PASSWORD_MUST_CHANGE, 403, //
				"[%s][%s] credentials password must change", //
				credentials.type(), credentials.username());
	}

	//
	// 401
	//

	public static AuthenticationException invalidAuthorizationHeader(//
			String message, Object... args) {
		return new AuthenticationException(INVALID_AUTHORIZATION_HEADER, message, args);
	}

	public static AuthenticationException invalidAuthorizationHeader(//
			Throwable t, String message, Object... args) {
		return new AuthenticationException(INVALID_AUTHORIZATION_HEADER, t, message, args);
	}

	public static AuthenticationException disabledCredentials(Credentials credentials) {
		return new AuthenticationException(DISABLED_CREDENTIALS, //
				"[%s][%s] credentials disabled", credentials.type(), credentials.username());
	}

	public static AuthenticationException invalidAccessToken() {
		return new AuthenticationException(INVALID_ACCESS_TOKEN, //
				"invalid access token");
	}

	public static AuthenticationException accessTokenHasExpired() {
		return new AuthenticationException(EXPIRED_ACCESS_TOKEN, "access token has expired");
	}

	public static AuthenticationException invalidUsernamePassword() {
		return new AuthenticationException(INVALID_CREDENTIALS, //
				"invalid username or password");
	}

	//
	// 405
	//

	public static SpaceException unsupportedHttpRequest(String method, String uri) {
		return new SpaceException("unsupported", 405, "http request [%s][%s] is not supported", method, uri);
	}

}
