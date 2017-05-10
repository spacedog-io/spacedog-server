package io.spacedog.sdk;

import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.rest.SpaceRequest;

public class StripeEndpoint {

	SpaceDog dog;

	StripeEndpoint(SpaceDog dog) {
		this.dog = dog;
	}

	public ObjectNode charge(Map<String, Object> parameters) {

		SpaceRequest request = dog.post("/1/stripe/charges");

		for (Entry<String, Object> param : parameters.entrySet())
			request.formField(param.getKey(), param.getValue().toString());

		return request.go(200).asJsonObject();
	}
}
