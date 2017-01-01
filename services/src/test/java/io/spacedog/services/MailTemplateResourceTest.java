package io.spacedog.services;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Json;
import io.spacedog.utils.MailSettings;
import io.spacedog.utils.MailTemplate;
import io.spacedog.utils.Schema;

public class MailTemplateResourceTest extends Assert {

	@Test
	public void sendTemplatedEmails() throws IOException {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		MailSettings mailSettings = new MailSettings();
		mailSettings.templates = Maps.newHashMap();

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
		template.subject = "Demande d'inscription de {{demande.prenom}} {{demande.nom}} (M-0370)";
		template.text = Resources.toString(
				Resources.getResource("io/spacedog/services/pebble/colibee.mail.demande.inscription.pebble"), //
				Charset.forName("UTF-8"));
		template.model = Maps.newHashMap();
		template.model.put("demande", "demande");
		template.roles = Collections.singleton("key");

		mailSettings.templates.put("demande", template);
		SpaceClient.saveSettings(test, mailSettings);

		// send inscription email
		SpaceRequest.post("/1/mail/template/demande").backend(test)//
				.body("demande", inscriptionId).go(200);

		// set the confirmation mail template
		template = new MailTemplate();
		template.to = Lists.newArrayList("{{demande.email}}");
		template.subject = "Votre demande d'inscription a bien été enregistrée";
		template.html = Resources.toString(
				Resources.getResource("io/spacedog/services/pebble/colibee.mail.demande.confirmation.pebble"), //
				Charset.forName("UTF-8"));
		template.model = Maps.newHashMap();
		template.model.put("demande", "demande");
		template.roles = Collections.singleton("key");

		mailSettings.templates.put("confirmation", template);
		SpaceClient.saveSettings(test, mailSettings);

		// send inscription confirmation email
		SpaceRequest.post("/1/mail/template/confirmation").backend(test)//
				.body("demande", inscriptionId).go(200);

	}
}
