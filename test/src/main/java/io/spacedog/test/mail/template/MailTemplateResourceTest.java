package io.spacedog.test.mail.template;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import io.spacedog.model.DataPermission;
import io.spacedog.model.MailSettings;
import io.spacedog.model.MailSettings.SmtpSettings;
import io.spacedog.model.MailTemplate;
import io.spacedog.model.Schema;
import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json7;

public class MailTemplateResourceTest extends SpaceTest {

	@Test
	public void sendTemplatedEmails() throws IOException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// create a schema
		Schema schema = Schema.builder("demande")//
				.acl("key", DataPermission.create)//
				.string("email").text("nom").text("prenom").text("civilite")//
				.string("cvUrl").string("tel").string("statut")//
				.object("dispos").array()//
				.date("date").time("debut").time("fin")//
				.close().build();

		test.schema().set(schema);

		// create an inscription
		ArrayNode dispos = Json7.array(//
				Json7.object("date", "2016-12-01", "debut", "12:00:00", "fin", "13:00:00"), //
				Json7.object("date", "2016-12-03", "debut", "16:00:00", "fin", "17:00:00"));

		String inscriptionId = SpaceRequest.post("/1/data/demande").backend(test)//
				.bodyJson("nom", "Pons", "prenom", "Stéphane", "email", "david@spacedog.io", //
						"civilite", "Monsieur", "tel", "0607080920", "statut", "fuzzy", //
						"cvUrl", "https://spacedog.io", "dispos", dispos)
				.go(201)//
				.getString("id");

		// set smtp provider in mail settings
		MailSettings mailSettings = new MailSettings();
		mailSettings.smtp = new SmtpSettings();
		mailSettings.smtp.host = "mail.gandi.net";
		mailSettings.smtp.startTlsRequired = false;
		mailSettings.smtp.sslOnConnect = true;
		mailSettings.smtp.login = SpaceEnv.defaultEnv().get("spacedog.test.smtp.login");
		mailSettings.smtp.password = SpaceEnv.defaultEnv().get("spacedog.test.smtp.password");

		// set the demande mail template
		mailSettings.templates = Maps.newHashMap();
		MailTemplate template = new MailTemplate();
		template.from = "attias666@gmail.com";
		template.to = Lists.newArrayList("{{demande.email}}");
		template.subject = "Demande d'inscription de {{demande.prenom}} {{demande.nom}} (M-0370)";
		template.html = Resources.toString(//
				Resources.getResource(this.getClass(), "mail.demande.inscription.pebble"), //
				Charset.forName("UTF-8"));
		template.model = Maps.newHashMap();
		template.model.put("demande", "demande");
		template.roles = Collections.singleton("key");

		mailSettings.templates.put("demande", template);

		// save mail settings
		test.settings().save(mailSettings);

		// send inscription email
		SpaceRequest.post("/1/mail/template/demande").backend(test)//
				.bodyJson("demande", inscriptionId).go(200);

		// set the confirmation mail template
		template = new MailTemplate();
		template.to = Lists.newArrayList("{{demande.email}}");
		template.subject = "Votre demande d'inscription a bien été enregistrée";
		template.html = Resources.toString(//
				Resources.getResource(this.getClass(), "mail.demande.confirmation.pebble"), //
				Charset.forName("UTF-8"));
		template.model = Maps.newHashMap();
		template.model.put("demande", "demande");
		template.roles = Collections.singleton("key");

		mailSettings.templates.put("confirmation", template);

		// set default provider in mail settings
		mailSettings.smtp = null;

		// save mail settings
		test.settings().save(mailSettings);

		// send inscription confirmation email
		SpaceRequest.post("/1/mail/template/confirmation").backend(test)//
				.bodyJson("demande", inscriptionId).go(200);
	}
}
