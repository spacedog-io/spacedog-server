package io.spacedog.services;

@SuppressWarnings("serial")
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}

	public static NotFoundException type(String type) {
		return new NotFoundException(String.format("object type [%s] not found", type));
	}

	public static NotFoundException object(String type, String objectId) {
		return new NotFoundException(String.format("[%] object with id [%s] not found", type, objectId));
	}

	public static NotFoundException backend(String backendId) {
		return new NotFoundException(String.format("backend with id [%s] not found", backendId));
	}

	public static NotFoundException snapshot(String snapshotId) {
		return new NotFoundException(String.format("snapshot with id [%s] not found", snapshotId));
	}
}