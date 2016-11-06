/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.beust.jcommander.internal.Maps;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.loader.StringLoader;

import io.spacedog.services.MailResource.Message;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.MailSettings;
import io.spacedog.utils.MailTemplate;
import io.spacedog.utils.MailTemplate.Reference;
import io.spacedog.utils.NotFoundException;
import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

public class MailPebbleResource extends Resource {

	private PebbleEngine pebble;

	//
	// Routes
	//

	@Put("/1/mail/template/:name")
	@Put("/1/mail/template/:name/")
	public Payload putTemplate(String name, String body, Context context) {

		SpaceContext.checkAdminCredentials();

		MailSettings settings = SettingsResource.get().load(MailSettings.class);
		if (settings.templates == null)
			settings.templates = Maps.newHashMap();

		try {
			MailTemplate template = Json.mapper().readValue(body, MailTemplate.class);
			settings.templates.put(name, template);
		} catch (IOException e) {
			throw Exceptions.illegalArgument(e, "invalid [%s] mail template", name);
		}

		SettingsResource.get().save(settings);

		return JsonPayload.success();
	}

	@Post("/1/mail/template/:name")
	@Post("/1/mail/template/:name/")
	public Payload postTemplatedMail(String name, String body, Context context) {

		MailSettings settings = SettingsResource.get().load(MailSettings.class);

		if (settings.templates != null) {
			MailTemplate template = settings.templates.get(name);

			if (template != null) {

				Map<String, Object> model = checkParameters(template.parameters, body);
				model = loadDataObjects(model, template.references);
				Message message = toMessage(template, model);
				return MailResource.get().email(SpaceContext.getCredentials(), message);
			}
		}

		throw new NotFoundException("mail template [%s] not found", name);
	}

	//
	// Implementation
	//

	@SuppressWarnings("unchecked")
	private Map<String, Object> checkParameters(Map<String, String> parameters, String body) {

		// TODO read and check all parameters from body
		try {
			return Json.mapper().readValue(body, Map.class);
		} catch (IOException e) {
			throw Exceptions.illegalArgument(e, "error deserializing request payload");
		}
	}

	private Map<String, Object> loadDataObjects(Map<String, Object> context, //
			Map<String, Reference> references) {

		String backendId = SpaceContext.backendId();
		for (Entry<String, Reference> entry : references.entrySet()) {
			Reference reference = entry.getValue();
			reference.type = render("reference.type", reference.type, context);
			reference.id = render("reference.id", reference.id, context);
			ObjectNode object = DataStore.get().getObject(backendId, reference.type, reference.id);
			context.put(entry.getKey(), Json.mapper().convertValue(object, Map.class));
		}
		return context;
	}

	private Message toMessage(MailTemplate template, Map<String, Object> context) {

		Message message = new Message();
		message.from = render("from", template.from, context);
		message.to = render("to", template.to, context);
		message.cc = render("cc", template.cc, context);
		message.bcc = render("bcc", template.bcc, context);
		message.subject = render("subject", template.subject, context);
		message.text = render("text", template.text, context);
		return message;
	}

	private List<String> render(String propertyName, List<String> propertyValue, Map<String, Object> context) {

		if (propertyValue == null)
			return null;

		return propertyValue.stream()//
				.map(value -> render(propertyName, value, context))//
				.collect(Collectors.toList());
	}

	private String render(String propertyName, String propertyValue, Map<String, Object> context) {

		if (propertyValue == null)
			return null;

		try {
			StringWriter writer = new StringWriter();
			pebble.getTemplate(propertyValue).evaluate(writer, context);
			return writer.toString();

		} catch (IOException | PebbleException e) {
			throw Exceptions.illegalArgument(e, //
					"error rendering [%s] mail template property", propertyName);
		}

	}

	//
	// singleton
	//

	private static MailPebbleResource singleton = new MailPebbleResource();

	static MailPebbleResource get() {
		return singleton;
	}

	private MailPebbleResource() {
		pebble = new PebbleEngine.Builder().loader(new StringLoader()).build();
	}
}
