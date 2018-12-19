package io.spacedog.client.http;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;

import io.spacedog.utils.Utils;

public class SpaceException extends RuntimeException {

	private static final long serialVersionUID = 491847884892423601L;

	private String message;
	private int httpStatus;
	private String code;
	private JsonNode details;
	private Map<String, String> headers = Maps.newLinkedHashMap();

	public SpaceException(int httpStatus, String message, Object... args) {
		this(fromStatusToCode(httpStatus), httpStatus, message, args);
	}

	public SpaceException(int httpStatus, Throwable cause, String message, Object... args) {
		this(fromStatusToCode(httpStatus), httpStatus, cause, message, args);
	}

	public SpaceException(String code, int httpStatus, String message, Object... args) {
		this(code, httpStatus, (Throwable) null, message, args);
	}

	public SpaceException(String code, int httpStatus, Throwable cause, String message, Object... args) {
		this.message = Utils.isNullOrEmpty(message) //
				? String.format("HTTP error %s", httpStatus)
				: String.format(message, args);

		if (cause != null)
			this.message = this.message + ": " + cause.getMessage();

		this.httpStatus = httpStatus;
		this.code = code;
	}

	@Override
	public String getMessage() {
		return message;
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

	public JsonNode details() {
		return details;
	}

	public SpaceException details(JsonNode details) {
		this.details = details;
		return this;
	}

	public SpaceException cause(Throwable t) {
		this.initCause(t);
		return this;
	}

	public SpaceException withHeader(String key, String value) {
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
