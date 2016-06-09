package io.spacedog.utils;

public class Exceptions {

	public static RuntimeException runtime(String message, Object... arguments) {
		return new RuntimeException(String.format(message, arguments));
	}

	public static RuntimeException runtime(Throwable t) {
		return new RuntimeException(t);
	}

	public static IllegalArgumentException illegalArgument(String message, Object... arguments) {
		return new IllegalArgumentException(String.format(message, arguments));
	}

	public static NotFoundException notFound(String message, Object... arguments) {
		return new NotFoundException(String.format(message, arguments));
	}

	public static SpaceException space(int httpStatus, String message, Object... arguments) {
		return new SpaceException(httpStatus, message, arguments);
	}

	public static SpaceException space(int httpStatus, Throwable cause, String message, Object... arguments) {
		return new SpaceException(httpStatus, cause, message, arguments);
	}
}
