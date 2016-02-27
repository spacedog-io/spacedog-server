package io.spacedog.utils;

public class Exceptions {

	public static RuntimeException wrap(String message, Object... arguments) {
		return new RuntimeException(String.format(message, arguments));
	}

	public static RuntimeException wrap(Throwable t) {
		return new RuntimeException(t);
	}

}
