/**
 * © David Attias 2015
 */
package io.spacedog.services;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.sms.SmsRequest;
import io.spacedog.client.sms.SmsTemplate;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

public class SmsResty extends SpaceResty {

	@Post("/1/sms")
	@Post("/1/sms/")
	public Payload postSms(SmsRequest request, Context context) {
		ObjectNode response = Services.sms().sendIfAuthorized(request);
		return JsonPayload.created().withContent(response).build();
	}

	@Get("/1/sms/:messageId")
	@Get("/1/sms/:messageId")
	public Payload getSms(String messageId, Context context) {
		Server.context().credentials().checkIfAuthorized(//
				Services.sms().settings().authorizedRoles);
		ObjectNode response = Services.sms().get(messageId);
		return JsonPayload.ok().withContent(response).build();
	}

	@Put("/1/sms/templates/:name")
	@Put("/1/sms/templates/:name/")
	public Payload putTemplate(String templateName, SmsTemplate template, Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		template.name = templateName;
		Services.sms().saveTemplate(template);
		return JsonPayload.ok().withFields(//
				"id", templateName, "type", "SmsTemplate")//
				.withLocation("/1/sms/templates/" + templateName)//
				.build();
	}

	@Get("/1/sms/templates/:name")
	@Get("/1/sms/templates/:name/")
	public Payload getTemplate(String templateName) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		SmsTemplate template = Services.sms().getTemplate(templateName);
		return JsonPayload.ok().withContent(template).build();
	}

	@Delete("/1/sms/templates/:name")
	@Delete("/1/sms/templates/:name/")
	public Payload deleteTemplate(String templateName) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.sms().deleteTemplate(templateName);
		return JsonPayload.ok().build();
	}

}
