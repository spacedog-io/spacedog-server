/**
 * © David Attias 2015
 */
package io.spacedog.utils;

public class AuthenticationException extends SpaceException {

	private static final long serialVersionUID = 25496310542011899L;

	public AuthenticationException(String code, String message, Object... args) {
		super(code, 401, message, args);
		addWwwAuthenticateHeader();
	}

	public AuthenticationException(String code, //
			Throwable cause, String message, Object... args) {
		super(code, 401, cause, message, args);
		addWwwAuthenticateHeader();
	}

	private void addWwwAuthenticateHeader() {
		withHeader(SpaceHeaders.WWW_AUTHENTICATE, SpaceHeaders.BASIC_SCHEME);
	}

}