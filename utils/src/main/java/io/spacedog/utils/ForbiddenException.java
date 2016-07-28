/**
 * © David Attias 2015
 */
package io.spacedog.utils;

public class ForbiddenException extends SpaceException {

	private static final long serialVersionUID = 7805189968247866416L;

	public ForbiddenException(String message) {
		super(403, message);
	}

	public ForbiddenException(String message, Object... parameters) {
		super(403, message, parameters);
	}

	public ForbiddenException(String message, Throwable cause) {
		super(403, cause, message);
	}
}