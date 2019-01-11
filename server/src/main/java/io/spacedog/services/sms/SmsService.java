package io.spacedog.services.sms;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.http.SpaceException;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.client.sms.SmsBasicRequest;
import io.spacedog.client.sms.SmsRequest;
import io.spacedog.client.sms.SmsSettings;
import io.spacedog.client.sms.SmsSettings.TwilioSettings;
import io.spacedog.client.sms.SmsTemplate;
import io.spacedog.client.sms.SmsTemplateRequest;
import io.spacedog.server.Server;
import io.spacedog.services.PebbleTemplating;
import io.spacedog.services.Services;
import io.spacedog.utils.Exceptions;

public class SmsService {

	public ObjectNode get(String messageId) {

		SmsSettings settings = settings();
		if (settings.twilio != null)
			return getFromTwilio(messageId, settings.twilio);

		throw Exceptions.illegalArgument("no sms provider settings set");
	}

	public ObjectNode send(String to, String message) {
		return send(null, to, message);
	}

	public ObjectNode send(String from, String to, String message) {
		return send(new SmsBasicRequest().from(from).to(to).body(message));
	}

	public ObjectNode sendIfAuthorized(SmsRequest request) {

		if (request instanceof SmsBasicRequest)
			return sendIfAuthorized((SmsBasicRequest) request);

		if (request instanceof SmsTemplateRequest)
			return sendIfAuthorized((SmsTemplateRequest) request);

		throw Exceptions.illegalArgument("sms request [%s] is invalid", //
				request.getClass().getSimpleName());
	}

	private ObjectNode sendIfAuthorized(SmsTemplateRequest request) {
		SmsTemplate template = getTemplate(request.templateName);
		Server.context().credentials().checkRoleAccess(template.roles);
		return send(request, template);
	}

	private ObjectNode sendIfAuthorized(SmsBasicRequest request) {
		Server.context().credentials().checkRoleAccess(//
				Services.sms().settings().authorizedRoles);
		return send(request);
	}

	public ObjectNode send(SmsBasicRequest request) {
		SmsSettings settings = settings();
		if (settings.twilio != null)
			return smsViaTwilio(request, settings.twilio);
		throw Exceptions.illegalArgument("no sms provider settings set");
	}

	public ObjectNode send(SmsTemplateRequest request) {
		SmsTemplate template = getTemplate(request.templateName);
		return send(request, template);
	}

	public ObjectNode send(SmsTemplateRequest request, SmsTemplate template) {
		return send(toBasicRequest(request, template));
	}

	public void saveTemplate(SmsTemplate template) {
		Services.settings().save(toSettingsId(template.name), template);
	}

	public SmsTemplate getTemplate(String name) {
		return Services.settings().get(toSettingsId(name), SmsTemplate.class)//
				.orElseThrow(() -> Exceptions.objectNotFound("SmsTemplate", name));
	}

	public void deleteTemplate(String name) {
		Services.settings().delete(toSettingsId(name));
	}

	public SmsSettings settings() {
		return Services.settings().getOrThrow(SmsSettings.class);
	}

	//
	// Implementation
	//

	private String toSettingsId(String templateName) {
		return "internal-sms-template-" + templateName;
	}

	private SmsBasicRequest toBasicRequest(SmsTemplateRequest request, SmsTemplate template) {
		PebbleTemplating pebble = PebbleTemplating.get();
		Map<String, Object> context = pebble.createContext(template.model, request.parameters);

		SmsBasicRequest message = new SmsBasicRequest();
		message.from = pebble.render("from", template.from, context);
		message.to = pebble.render("to", template.to, context);
		message.body = pebble.render("body", template.body, context);
		return message;
	}

	//
	// Twilio
	//

	private ObjectNode getFromTwilio(String messageId, TwilioSettings twilio) {

		SpaceResponse response = SpaceRequest//
				.get("/2010-04-01/Accounts/{accountId}/Messages/{messageId}.json")//
				.backend("https://api.twilio.com")//
				.routeParam("accountId", twilio.accountSid)//
				.routeParam("messageId", messageId)//
				.basicAuth(twilio.accountSid, twilio.authToken)//
				.go();

		return handleTwilioResponse(response);
	}

	private ObjectNode smsViaTwilio(SmsBasicRequest request, TwilioSettings twilio) {

		if (request.from == null)
			request.from = twilio.defaultFrom;

		SpaceResponse response = SpaceRequest//
				.post("/2010-04-01/Accounts/{id}/Messages.json")//
				.backend("https://api.twilio.com")//
				.routeParam("id", twilio.accountSid)//
				.basicAuth(twilio.accountSid, twilio.authToken)//
				.formField("To", request.to)//
				.formField("From", request.from)//
				.formField("Body", request.body)//
				.go();

		return handleTwilioResponse(response);
	}

	private ObjectNode handleTwilioResponse(SpaceResponse response) {
		ObjectNode twilio = response.asJsonObject();

		if (response.status() >= 400)
			throw new SpaceException("twilio:" + twilio.get("code").asText(), //
					response.status(), twilio.get("message").asText())//
							.withDetails(twilio);

		return twilio;
	}

}
