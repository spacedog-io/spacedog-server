/**
 * © David Attias 2015
 */
package io.spacedog.utils;

public class ForbiddenException extends SpaceException {

	private static final long serialVersionUID = 7805189968247866416L;

	public ForbiddenException(String message, Object... args) {
		super(403, message, args);
	}

	public ForbiddenException(Throwable cause, String message, Object... args) {
		super(403, cause, message, args);
	}
}