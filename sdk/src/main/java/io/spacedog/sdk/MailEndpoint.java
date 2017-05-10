package io.spacedog.sdk;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.rest.SpaceRequest;

public class MailEndpoint {

	SpaceDog dog;

	MailEndpoint(SpaceDog dog) {
		this.dog = dog;
	}

	public ObjectNode send(Message message) {
		SpaceRequest request = dog.post("/1/mail")//
				.formField("to", message.to);

		if (!Strings.isNullOrEmpty(message.from))
			request.formField("from", message.from);
		if (!Strings.isNullOrEmpty(message.subject))
			request.formField("subject", message.subject);
		if (!Strings.isNullOrEmpty(message.text))
			request.formField("text", message.text);
		if (!Strings.isNullOrEmpty(message.html))
			request.formField("html", message.html);
		if (!Strings.isNullOrEmpty(message.cc))
			request.formField("cc", message.cc);
		if (!Strings.isNullOrEmpty(message.bcc))
			request.formField("bcc", message.bcc);

		return request.go(200).asJsonObject();
	}

	public static class Message {
		public String from;
		public String to;
		public String cc;
		public String bcc;
		public String subject;
		public String text;
		public String html;
	}

	public void sendTemplate(String templateName, ObjectNode context) {
		dog.post("/1/mail/template/{name}")//
				.routeParam("name", templateName).bodyJson(context).go(200);
	}

}
