/**
 * © David Attias 2015
 */
package io.spacedog.services.email;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.email.EmailRequest;
import io.spacedog.client.email.EmailTemplate;
import io.spacedog.server.Server;
import io.spacedog.services.JsonPayload;
import io.spacedog.services.Services;
import io.spacedog.services.SpaceResty;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/2/emails")
public class EmailResty extends SpaceResty {

	@Post("")
	@Post("/")
	public ObjectNode postEmail(EmailRequest request, Context context) {
		return Services.emails().sendIfAuthorized(request);
	}

	@Put("/templates/:name")
	@Put("/templates/:name/")
	public Payload putTemplate(String templateName, EmailTemplate template, Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		template.name = templateName;
		Services.emails().saveTemplate(template);
		return JsonPayload.saved(false) //
				.withFields("id", templateName, "type", "EmailTemplate")//
				.withLocation("/2/emails/templates/" + templateName)//
				.build();
	}

	@Get("/templates/:name")
	@Get("/templates/:name/")
	public EmailTemplate getTemplate(String templateName) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		return Services.emails().getTemplate(templateName);
	}

	@Delete("/templates/:name")
	@Delete("/templates/:name/")
	public void deleteTemplate(String templateName) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.emails().deleteTemplate(templateName);
	}

}
