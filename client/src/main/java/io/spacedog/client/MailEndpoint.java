package io.spacedog.client;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.EmailBasicRequest;

public class MailEndpoint {

	SpaceDog dog;

	MailEndpoint(SpaceDog dog) {
		this.dog = dog;
	}

	public ObjectNode send(EmailBasicRequest message) {
		return dog.post("/1/mail")//
				.bodyPojo(message).go(200).asJsonObject();
	}

	public void sendTemplate(String templateName, ObjectNode context) {
		dog.post("/1/mail/template/{name}")//
				.routeParam("name", templateName).bodyJson(context).go(200);
	}

}
