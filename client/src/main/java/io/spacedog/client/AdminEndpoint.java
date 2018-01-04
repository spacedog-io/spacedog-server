package io.spacedog.client;

import io.spacedog.http.SpaceFields;
import io.spacedog.http.SpaceParams;
import io.spacedog.model.CreateBackendRequest;
import io.spacedog.model.CreateCredentialsRequest;

public class AdminEndpoint implements SpaceParams, SpaceFields {

	SpaceDog dog;

	AdminEndpoint(SpaceDog session) {
		this.dog = session;
	}

	public SpaceDog dog() {
		return dog;
	}

	public boolean doesMyBackendExist() {
		return dog.get("/1/backends").go().status() == 200;
	}

	public AdminEndpoint createMyBackend(boolean notification) {
		return SpaceDog.dog().admin()//
				.createBackend(dog.backendId(), dog.username(), dog.password().get(), //
						dog.email().get(), notification);
	}

	public AdminEndpoint createBackend(String backendId, String username, String password, String email, //
			boolean notification) {

		CreateCredentialsRequest superadmin = new CreateCredentialsRequest()//
				.username(username).password(password).email(email);

		return createBackend(new CreateBackendRequest()//
				.backendId(backendId).superadmin(superadmin), notification);
	}

	public AdminEndpoint createBackend(CreateBackendRequest request, boolean notification) {

		dog.post("/1/backends")//
				.queryParam(NOTIF_PARAM, notification)//
				.bodyPojo(request)//
				.go(201);

		return this;
	}

	public AdminEndpoint deleteBackend(String backendId) {
		dog.delete("/1/backends/{id}")//
				.routeParam("id", backendId).backendId(backendId).go(200, 401);
		return this;
	}

}
