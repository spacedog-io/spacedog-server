package io.spacedog.client;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.SmsBasicRequest;
import io.spacedog.model.SmsRequest;
import io.spacedog.model.SmsTemplate;

public class SmsClient {

	SpaceDog dog;

	SmsClient(SpaceDog session) {
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
		return send(new SmsBasicRequest().from(from).to(to).body(message));
	}

	public String send(SmsRequest request) {
		return dog.post("/1/sms")//
				.bodyPojo(request)//
				.go(201)//
				.getString("sid");
	}

	public void saveTemplate(SmsTemplate template) {
		dog.put("/1/sms/templates/{name}")//
				.routeParam("name", template.name)//
				.bodyPojo(template).go(200, 201);
	}

	public void deleteTemplate(String templateName) {
		dog.delete("/1/sms/templates/{name}")//
				.routeParam("name", templateName)//
				.go(200);
	}

}
