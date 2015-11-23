package io.spacedog.services;

@SuppressWarnings("serial")
public class NotFoundException extends RuntimeException {

	public NotFoundException(String backendId, String type) {
		super(String.format("object type [%s] not found in backend [%s]", type, backendId));
	}

	public NotFoundException(String backendId) {
		super(String.format("backend [%s] not found", backendId));
	}

	public NotFoundException(String backendId, String type, String objectId) {
		super(String.format("object of type [%s] with id [%s] not found in backend [%s]", type, objectId, backendId));
	}

}