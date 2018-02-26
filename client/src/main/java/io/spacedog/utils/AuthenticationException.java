/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import io.spacedog.client.http.SpaceException;

public class AuthenticationException extends SpaceException {

	private static final long serialVersionUID = 25496310542011899L;

	public AuthenticationException(String code, String message, Object... args) {
		super(code, 401, message, args);
	}

	public AuthenticationException(String code, Throwable cause, String message, Object... args) {
		super(code, 401, cause, message, args);
	}
}