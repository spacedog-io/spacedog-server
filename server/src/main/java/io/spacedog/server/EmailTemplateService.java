/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.Map;
import java.util.Optional;

import io.spacedog.model.EmailBasicRequest;
import io.spacedog.model.EmailSettings;
import io.spacedog.model.EmailTemplate;
import io.spacedog.utils.Json;
import io.spacedog.utils.NotFoundException;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class EmailTemplateService extends SpaceService {

	//
	// Routes
	//

	@Post("/1/emails/templates/:name")
	@Post("/1/emails/templates/:name/")
	public Payload postTemplatedMail(String name, String body) {

		EmailTemplate template = getTemplate(name).orElseThrow(//
				() -> new NotFoundException("mail template [%s] not found", name));

		SpaceContext.credentials().checkRoles(template.roles);

		Map<String, Object> context = PebbleTemplating.get()//
				.createContext(template.model, Json.readMap(body));

		EmailBasicRequest message = toMessage(template, context);
		return EmailService.get().email(message);
	}

	//
	// internal public interface
	//

	Payload sendTemplatedMail(EmailTemplate template, Map<String, Object> context) {
		EmailBasicRequest message = toMessage(template, context);
		return EmailService.get().email(message);
	}

	Optional<EmailTemplate> getTemplate(String name) {

		EmailSettings settings = SettingsService.get().getAsObject(EmailSettings.class);

		if (settings.templates == null)
			return Optional.empty();

		return Optional.ofNullable(settings.templates.get(name));
	}

	//
	// Implementation
	//

	private EmailBasicRequest toMessage(EmailTemplate template, Map<String, Object> context) {

		PebbleTemplating pebble = PebbleTemplating.get();

		EmailBasicRequest message = new EmailBasicRequest();
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

	private static EmailTemplateService singleton = new EmailTemplateService();

	static EmailTemplateService get() {
		return singleton;
	}

	private EmailTemplateService() {
	}
}
