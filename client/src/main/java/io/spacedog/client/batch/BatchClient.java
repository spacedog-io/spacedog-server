package io.spacedog.client.batch;

import java.util.List;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.http.SpaceResponse;

public class BatchClient {

	private SpaceDog dog;

	public BatchClient(SpaceDog session) {
		this.dog = session;
	}

	public SpaceResponse execute(List<ServiceCall> batch, Boolean stopOnError) {
		return dog.post("/1/batch")//
				.bodyPojo(batch)//
				.queryParam("stopOnError", stopOnError)//
				.go(200);
	}

	public SpaceResponse execute(List<ServiceCall> batch) {
		return execute(batch, null);
	}

}
