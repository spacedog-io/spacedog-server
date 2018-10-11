/**
 * © David Attias 2015
 */
package io.spacedog.services.sms;

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
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/2/sms")
public class SmsResty extends SpaceResty {

	@Post("")
	@Post("/")
	public ObjectNode postSms(SmsRequest request, Context context) {
		return Services.sms().sendIfAuthorized(request);
	}

	@Get("/:messageId")
	@Get("/:messageId")
	public ObjectNode getSms(String messageId, Context context) {
		Server.context().credentials().checkRoleAccess(//
				Services.sms().settings().authorizedRoles);
		return Services.sms().get(messageId);
	}

	@Put("/templates/:name")
	@Put("/templates/:name/")
	public Payload putTemplate(String templateName, SmsTemplate template, Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		template.name = templateName;
		Services.sms().saveTemplate(template);
		return JsonPayload.ok().withFields(//
				"id", templateName, "type", "SmsTemplate")//
				.withLocation("/2/sms/templates/" + templateName)//
				.build();
	}

	@Get("/templates/:name")
	@Get("/templates/:name/")
	public SmsTemplate getTemplate(String templateName) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		return Services.sms().getTemplate(templateName);
	}

	@Delete("/templates/:name")
	@Delete("/templates/:name/")
	public void deleteTemplate(String templateName) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.sms().deleteTemplate(templateName);
	}

}
