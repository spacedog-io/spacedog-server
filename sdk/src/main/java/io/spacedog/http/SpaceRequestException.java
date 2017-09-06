package io.spacedog.http;

import com.fasterxml.jackson.databind.JsonNode;

public class SpaceRequestException extends RuntimeException {

	private static final long serialVersionUID = 6368102098165046489L;

	private SpaceResponse response;

	public SpaceRequestException(SpaceResponse response) {
		super(String.format("[%s][%s] failed with status [%s] and payload [%s]", //
				response.okRequest().method(), response.okRequest().url(), //
				response.okResponse().message(), response.asString()));

		this.response = response;
	}

	public int httpStatus() {
		return response.status();
	}

	public String serverErrorCode() {
		return response.getString("error.code");
	}

	public String serverErrorMessage() {
		return response.getString("error.message");
	}

	public JsonNode serverError() {
		return response.get("error");
	}

}
