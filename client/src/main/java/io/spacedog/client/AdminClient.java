package io.spacedog.client;

import io.spacedog.http.SpaceFields;
import io.spacedog.http.SpaceParams;
import io.spacedog.model.CreateBackendRequest;
import io.spacedog.model.CreateCredentialsRequest;

public class AdminClient implements SpaceParams, SpaceFields {

	SpaceDog dog;

	AdminClient(SpaceDog session) {
		this.dog = session;
	}

	public SpaceDog dog() {
		return dog;
	}

	public boolean doesMyBackendExist() {
		return dog.get("/1/backends").go().status() == 200;
	}

	public AdminClient createMyBackend(boolean notification) {
		return SpaceDog.dog().admin()//
				.createBackend(dog.backendId(), dog.username(), dog.password().get(), //
						dog.email().get(), notification);
	}

	public AdminClient createBackend(String backendId, String username, String password, String email, //
			boolean notification) {

		CreateCredentialsRequest superadmin = new CreateCredentialsRequest()//
				.username(username).password(password).email(email);

		return createBackend(new CreateBackendRequest()//
				.backendId(backendId).superadmin(superadmin), notification);
	}

	public AdminClient createBackend(CreateBackendRequest request, boolean notification) {

		dog.post("/1/backends")//
				.queryParam(NOTIF_PARAM, notification)//
				.bodyPojo(request)//
				.go(201);

		return this;
	}

	public AdminClient deleteBackend(String backendId) {
		dog.delete("/1/backends/{id}")//
				.routeParam("id", backendId).backendId(backendId).go(200, 401);
		return this;
	}

}
