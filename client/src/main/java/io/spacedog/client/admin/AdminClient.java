package io.spacedog.client.admin;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.CreateCredentialsRequest;
import io.spacedog.client.http.SpaceBackend;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceParams;

public class AdminClient implements SpaceParams, SpaceFields {

	private SpaceDog dog;

	public AdminClient(SpaceDog session) {
		this.dog = session;
	}

	public SpaceDog dog() {
		return dog;
	}

	public boolean doesMyBackendExist() {
		return dog.get("/1/backends").go().status() == 200;
	}

	public SpaceDog createMyBackend(boolean notification) {
		return SpaceDog.dog().admin()//
				.createBackend(dog.backendId(), dog.username(), dog.password().get(), //
						dog.email().get(), notification);
	}

	public SpaceDog createBackend(String backendId, String username, String password, String email, //
			boolean notification) {

		CreateCredentialsRequest superadmin = new CreateCredentialsRequest()//
				.username(username).password(password).email(email);

		return createBackend(new CreateBackendRequest()//
				.backendId(backendId).superadmin(superadmin), notification);
	}

	public SpaceDog createBackend(CreateBackendRequest request, boolean notification) {

		dog.post("/1/backends")//
				.queryParam(NOTIF_PARAM, notification)//
				.bodyPojo(request)//
				.go(201);

		SpaceBackend newBackend = dog.backend().fromBackendId(request.backendId());

		return SpaceDog.dog(newBackend)//
				.username(request.superadmin().username())//
				.password(request.superadmin().password())//
				.email(request.superadmin().email());
	}

	public AdminClient deleteBackend(String backendId) {
		dog.delete("/1/backends/{id}")//
				.routeParam("id", backendId).go(200, 401);
		return this;
	}

}
