package io.spacedog.client.email;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;

public class EmailClient {

	private SpaceDog dog;

	public EmailClient(SpaceDog dog) {
		this.dog = dog;
	}

	public ObjectNode send(EmailRequest request) {
		return dog.post("/2/emails")//
				.bodyPojo(request).go(200)//
				.asJsonObject();
	}

	public ObjectNode send(String templateName, Map<String, Object> parameters) {
		EmailTemplateRequest request = new EmailTemplateRequest();
		request.templateName = templateName;
		request.parameters = parameters;
		return send(request);
	}

	public void saveTemplate(EmailTemplate template) {
		dog.put("/2/emails/templates/{name}")//
				.routeParam("name", template.name)//
				.bodyPojo(template)//
				.go(200, 201).asVoid();
	}

	public EmailTemplate getTemplate(String name) {
		return dog.get("/2/emails/templates/{name}")//
				.routeParam("name", name)//
				.go(200)//
				.asPojo(EmailTemplate.class);
	}

	public void deleteTemplate(String templateName) {
		dog.delete("/2/emails/templates/{name}")//
				.routeParam("name", templateName)//
				.go(200).asVoid();
	}

	//
	// Settings
	//

	public EmailSettings settings() {
		return dog.settings().get(EmailSettings.class);
	}

	public void settings(EmailSettings settings) {
		dog.settings().save(settings);
	}

}
