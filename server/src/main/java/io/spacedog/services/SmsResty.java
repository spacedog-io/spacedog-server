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
	public ObjectNode postSms(SmsRequest request, Context context) {
		return Services.sms().sendIfAuthorized(request);
	}

	@Get("/1/sms/:messageId")
	@Get("/1/sms/:messageId")
	public ObjectNode getSms(String messageId, Context context) {
		Server.context().credentials().checkIfAuthorized(//
				Services.sms().settings().authorizedRoles);
		return Services.sms().get(messageId);
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
	public SmsTemplate getTemplate(String templateName) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		return Services.sms().getTemplate(templateName);
	}

	@Delete("/1/sms/templates/:name")
	@Delete("/1/sms/templates/:name/")
	public void deleteTemplate(String templateName) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.sms().deleteTemplate(templateName);
	}

}
