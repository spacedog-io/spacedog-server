package io.spacedog.utils;

import io.spacedog.http.SpaceException;

public class NotFoundException extends SpaceException {

	private static final long serialVersionUID = 5495027748335056224L;

	public NotFoundException(String message, Object... args) {
		super(404, message, args);
	}

	public static NotFoundException type(String type) {
		return new NotFoundException("no object of type [%s] found", type);
	}

	public static NotFoundException object(String type, String objectId) {
		return new NotFoundException("object [%s] of type [%s] not found", objectId, type);
	}

	public static NotFoundException backend(String backendId) {
		return new NotFoundException("backend [%s] not found", backendId);
	}

	public static NotFoundException snapshot(String snapshotId) {
		return new NotFoundException("snapshot [%s] not found", snapshotId);
	}
}