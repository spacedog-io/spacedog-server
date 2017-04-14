/**
 * © David Attias 2015
 */
package io.spacedog.services;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.SmsSettings;
import io.spacedog.model.SmsSettings.TwilioSettings;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceResponse;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class SmsResource extends Resource {

	//
	// Twilio form names
	//

	private static final String TO = "To";
	private static final String FROM = "From";
	private static final String BODY = "Body";

	//
	// Routes
	//

	@Post("/1/sms")
	@Post("/1/sms/")
	public Payload post(Context context) {

		SmsSettings settings = SettingsResource.get().load(SmsSettings.class);
		Credentials credentials = SpaceContext.getCredentials();
		credentials.checkRoles(settings.rolesAllowedToSendSms);
		return send(toMessage(credentials, context));
	}

	@Get("/1/sms/:messageId")
	@Get("/1/sms/:messageId")
	public Payload get(String messageId, Context context) {

		SmsSettings settings = SettingsResource.get().load(SmsSettings.class);
		Credentials credentials = SpaceContext.getCredentials();
		credentials.checkRoles(settings.rolesAllowedToSendSms);
		return fetch(messageId);
	}

	//
	// Implementation
	//

	static class SmsMessage {
		public String from;
		public String to;
		public String body;
	}

	private SmsMessage toMessage(Credentials credentials, Context context) {
		SmsMessage message = new SmsMessage();
		message.from = context.get(FROM);
		message.to = context.get(TO);
		message.body = context.get(BODY);
		return message;
	}

	public Payload send(SmsMessage message) {

		SmsSettings settings = SettingsResource.get().load(SmsSettings.class);

		if (settings.twilio != null)
			return smsViaTwilio(message, settings.twilio);

		throw Exceptions.illegalArgument("no sms provider settings set");
	}

	private Payload smsViaTwilio(SmsMessage message, TwilioSettings twilio) {

		if (message.from == null)
			message.from = twilio.defaultFrom;

		SpaceResponse response = SpaceRequest//
				.post("/2010-04-01/Accounts/{id}/Messages.json")//
				.backend("https://api.twilio.com")//
				.routeParam("id", twilio.accountSid)//
				.basicAuth(twilio.accountSid, twilio.authToken)//
				.formField(TO, message.to)//
				.formField(FROM, message.from)//
				.formField(BODY, message.body)//
				.go();

		return toPayload(response);
	}

	public Payload fetch(String messageId) {

		SmsSettings settings = SettingsResource.get().load(SmsSettings.class);

		if (settings.twilio != null)
			return fetchFromTwilio(messageId, settings.twilio);

		throw Exceptions.illegalArgument("no sms provider settings set");
	}

	private Payload fetchFromTwilio(String messageId, TwilioSettings twilio) {

		SpaceResponse response = SpaceRequest//
				.get("/2010-04-01/Accounts/{accountId}/Messages/{messageId}.json")//
				.backend("https://api.twilio.com")//
				.routeParam("accountId", twilio.accountSid)//
				.routeParam("messageId", messageId)//
				.basicAuth(twilio.accountSid, twilio.authToken)//
				.go();

		return toPayload(response);
	}

	//
	// implementation
	//

	private Payload toPayload(SpaceResponse response) {
		ObjectNode node = response.asJsonObject();
		if (response.status() >= 400) {
			ObjectNode twilio = response.asJsonObject();
			node = JsonPayload.builder(response.status())//
					.object("error")//
					.put("code", "twilio:" + twilio.get("code").asText())//
					.put("message", twilio.get("message").asText())//
					.node("twilio", twilio)//
					.build();
		}
		return JsonPayload.json(node, response.status());
	}

	//
	// singleton
	//

	private static SmsResource singleton = new SmsResource();

	static SmsResource get() {
		return singleton;
	}

	private SmsResource() {
		SettingsResource.get().registerSettingsClass(SmsSettings.class);
	}
}
