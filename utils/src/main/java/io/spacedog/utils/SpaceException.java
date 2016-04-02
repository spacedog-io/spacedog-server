package io.spacedog.utils;

import org.apache.http.HttpStatus;

public class SpaceException extends RuntimeException {

	private static final long serialVersionUID = 491847884892423601L;

	private int httpStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;

	public SpaceException(int httpStatus, String message, Object... args) {
		super(String.format(message, args));
		this.httpStatus = httpStatus;
	}

	public SpaceException(int httpStatus, Throwable cause, String message, Object... args) {
		super(String.format(message, args), cause);
		this.httpStatus = httpStatus;
	}

	public int httpStatus() {
		return httpStatus;
	}
}
