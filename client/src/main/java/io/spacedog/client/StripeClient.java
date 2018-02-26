package io.spacedog.client;

import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.http.SpaceRequest;

public class StripeClient {

	SpaceDog dog;

	StripeClient(SpaceDog dog) {
		this.dog = dog;
	}

	public ObjectNode charge(Map<String, Object> parameters) {

		SpaceRequest request = dog.post("/1/stripe/charges");

		for (Entry<String, Object> param : parameters.entrySet())
			request.formField(param.getKey(), param.getValue().toString());

		return request.go(200).asJsonObject();
	}
}
