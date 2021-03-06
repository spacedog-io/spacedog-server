package io.spacedog.client.sms;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;

public class SmsClient {

	private SpaceDog dog;

	public SmsClient(SpaceDog session) {
		this.dog = session;
	}

	public ObjectNode get(String messageId) {
		return dog.get("/2/sms/{id}")//
				.routeParam("id", messageId).go(200).asJsonObject();
	}

	public String send(String to, String message) {
		return send(null, to, message);
	}

	public String send(String from, String to, String message) {
		return send(new SmsBasicRequest().from(from).to(to).body(message));
	}

	public String send(SmsRequest request) {
		return dog.post("/2/sms")//
				.bodyPojo(request)//
				.go(200)//
				.getString("sid");
	}

	public void saveTemplate(SmsTemplate template) {
		dog.put("/2/sms/templates/{name}")//
				.routeParam("name", template.name)//
				.bodyPojo(template).go(200, 201).asVoid();
	}

	public SmsTemplate getTemplate(String name) {
		return dog.get("/2/sms/templates/{name}")//
				.routeParam("name", name)//
				.go(200)//
				.asPojo(SmsTemplate.class);
	}

	public void deleteTemplate(String templateName) {
		dog.delete("/2/sms/templates/{name}")//
				.routeParam("name", templateName)//
				.go(200).asVoid();
	}

	//
	// Settings
	//

	public SmsSettings settings() {
		return dog.settings().get(SmsSettings.class);
	}

	public void settings(SmsSettings settings) {
		dog.settings().save(settings);
	}

}
