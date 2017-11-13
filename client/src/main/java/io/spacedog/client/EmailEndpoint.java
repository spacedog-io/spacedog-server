package io.spacedog.client;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.EmailRequest;
import io.spacedog.model.EmailTemplateRequest;

public class EmailEndpoint {

	SpaceDog dog;

	EmailEndpoint(SpaceDog dog) {
		this.dog = dog;
	}

	public ObjectNode send(EmailRequest request) {
		return dog.post("/1/emails")//
				.bodyPojo(request).go(200)//
				.asJsonObject();
	}

	public ObjectNode send(String templateName, Map<String, Object> parameters) {
		EmailTemplateRequest request = new EmailTemplateRequest();
		request.templateName = templateName;
		request.parameters = parameters;
		return send(request);
	}

}
