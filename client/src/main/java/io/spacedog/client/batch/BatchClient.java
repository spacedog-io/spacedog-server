package io.spacedog.client.batch;

import java.util.List;

import com.fasterxml.jackson.databind.type.TypeFactory;

import io.spacedog.client.SpaceDog;

public class BatchClient {

	private SpaceDog dog;

	public BatchClient(SpaceDog session) {
		this.dog = session;
	}

	public List<ServiceResponse> execute(List<ServiceCall> batch, Boolean stopOnError) {
		return dog.post("/1/batch")//
				.bodyPojo(batch)//
				.queryParam("stopOnError", stopOnError)//
				.go(200)//
				.asPojo(TypeFactory.defaultInstance()//
						.constructCollectionLikeType(List.class, ServiceResponse.class));
	}

	public List<ServiceResponse> execute(List<ServiceCall> batch) {
		return execute(batch, null);
	}

}
