package io.spacedog.utils;

public class Exceptions {

	public static RuntimeException runtime(String message, Object... args) {
		return new RuntimeException(String.format(message, args));
	}

	public static RuntimeException runtime(Throwable t) {
		return new RuntimeException(t);
	}

	public static IllegalArgumentException illegalArgument(String message, Object... args) {
		return new IllegalArgumentException(String.format(message, args));
	}

	public static IllegalArgumentException illegalArgument(Throwable t) {
		return new IllegalArgumentException(t);
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

	public static ForbiddenException forbidden(String message, Object... args) {
		return new ForbiddenException(message, args);
	}

	public static AuthenticationException invalidAuthentication(String message, Object... args) {
		return new AuthenticationException(message, args);
	}

	public static ForbiddenException insufficientCredentials(Credentials credentials) {
		return forbidden("[%s][%s] has insufficient credentials", //
				credentials.level(), credentials.name());
	}

}
