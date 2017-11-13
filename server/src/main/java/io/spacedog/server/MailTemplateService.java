/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.Map;
import java.util.Optional;

import io.spacedog.model.Mail;
import io.spacedog.model.MailSettings;
import io.spacedog.model.MailTemplate;
import io.spacedog.utils.Json;
import io.spacedog.utils.NotFoundException;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class MailTemplateService extends SpaceService {

	//
	// Routes
	//

	@Post("/1/mail/template/:name")
	@Post("/1/mail/template/:name/")
	public Payload postTemplatedMail(String name, String body) {

		MailTemplate template = getTemplate(name).orElseThrow(//
				() -> new NotFoundException("mail template [%s] not found", name));

		SpaceContext.credentials().checkRoles(template.roles);

		Map<String, Object> context = PebbleTemplating.get()//
				.createContext(template.model, Json.readMap(body));

		Mail message = toMessage(template, context);
		return MailService.get().email(message);
	}

	//
	// internal public interface
	//

	Payload sendTemplatedMail(MailTemplate template, Map<String, Object> context) {
		Mail message = toMessage(template, context);
		return MailService.get().email(message);
	}

	Optional<MailTemplate> getTemplate(String name) {

		MailSettings settings = SettingsService.get().getAsObject(MailSettings.class);

		if (settings.templates == null)
			return Optional.empty();

		return Optional.ofNullable(settings.templates.get(name));
	}

	//
	// Implementation
	//

	private Mail toMessage(MailTemplate template, Map<String, Object> context) {

		PebbleTemplating pebble = PebbleTemplating.get();

		Mail message = new Mail();
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

	private static MailTemplateService singleton = new MailTemplateService();

	static MailTemplateService get() {
		return singleton;
	}

	private MailTemplateService() {
	}
}
