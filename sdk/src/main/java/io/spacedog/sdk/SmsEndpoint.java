package io.spacedog.sdk;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.utils.Json;

public class SmsEndpoint {

	SpaceDog dog;

	SmsEndpoint(SpaceDog session) {
		this.dog = session;
	}

	public ObjectNode get(String messageId) {
		return dog.get("/1/sms/{id}")//
				.routeParam("id", messageId).go(200).asJsonObject();
	}

	public String send(String to, String message) {
		return send(null, to, message);
	}

	public String send(String from, String to, String message) {
		SpaceRequest request = dog.post("/1/sms").formField("To", to)//
				.formField("Body", message);

		if (!Strings.isNullOrEmpty(from))
			request.formField("From", from);

		return request.go(201).getString("sid");
	}

	public void sendTemplated(String templateName, Object... parameters) {
		sendTemplated(templateName, Json.object(parameters));
	}

	public void sendTemplated(String templateName, ObjectNode parameters) {
		dog.post("/1/sms/template/{name}").routeParam("name", templateName)//
				.bodyJson(parameters).go(201);
	}

}
