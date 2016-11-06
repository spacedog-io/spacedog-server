package io.spacedog.examples;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.loader.StringLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceRequest;
import io.spacedog.services.MailHandlebarsResource;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Json;
import io.spacedog.utils.MailTemplate;
import io.spacedog.utils.MailTemplate.Reference;
import io.spacedog.utils.Schema;

public class ColibeeMailTemplate {

	@Test
	public void sendTemplatedEmails() throws IOException {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// create a schema
		Schema schema = Schema.builder("demande")//
				.acl("key", DataPermission.create)//
				.string("email").text("nom").text("prenom").text("civilite")//
				.string("cvUrl").string("tel").string("statut")//
				.object("dispos").array()//
				.date("date").time("debut").time("fin")//
				.close().build();

		SpaceClient.setSchema(schema, test);

		// create an inscription
		ArrayNode dispos = Json.array(//
				Json.object("date", "2016-12-01", "debut", "12:00:00", "fin", "13:00:00"), //
				Json.object("date", "2016-12-03", "debut", "16:00:00", "fin", "17:00:00"));

		String inscriptionId = SpaceRequest.post("/1/data/demande").backend(test)//
				.body("nom", "Pons", "prenom", "Pilate", "email", "attias666@gmail.com", //
						"civilite", "Monsieur", "tel", "0607080920", "statut", "fuzzy", //
						"cvUrl", "https://spacedog.io", "dispos", dispos)
				.go(201)//
				.getString("id");

		// set the demande mail template
		MailTemplate template = new MailTemplate();
		template.to = Lists.newArrayList("attias666@gmail.com");
		template.subject = "Nouvelle demande d'inscription";
		template.text = Resources.toString(
				Resources.getResource("io/spacedog/examples/colibee.mail.demande.inscription.hbs"), //
				Charset.forName("UTF-8"));
		template.parameters = Maps.newHashMap();
		template.parameters.put("demandeId", "string");
		template.references = Maps.newHashMap();
		template.references.put("demande", new Reference("demande", "{{demandeId}}"));

		SpaceRequest.put("/1/mail/template/demande").adminAuth(test)//
				.body(Json.mapper().writeValueAsString(template)).go(200);

		// send inscription email
		SpaceRequest.post("/1/mail/template/demande").backend(test)//
				.body("demandeId", inscriptionId).go(200);

		// set the confirmation mail template
		template = new MailTemplate();
		template.to = Lists.newArrayList("{{demande.email}}");
		template.subject = "Confirmation de votre de demande d'inscription";
		template.text = Resources.toString(
				Resources.getResource("io/spacedog/examples/colibee.mail.demande.confirmation.hbs"), //
				Charset.forName("UTF-8"));
		template.parameters = Maps.newHashMap();
		template.parameters.put("demandeId", "string");
		template.references = Maps.newHashMap();
		template.references.put("demande", new Reference("demande", "{{demandeId}}"));

		SpaceRequest.put("/1/mail/template/confirmation").adminAuth(test)//
				.body(Json.mapper().writeValueAsString(template)).go(200);

		// send inscription confirmation email
		SpaceRequest.post("/1/mail/template/confirmation").backend(test)//
				.body("demandeId", inscriptionId).go(200);

	}

	@Test
	public void testHandlebars() throws IOException {

		Handlebars handlebars = new Handlebars();
		Context context = MailHandlebarsResource.toContext(Json.object("demande", Json.object("sexe", "Monsieur")));
		System.out.println(handlebars.compileInline("{{demande.sexe}}").apply(context));
	}

	@Test
	public void testPebble() throws PebbleException, IOException {

		PebbleEngine engine = new PebbleEngine.Builder().loader(new StringLoader()).build();
		PebbleTemplate compiledTemplate = engine.getTemplate("{{demande.sexe}}");

		Map<String, Object> demande = Maps.newHashMap();
		demande.put("sexe", "Monsieur");
		Map<String, Object> context = new HashMap<>();
		context.put("demande", demande);

		compiledTemplate.evaluate(new OutputStreamWriter(System.out), context);
	}

}
