/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Map;

import io.spacedog.services.MailResource.Message;
import io.spacedog.utils.MailSettings;
import io.spacedog.utils.MailTemplate;
import io.spacedog.utils.NotFoundException;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class MailTemplateResource extends Resource {

	//
	// Routes
	//

	@Post("/1/mail/template/:name")
	@Post("/1/mail/template/:name/")
	public Payload postTemplatedMail(String name, String body) {

		MailSettings settings = SettingsResource.get().load(MailSettings.class);

		if (settings.templates != null) {
			MailTemplate template = settings.templates.get(name);

			if (template != null) {
				SpaceContext.getCredentials().checkRoles(template.roles);
				Message message = toMessage(template, body);
				return MailResource.get().email(SpaceContext.getCredentials(), message);
			}
		}

		throw new NotFoundException("mail template [%s] not found", name);
	}

	//
	// Implementation
	//

	private Message toMessage(MailTemplate template, String body) {

		PebbleTemplating pebble = PebbleTemplating.get();
		Map<String, Object> context = pebble.createContext(template.model, body);

		Message message = new Message();
		message.from = pebble.render("from", template.from, context);
		message.to = pebble.render("to", template.to, context);
		message.cc = pebble.render("cc", template.cc, context);
		message.bcc = pebble.render("bcc", template.bcc, context);
		message.subject = pebble.render("subject", template.subject, context);
		message.text = pebble.render("text", template.text, context);
		message.html = pebble.render("html", template.html, context);
		return message;
	}

	//
	// singleton
	//

	private static MailTemplateResource singleton = new MailTemplateResource();

	static MailTemplateResource get() {
		return singleton;
	}

	private MailTemplateResource() {
	}
}
