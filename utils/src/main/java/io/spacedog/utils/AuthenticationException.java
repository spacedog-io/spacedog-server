/**
 * © David Attias 2015
 */
package io.spacedog.utils;

public class AuthenticationException extends SpaceException {

	private static final long serialVersionUID = 25496310542011899L;

	public AuthenticationException(String message, Object... args) {
		super(401, message, args);
	}

	public AuthenticationException(Throwable cause, String message, Object... args) {
		super(401, cause, message, args);
	}
}