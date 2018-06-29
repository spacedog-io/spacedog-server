/**
 * © David Attias 2015
 */
package io.spacedog.services;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.email.EmailRequest;
import io.spacedog.client.email.EmailTemplate;
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

public class EmailResty extends SpaceResty {

	@Post("/1/emails")
	@Post("/1/emails/")
	public ObjectNode postEmail(EmailRequest request, Context context) {
		return Services.emails().sendIfAuthorized(request);
	}

	@Put("/1/emails/templates/:name")
	@Put("/1/emails/templates/:name/")
	public Payload putTemplate(String templateName, EmailTemplate template, Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		template.name = templateName;
		Services.emails().saveTemplate(template);
		return JsonPayload.saved(false) //
				.withFields("id", templateName, "type", "EmailTemplate")//
				.withLocation("/1/emails/templates/" + templateName)//
				.build();
	}

	@Get("/1/emails/templates/:name")
	@Get("/1/emails/templates/:name/")
	public Payload getTemplate(String templateName) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		EmailTemplate template = Services.emails().getTemplate(templateName);
		return JsonPayload.ok().withContent(template).build();
	}

	@Delete("/1/emails/templates/:name")
	@Delete("/1/emails/templates/:name/")
	public Payload deleteTemplate(String templateName) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.emails().deleteTemplate(templateName);
		return JsonPayload.ok().build();
	}

}
