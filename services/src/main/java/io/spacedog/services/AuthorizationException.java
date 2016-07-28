/**
 * © David Attias 2015
 */
package io.spacedog.services;

import io.spacedog.utils.SpaceException;
import net.codestory.http.constants.HttpStatus;

public class AuthorizationException extends SpaceException {

	private static final long serialVersionUID = 25496310542011899L;

	public AuthorizationException(String message) {
		super(HttpStatus.UNAUTHORIZED, message);
	}

	@Deprecated
	public AuthorizationException(Credentials credentials) {
		this("user [%s] of level [%s] not authorized", credentials.name(), credentials.level());
	}

	public AuthorizationException(String message, Object... parameters) {
		super(HttpStatus.UNAUTHORIZED, message, parameters);
	}

	public AuthorizationException(String message, Throwable cause) {
		super(HttpStatus.UNAUTHORIZED, cause, message);
	}
}