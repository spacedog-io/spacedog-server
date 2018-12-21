package io.spacedog.utils;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.http.SpaceException;
import io.spacedog.client.http.SpaceHeaders;

public class Exceptions {

	//
	// Generic
	//

	public static SpaceException exception(String code, int httpStatus, //
			Throwable cause, String message, Object... args) {

		return Utils.isNull(cause) ? new SpaceException(code, httpStatus, message, args)
				: new SpaceException(code, httpStatus, cause, message, args);
	}

	public static SpaceException exception(String code, int httpStatus, String message, Object... args) {
		return exception(code, httpStatus, null, message, args);
	}

	public static SpaceException exception(int httpStatus, String message, Object... args) {
		return exception(null, httpStatus, message, args);
	}

	public static SpaceException exception(int httpStatus, Throwable cause, String message, Object... args) {
		return exception(null, httpStatus, cause, message, args);
	}

	//
	// 400
	//

	public static SpaceException illegalArgument(String code, Throwable cause, String message, Object... args) {
		return exception(code, 400, cause, message, args);
	}

	public static SpaceException illegalArgument(Throwable t, String message, Object... args) {
		return illegalArgument("bad-request", t, message, args);
	}

	public static SpaceException illegalArgument(String message, Object... args) {
		return illegalArgument(null, message, args);
	}

	public static SpaceException illegalArgumentWithCode(String code, String message, Object... args) {
		return illegalArgument(code, null, message, args);
	}

	public static SpaceException illegalArgument(Throwable t) {
		return illegalArgument(t, t.getMessage());
	}

	public static SpaceException invalidFieldPath(JsonNode json, String fieldPath) {
		return illegalArgument("field path [%s] invalid for [%s]", fieldPath, json);
	}

	public static SpaceException alreadyExists(String type, String value) {
		return illegalArgument("already-exists", null, "[%s][%s] already exists", type, value);
	}

	//
	// 401
	//

	public static SpaceException unauthorized(String code, Throwable cause, String message, Object... args) {
		return exception(code, 401, cause, message, args)//
				.withHeader(SpaceHeaders.WWW_AUTHENTICATE, SpaceHeaders.BASIC_SCHEME);
	}

	public static SpaceException unauthorized(String code, String message, Object... args) {
		return unauthorized(code, null, message, args);
	}

	public static SpaceException unauthorized(String message, Object... args) {
		return unauthorized("unauthorized", message, args);
	}

	public static SpaceException guestsAreUnauthorized() {
		return unauthorized("guests-unauthorized", "guests are unauthorized");
	}

	public static SpaceException invalidAuthorizationHeader(String message, Object... args) {
		return invalidAuthorizationHeader(null, message, args);
	}

	public static SpaceException invalidAuthorizationHeader(Throwable t, String message, Object... args) {
		return unauthorized("invalid-authorization-header", t, message, args);
	}

	public static SpaceException disabledCredentials(Credentials credentials) {
		return unauthorized("disabled-credentials", "[%s][%s] => disabled", //
				credentials.type(), credentials.username());
	}

	public static SpaceException invalidAccessToken() {
		return unauthorized("invalid-access-token", "invalid access token");
	}

	public static SpaceException accessTokenHasExpired() {
		return unauthorized("expired-access-token", "access token has expired");
	}

	public static SpaceException invalidUsernamePassword() {
		return unauthorized("invalid-credentials", "invalid username or password");
	}

	//
	// 403
	//

	public static SpaceException forbidden(String code, Credentials credentials, String message, Object... args) {
		if (credentials.isGuest())
			return guestsAreUnauthorized();

		String prefix = String.format("[%s][%s] is forbidden: ", credentials.type(), credentials.username());
		return exception(code, 403, prefix + message, args);
	}

	public static SpaceException forbidden(Credentials credentials, String message, Object... args) {
		return forbidden("forbidden", credentials, message, args);
	}

	public static SpaceException insufficientPermissions(Credentials credentials) {
		return forbidden("insufficient-permissions", credentials, "insufficient permissions");
	}

	public static SpaceException passwordMustBeChallenged(Credentials credentials) {
		return forbidden("unchallenged-password", credentials, "password must be challenged");
	}

	public static SpaceException passwordMustChange(Credentials credentials) {
		return forbidden("password-must-change", credentials, "password must change");
	}

	//
	// 404
	//

	public static SpaceException notFound(String message, Object... args) {
		return exception("not-found", 404, message, args);
	}

	public static SpaceException objectNotFound(String type, String resourceName) {
		return notFound("[%s][%s] not found", type, resourceName);
	}

	public static SpaceException objectNotFound(DataWrap<?> object) {
		return objectNotFound(object.type(), object.id());
	}

	//
	// 405
	//

	public static SpaceException methodNotAllowed(String method, String uri) {
		return exception("http-method-not-allowed", 405, //
				"[%s][%s] is not yet supported", method, uri);
	}

	//
	// 409
	//

	public static SpaceException invalidState(String code, String message, Object... args) {
		return exception(code, 409, message, args);
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

	//
	// 501
	//

	public static SpaceException notImplemented(String message, Object... args) {
		return exception("not-implemented", 501, message, args);
	}

}
