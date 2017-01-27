package io.spacedog.sdk;

import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;

public class SpaceStripe {

	SpaceDog dog;

	SpaceStripe(SpaceDog dog) {
		this.dog = dog;
	}

	public ObjectNode charge(Map<String, Object> parameters) {

		SpaceRequest request = SpaceRequest.post("/1/stripe/charges").auth(dog);

		for (Entry<String, Object> param : parameters.entrySet())
			request.formField(param.getKey(), param.getValue().toString());

		return request.go(200).objectNode();
	}
}
