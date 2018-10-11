package io.spacedog.client.stripe;

import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.http.SpaceRequest;

public class StripeClient {

	private SpaceDog dog;

	public StripeClient(SpaceDog dog) {
		this.dog = dog;
	}

	public ObjectNode charge(Map<String, Object> parameters) {

		SpaceRequest request = dog.post("/2/stripe/charges");

		for (Entry<String, Object> param : parameters.entrySet())
			request.formField(param.getKey(), param.getValue().toString());

		return request.go(200).asJsonObject();
	}

	//
	// Settings
	//

	public StripeSettings settings() {
		return dog.settings().get(StripeSettings.class);
	}

	public void settings(StripeSettings settings) {
		dog.settings().save(settings);
	}

}
