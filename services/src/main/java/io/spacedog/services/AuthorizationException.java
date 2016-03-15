/**
 * © David Attias 2015
 */
package io.spacedog.services;

@SuppressWarnings("serial")
public class AuthorizationException extends RuntimeException {

	public AuthorizationException(String message) {
		super(message);
	}

	public AuthorizationException(Credentials credentials) {
		this("user [%s] of type [%s] not authorized", credentials.name(), credentials.type());
	}

	public AuthorizationException(String message, Object... parameters) {
		super(String.format(message, parameters));
	}

	public AuthorizationException(String message, IllegalArgumentException cause) {
		super(message, cause);
	}
}