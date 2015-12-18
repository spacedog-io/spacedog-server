/**
 * © David Attias 2015
 */
package io.spacedog.services;

@SuppressWarnings("serial")
public class AuthenticationException extends RuntimeException {

	public AuthenticationException(String message) {
		super(message);
	}

	public AuthenticationException(String message, Object... parameters) {
		super(String.format(message, parameters));
	}

	public AuthenticationException(String message, IllegalArgumentException cause) {
		super(message, cause);
	}
}