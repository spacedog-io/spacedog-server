package io.spacedog.client.bulk;

import java.util.List;

import com.fasterxml.jackson.databind.type.TypeFactory;

import io.spacedog.client.SpaceDog;

public class BulkClient {

	private SpaceDog dog;

	public BulkClient(SpaceDog session) {
		this.dog = session;
	}

	public List<ServiceResponse> execute(List<ServiceCall> calls, Boolean stopOnError) {
		return dog.post("/2/bulk")//
				.bodyPojo(calls)//
				.queryParam("stopOnError", stopOnError)//
				.go(200)//
				.asPojo(TypeFactory.defaultInstance()//
						.constructCollectionLikeType(List.class, ServiceResponse.class));
	}

	public List<ServiceResponse> execute(List<ServiceCall> calls) {
		return execute(calls, null);
	}

}
