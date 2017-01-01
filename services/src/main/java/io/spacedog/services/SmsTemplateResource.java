/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Map;

import io.spacedog.services.SmsResource.SmsMessage;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.SmsSettings;
import io.spacedog.utils.SmsTemplate;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class SmsTemplateResource extends Resource {

	//
	// Routes
	//

	@Post("/1/sms/template/:name")
	@Post("/1/sms/template/:name/")
	public Payload postTemplatedSms(String name, String body) {

		SmsSettings settings = SettingsResource.get().load(SmsSettings.class);

		if (settings.templates != null) {
			SmsTemplate template = settings.templates.get(name);

			if (template != null) {
				SpaceContext.getCredentials().checkRoles(template.roles);
				SmsMessage message = toMessage(template, body);
				return SmsResource.get().send(message);
			}
		}

		throw new NotFoundException("sms template [%s] not found", name);
	}

	//
	// Implementation
	//

	private SmsMessage toMessage(SmsTemplate template, String body) {
		PebbleTemplating pebble = PebbleTemplating.get();
		Map<String, Object> context = pebble.createContext(template.model, body);

		SmsMessage message = new SmsMessage();
		message.from = pebble.render("from", template.from, context);
		message.to = pebble.render("to", template.to, context);
		message.body = pebble.render("body", template.body, context);
		return message;
	}

	//
	// singleton
	//

	private static SmsTemplateResource singleton = new SmsTemplateResource();

	static SmsTemplateResource get() {
		return singleton;
	}

	private SmsTemplateResource() {
	}
}
