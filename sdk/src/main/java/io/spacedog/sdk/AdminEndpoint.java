package io.spacedog.sdk;

import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;

public class AdminEndpoint implements SpaceParams, SpaceFields {

	SpaceDog dog;

	AdminEndpoint(SpaceDog session) {
		this.dog = session;
	}

	public SpaceDog dog() {
		return dog;
	}

	public boolean doesMyBackendExist() {
		return dog.get("/1/backend").go().status() == 200;
	}

	public AdminEndpoint createMyBackend(boolean notification) {
		return SpaceDog.backend(dog).admin()//
				.createBackend(dog.username(), dog.password().get(), //
						dog.email().get(), notification);

	}

	public AdminEndpoint createBackend(String username, String password, String email, //
			boolean notification) {

		dog.post("/1/backend")//
				.queryParam(PARAM_NOTIF, Boolean.toString(notification))//
				.bodyJson(FIELD_USERNAME, username, FIELD_PASSWORD, password, FIELD_EMAIL, email)//
				.go(201);

		return this;
	}

	public AdminEndpoint deleteBackend(String backendId) {
		dog.delete("/1/backend").backend(backendId).go(200, 401);
		return this;
	}

}
