package io.spacedog.utils;

public class Exceptions {

	public static RuntimeException runtime(String message, Object... args) {
		return new RuntimeException(String.format(message, args));
	}

	public static RuntimeException runtime(Throwable t) {
		return new RuntimeException(t);
	}

	public static RuntimeException runtime(Throwable t, String message, Object... args) {
		return new RuntimeException(String.format(message, args), t);
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

	public static NotFoundException notFound(String backendId, String type, String id) {
		return new NotFoundException("[%s][%s] not found in backend [%s]", type, id, backendId);
	}

	public static SpaceException space(int httpStatus, String message, Object... args) {
		return new SpaceException(httpStatus, message, args);
	}

	public static SpaceException space(int httpStatus, Throwable cause, String message, Object... args) {
		return new SpaceException(httpStatus, cause, message, args);
	}

	public static IllegalStateException illegalState(String message, Object... args) {
		return new IllegalStateException(String.format(message, args));
	}

	public static IllegalStateException illegalState(Throwable t, String message, Object... args) {
		return new IllegalStateException(String.format(message, args), t);
	}

	public static ForbiddenException forbidden(String message, Object... args) {
		return new ForbiddenException(message, args);
	}

	public static ForbiddenException insufficientCredentials(Credentials credentials) {
		return forbidden("[%s][%s] has insufficient credentials", //
				credentials.level(), credentials.name());
	}

	public static SpaceException alreadyExists(String type, String value) {
		return new SpaceException("already-exists", 400, "[%s][%s] already exists", //
				type, value);
	}

	public static SpaceException passwordMustBeChallenged() {
		return new SpaceException("unchallenged-password", 403, "password must be challenged");
	}

	//
	// 401
	//

	public static AuthenticationException invalidAuthorizationHeader(//
			String message, Object... args) {
		return new AuthenticationException("invalid-authorization-header", message, args);
	}

	public static AuthenticationException invalidAuthorizationHeader(//
			Throwable t, String message, Object... args) {
		return new AuthenticationException("invalid-authorization-header", t, message, args);
	}

	public static AuthenticationException disabledCredentials(Credentials credentials) {
		return new AuthenticationException("disabled-credentials", //
				"[%s][%s] credentials disabled", credentials.level(), credentials.name());
	}

	public static AuthenticationException invalidAccessToken(String backendId) {
		return new AuthenticationException("invalid-access-token", //
				"invalid access token for backend [%s]", backendId);
	}

	public static AuthenticationException accessTokenHasExpired() {
		return new AuthenticationException("expired-access-token", "access token has expired");
	}

	public static AuthenticationException invalidUsernamePassword(String backendId) {
		return new AuthenticationException("invalid-credentials", //
				"invalid username or password for backend [%s]", backendId);
	}

}
