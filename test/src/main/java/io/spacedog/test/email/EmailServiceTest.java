package io.spacedog.test.email;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceEnv;
import io.spacedog.model.EmailBasicRequest;
import io.spacedog.model.EmailSettings;
import io.spacedog.model.EmailSettings.SmtpSettings;
import io.spacedog.model.EmailTemplate;
import io.spacedog.model.Permission;
import io.spacedog.model.Schema;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class EmailServiceTest extends SpaceTest {

	private static final String DEFAULT_FROM = "david@spacedog.io";
	private static final String DEFAULT_TO = "platform@spacedog.io";
	private static final String DEFAULT_TEXT = "∆ hello ∆";
	private static final String DEFAULT_SUBJECT = "SpaceDog Email Test";

	@Test
	public void sendBasicEmails() throws IOException {

		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");

		// by default only superadmins can send emails
		assertHttpError(403, () -> vince.emails().send(defaultMail()));

		// superadmin emails a simple text message
		superadmin.emails().send(defaultMail());

		// superadmin allows users to send emails
		EmailSettings settings = new EmailSettings();
		settings.authorizedRoles = Sets.newHashSet("user");
		superadmin.settings().save(settings);

		// now vince can email a simple html message
		EmailBasicRequest mail = defaultMail();
		mail.html = "<html><h1>" + DEFAULT_TEXT + "</h1></html>";
		vince.emails().send(mail);

		// vince can email with html without ending tags
		mail.html = "<html><h1>" + DEFAULT_TEXT + "</h1>";
		vince.emails().send(mail);

		// vince fails to email since no 'to' field
		assertHttpError(400, () -> vince.emails().send(defaultMail().to(null)));

		// superadmin sets specific mailgun settings with invalid key
		settings.mailgun = new EmailSettings.MailGunSettings();
		settings.mailgun.domain = "api.spacedog.io";
		settings.mailgun.key = "123456789";
		superadmin.settings().save(settings);

		// superadmin fails to email since mailgun key is invalid
		assertHttpError(401, () -> superadmin.emails().send(defaultMail()));

		// superadmin sets smtp settings
		settings.mailgun = null;
		settings.smtp = smtpSettings();
		superadmin.settings().save(settings);

		// vince emails a text message via smtp
		ObjectNode response = vince.emails().send(defaultMail());
		assertNotNull(response.get("messageId").asText());

		// vince emails an html message via smtp
		mail = defaultMail();
		mail.html = Resources.toString(//
				Resources.getResource(this.getClass(), "email.html"), //
				Utils.UTF8);
		response = vince.emails().send(mail);
		assertNotNull(response.get("messageId").asText());
	}

	private EmailBasicRequest defaultMail() {
		EmailBasicRequest mail = new EmailBasicRequest();
		mail.from = DEFAULT_FROM;
		mail.to = Lists.newArrayList(DEFAULT_TO);
		mail.text = DEFAULT_TEXT;
		mail.subject = DEFAULT_SUBJECT;
		return mail;
	}

	private SmtpSettings smtpSettings() {
		SmtpSettings settings = new SmtpSettings();
		settings.host = "mail.gandi.net";
		settings.startTlsRequired = false;
		settings.sslOnConnect = true;
		settings.login = SpaceEnv.defaultEnv().getOrElseThrow("spacedog.test.smtp.login");
		settings.password = SpaceEnv.defaultEnv().getOrElseThrow("spacedog.test.smtp.password");
		return settings;
	}

	@Test
	public void sendTemplatedEmails() throws IOException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin.backend());
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog nath = createTempDog(superadmin, "nath", "emailboss");

		// create a schema
		Schema schema = Schema.builder("demande")//
				.acl("all", Permission.create)//
				.string("email").text("nom").text("prenom").text("civilite")//
				.string("cvUrl").string("tel").string("statut")//
				.object("dispos").array()//
				.date("date").time("debut").time("fin")//
				.close().build();

		superadmin.schema().set(schema);

		// create an inscription
		ArrayNode dispos = Json.array(//
				Json.object("date", "2016-12-01", "debut", "12:00:00", "fin", "13:00:00"), //
				Json.object("date", "2016-12-03", "debut", "16:00:00", "fin", "17:00:00"));

		String inscriptionId = guest.data().save("demande", //
				Json.object("nom", "Pons", "prenom", "Stéphane", "email", "david@spacedog.io", //
						"civilite", "Monsieur", "tel", "0607080920", "statut", "fuzzy", //
						"cvUrl", "https://spacedog.io", "dispos", dispos))//
				.id();

		// set smtp provider in mail settings
		EmailSettings mailSettings = new EmailSettings();
		mailSettings.smtp = smtpSettings();

		// set the demande mail template
		mailSettings.templates = Maps.newHashMap();
		EmailTemplate template = new EmailTemplate();
		template.from = "attias666@gmail.com";
		template.to = Lists.newArrayList("{{demande.email}}");
		template.subject = "Demande d'inscription de {{demande.prenom}} {{demande.nom}} (M-0370)";
		template.html = Resources.toString(//
				Resources.getResource(this.getClass(), "mail.demande.inscription.pebble"), //
				Charset.forName("UTF-8"));
		template.model = Maps.newHashMap();
		template.model.put("demande", "demande");
		template.roles = Collections.singleton("emailboss");

		mailSettings.templates.put("demande", template);

		// save mail settings
		superadmin.settings().save(mailSettings);

		// send inscription email
		Map<String, Object> parameters = Maps.newHashMap();
		parameters.put("demande", inscriptionId);

		// guests are not allowed to send this templated email
		assertHttpError(403, () -> guest.emails().send("demande", parameters));

		// vince is not allowed to send this templated email
		assertHttpError(403, () -> vince.emails().send("demande", parameters));

		// nath's got the emailboss role
		// she is allowed to send this templated email
		nath.emails().send("demande", parameters);
	}
}
