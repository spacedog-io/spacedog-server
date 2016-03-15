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
}
