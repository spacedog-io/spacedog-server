/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.apache.http.HttpStatus;

import io.spacedog.utils.SpaceException;

public class AuthorizationException extends SpaceException {

	private static final long serialVersionUID = 25496310542011899L;

	public AuthorizationException(String message) {
		super(HttpStatus.SC_UNAUTHORIZED, message);
	}

	public AuthorizationException(Credentials credentials) {
		this("user [%s] of level [%s] not authorized", credentials.name(), credentials.level());
	}

	public AuthorizationException(String message, Object... parameters) {
		super(HttpStatus.SC_UNAUTHORIZED, message, parameters);
	}

	public AuthorizationException(String message, IllegalArgumentException cause) {
		super(HttpStatus.SC_UNAUTHORIZED, cause, message);
	}
}