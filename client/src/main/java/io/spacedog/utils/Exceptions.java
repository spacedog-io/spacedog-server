package io.spacedog.utils;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.http.SpaceException;

public class Exceptions {

	public static final String ALREADY_EXISTS = "already-exists";
	public static final String INVALID_AUTHORIZATION_HEADER = "invalid-authorization-header";
	public static final String DISABLED_CREDENTIALS = "disabled-credentials";
	public static final String INVALID_ACCESS_TOKEN = "invalid-access-token";
	public static final String EXPIRED_ACCESS_TOKEN = "expired-access-token";
	public static final String INVALID_CREDENTIALS = "invalid-credentials";

	//
	// Generic
	//

	public static SpaceException space(String code, int httpStatus, String message, Object... args) {
		return new SpaceException(code, httpStatus, message, args);
	}

	public static SpaceException space(String code, int httpStatus, Throwable cause, String message, Object... args) {
		return new SpaceException(code, httpStatus, cause, message, args);
	}

	public static SpaceException space(int httpStatus, String message, Object... args) {
		return new SpaceException(httpStatus, message, args);
	}

	public static SpaceException space(int httpStatus, Throwable cause, String message, Object... args) {
		return new SpaceException(httpStatus, cause, message, args);
	}

	//
	// 400
	//

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

	public static SpaceException alreadyExists(String type, String value) {
		return new SpaceException(ALREADY_EXISTS, 400, "[%s][%s] already exists", //
				type, value);
	}

	//
	// 401
	//

	public static AuthenticationException guestNotAuthorized() {
		return new AuthenticationException("guest-not-authorized", "guest not authorized");

	}

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
				"[%s][%s] => disabled", credentials.type(), credentials.username());
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
	// 403
	//

	public static SpaceException forbidden(Credentials credentials, String code, String message, Object... args) {
		if (credentials.isGuest())
			return guestNotAuthorized();

		String prefix = String.format("[%s][%s] => ", credentials.type(), credentials.username());
		return new SpaceException(code, 403, prefix + message, args);
	}

	public static SpaceException forbidden(Credentials credentials, String message, Object... args) {
		return forbidden(credentials, "forbidden", message, args);
	}

	public static SpaceException insufficientPermissions(Credentials credentials) {
		return forbidden(credentials, "insufficient-permissions", "insufficient permissions");
	}

	public static SpaceException passwordMustBeChallenged(Credentials credentials) {
		return forbidden(credentials, "unchallenged-password", "password must be challenged");
	}

	public static SpaceException passwordMustChange(Credentials credentials) {
		return forbidden(credentials, "password-must-change", "password must change");
	}

	//
	// 404
	//

	public static SpaceException notFound(String type, String id) {
		return new SpaceException("not-found", 404, "[%s][%s] not found", type, id);
	}

	public static SpaceException notFound(DataWrap<?> object) {
		return notFound(object.type(), object.id());
	}

	//
	// 405
	//

	public static SpaceException unsupportedOperation(String message, Object... args) {
		return new SpaceException("unsupported", 405, message, args);
	}

	public static SpaceException unsupportedHttpRequest(String method, String uri) {
		return unsupportedOperation("[%s][%s] is not yet supported", method, uri);
	}

	//
	// 409
	//

	public static SpaceException invalidState(String code, String message, Object... args) {
		return new SpaceException(code, 409, message, args);
	}

	//
	// 500
	//

	public static RuntimeException runtime(String message, Object... args) {
		return new RuntimeException(String.format(message, args));
	}

	public static RuntimeException runtime(Throwable t) {
		return new RuntimeException(t);
	}

	public static RuntimeException runtime(Throwable t, String message, Object... args) {
		return new RuntimeException(String.format(message, args), t);
	}

}
