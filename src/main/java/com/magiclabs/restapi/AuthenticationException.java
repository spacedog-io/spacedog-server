package com.magiclabs.restapi;

@SuppressWarnings("serial")
public class AuthenticationException extends RuntimeException {

	public AuthenticationException(String message) {
		super(message);
	}

	public AuthenticationException(String message,
			IllegalArgumentException cause) {
		super(message, cause);
	}
}