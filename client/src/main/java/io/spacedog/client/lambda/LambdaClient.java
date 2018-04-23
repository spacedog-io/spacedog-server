package io.spacedog.client.lambda;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.http.SpaceRequest;

public class LambdaClient implements SpaceParams {

	private SpaceDog dog;

	public LambdaClient(SpaceDog session) {
		this.dog = session;
	}

	public SpaceRequest getRequest(String path) {
		return dog.get("/1/services" + path);
	}

	public SpaceRequest postRequest(String path) {
		return dog.post("/1/services" + path);
	}

	public SpaceRequest putRequest(String path) {
		return dog.put("/1/services" + path);
	}

	public SpaceRequest deleteRequest(String path) {
		return dog.put("/1/services" + path);
	}
}
