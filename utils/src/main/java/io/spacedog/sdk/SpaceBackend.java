package io.spacedog.sdk;

import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;

public class SpaceBackend implements SpaceParams, SpaceFields {

	SpaceDog dog;

	SpaceBackend(SpaceDog session) {
		this.dog = session;
	}

	public SpaceDog dog() {
		return dog;
	}

	public SpaceBackend create(String username, String password, String email, //
			boolean notification) {

		SpaceRequest request = SpaceRequest.post("/1/backend")//
				.queryParam(PARAM_NOTIF, Boolean.toString(notification))//
				.body(FIELD_USERNAME, username, FIELD_PASSWORD, password, FIELD_EMAIL, email);

		request.auth(dog);

		request.go(201);
		return this;
	}

	public SpaceBackend delete() {
		return delete(dog.backendId());
	}

	public SpaceBackend delete(String backendId) {
		SpaceRequest.delete("/1/backend").auth(backendId, dog).go(200, 401);
		return this;
	}

}
