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
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.email.EmailBasicRequest;
import io.spacedog.client.email.EmailSettings;
import io.spacedog.client.email.EmailTemplate;
import io.spacedog.client.email.EmailSettings.SmtpSettings;
import io.spacedog.client.http.SpaceEnv;
import io.spacedog.client.schema.Schema;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class EmailServiceTest extends SpaceTest {

	private static final String DEFAULT_FROM = "david@spacedog.io";
	private static final String DEFAULT_TO = "platform@spacedog.io";
	private static final String DEFAULT_TEXT = "∆ hello ∆";
	private static final String DEFAULT_SUBJECT = "SpaceDog Email Test";

	@Test
	public void sendEmailBasicRequests() throws IOException {

		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");

		// by default nobody is authorized to send emails
		assertHttpError(403, () -> guest.emails().send(defaultMail()));
		assertHttpError(403, () -> vince.emails().send(defaultMail()));
		assertHttpError(403, () -> superadmin.emails().send(defaultMail()));

		// superadmin allows users with 'email' role to send emails
		EmailSettings settings = new EmailSettings();
		settings.authorizedRoles = Sets.newHashSet("email");
		superadmin.settings().save(settings);

		// since nobody has 'email' role
		// nobody is authorized to send emails
		assertHttpError(403, () -> guest.emails().send(defaultMail()));
		assertHttpError(403, () -> vince.emails().send(defaultMail()));
		assertHttpError(403, () -> superadmin.emails().send(defaultMail()));

		// superadmin adds 'email' role to vince
		superadmin.credentials().setRole(vince.id(), "email");

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
		assertHttpError(401, () -> vince.emails().send(defaultMail()));

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
		settings.login = SpaceEnv.env().getOrElseThrow("spacedog.test.smtp.login");
		settings.password = SpaceEnv.env().getOrElseThrow("spacedog.test.smtp.password");
		return settings;
	}

	@Test
	public void sendEmailTemplateRequests() throws IOException {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog nath = createTempDog(superadmin, "nath", "emailboss");

		// create a schema
		Schema schema = Schema.builder("demande")//
				.acl(Roles.all, Permission.create)//
				.string("email").text("nom").text("prenom").text("civilite")//
				.string("cvUrl").string("tel").string("statut")//
				.object("dispos").array()//
				.date("date").time("debut").time("fin")//
				.close().build();

		superadmin.schemas().set(schema);

		// create an inscription
		ArrayNode dispos = Json.array(//
				Json.object("date", "2016-12-01", "debut", "12:00:00", "fin", "13:00:00"), //
				Json.object("date", "2016-12-03", "debut", "16:00:00", "fin", "17:00:00"));

		String inscriptionId = guest.data().save("demande", //
				Json.object("nom", "Pons", "prenom", "Stéphane", "email", "David <david@spacedog.io>", //
						"civilite", "Monsieur", "tel", "0607080920", "statut", "fuzzy", //
						"cvUrl", "https://spacedog.io", "dispos", dispos))//
				.id();

		// superadmin saves the demande email template
		EmailTemplate template = new EmailTemplate();
		template.name = "demande";
		template.from = "attias666@gmail.com";
		template.to = Lists.newArrayList("{{demande.email|raw}}");
		template.subject = "Demande d'inscription de {{demande.prenom}} {{demande.nom}} (M-0370)";
		template.html = Resources.toString(//
				Resources.getResource(this.getClass(), "mail.demande.inscription.pebble"), //
				Charset.forName("UTF-8"));
		template.model = Maps.newHashMap();
		template.model.put("demande", "demande");
		template.authorizedRoles = Collections.singleton("emailboss");
		superadmin.emails().saveTemplate(template);

		// send inscription email
		Map<String, Object> parameters = Maps.newHashMap();
		parameters.put("demande", inscriptionId);

		// guests are not allowed to use this email template
		assertHttpError(403, () -> guest.emails().send(template.name, parameters));

		// vince is not allowed to use this email template
		assertHttpError(403, () -> vince.emails().send(template.name, parameters));

		// nath's got the emailboss role
		// she is allowed to use this email template
		nath.emails().send(template.name, parameters);

		// superadmins deletes this email template
		superadmin.emails().deleteTemplate(template.name);

		// nath can not use this email template anymore
		assertHttpError(404, () -> nath.emails().send(template.name, parameters));
	}
}
