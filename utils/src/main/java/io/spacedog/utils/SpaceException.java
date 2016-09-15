package io.spacedog.utils;

public class SpaceException extends RuntimeException {

	private static final long serialVersionUID = 491847884892423601L;

	private int httpStatus;
	private String code;

	public SpaceException(int httpStatus, String message, Object... args) {
		this(httpStatus, fromStatusToCode(httpStatus), message, args);
	}

	public SpaceException(int httpStatus, String code, String message, Object... args) {
		super(String.format(message, args));
		this.httpStatus = httpStatus;
		this.code = code;
	}

	public SpaceException(int httpStatus, Throwable cause, String message, Object... args) {
		this(httpStatus, fromStatusToCode(httpStatus), cause, message, args);
	}

	public SpaceException(int httpStatus, String code, Throwable cause, String message, Object... args) {
		super(String.format(message, args), cause);
		this.httpStatus = httpStatus;
		this.code = code;
	}

	public int httpStatus() {
		return httpStatus;
	}

	public String code() {
		return code;
	}

	//
	// implementation
	//

	private static String fromStatusToCode(int httpStatus) {
		return Integer.toString(httpStatus);
	}

}
