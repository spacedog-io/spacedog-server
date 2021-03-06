package io.spacedog.client.admin;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.CredentialsCreateRequest;
import io.spacedog.client.http.SpaceBackend;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.utils.Exceptions;

public class AdminClient implements SpaceParams {

	private SpaceDog dog;

	public AdminClient(SpaceDog session) {
		this.dog = session;
	}

	public boolean checkMyBackendExists() {
		// TODO test this
		// status 200 means backend exists
		// status 404 means the opposite
		// any other status throws an exception
		return dog.get("/").go(200, 404).asVoid().status() == 200;
	}

	public SpaceDog createBackend(String backendId, String username, String password, String email, //
			boolean notification) {

		CredentialsCreateRequest superadmin = new CredentialsCreateRequest()//
				.username(username).password(password).email(email);

		return createBackend(new BackendCreateRequest()//
				.backendId(backendId).superadmin(superadmin), notification);
	}

	public void clearBackend() {
		dog.post("/2/admin/_clear").go(200).close();
	}

	public SpaceDog createBackend(BackendCreateRequest request, boolean notification) {

		dog.post("/2/backends")//
				.queryParam(NOTIF_PARAM, notification)//
				.bodyPojo(request)//
				.go(201)//
				.asVoid();

		SpaceBackend newBackend = dog.backend().fromBackendId(request.backendId());

		return SpaceDog.dog(newBackend)//
				.username(request.superadmin().username())//
				.password(request.superadmin().password());
	}

	public void deleteBackend(String backendId) {
		if (dog.password().isPresent())
			deleteBackend(backendId, dog.password().get());
		else
			throw Exceptions.illegalArgument("password not set");
	}

	public void deleteBackend(String backendId, String password) {
		SpaceRequest.delete("/2/backends/{id}")//
				.backend(dog)//
				// password must be challenged
				.basicAuth(dog.username(), password)//
				.routeParam("id", backendId)//
				.go(200)//
				.asVoid();
	}

}
