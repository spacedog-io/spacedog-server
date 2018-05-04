package io.spacedog.utils;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

public class SpaceException extends RuntimeException {

	private static final long serialVersionUID = 491847884892423601L;

	private int httpStatus;
	private String code;
	private ObjectNode details;
	private Map<String, String> headers = Maps.newLinkedHashMap();

	public SpaceException(int httpStatus, String message, Object... args) {
		this(fromStatusToCode(httpStatus), httpStatus, message, args);
	}

	public SpaceException(String code, int httpStatus, String message, Object... args) {
		super(String.format(message, args));
		this.httpStatus = httpStatus;
		this.code = code;
	}

	public SpaceException(int httpStatus, Throwable cause, String message, Object... args) {
		this(fromStatusToCode(httpStatus), httpStatus, cause, message, args);
	}

	public SpaceException(String code, int httpStatus, Throwable cause, String message, Object... args) {
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

	public SpaceException code(String code) {
		this.code = code;
		return this;
	}

	public ObjectNode details() {
		return details;
	}

	public SpaceException details(ObjectNode details) {
		this.details = details;
		return this;
	}

	public SpaceException cause(Throwable t) {
		this.initCause(t);
		return this;
	}

	public SpaceException withHeader(String key, String value) {
		if (value != null)
			headers.put(key, value);
		return this;
	}

	public Map<String, String> headers() {
		return headers;
	}

	//
	// implementation
	//

	private static String fromStatusToCode(int httpStatus) {
		return Integer.toString(httpStatus);
	}

}
