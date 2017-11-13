package io.spacedog.test.mail;

import java.io.IOException;

import org.junit.Test;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceEnv;
import io.spacedog.model.EmailBasicRequest;
import io.spacedog.model.MailSettings;
import io.spacedog.model.MailSettings.SmtpSettings;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Utils;

public class MailServiceTest extends SpaceTest {

	private static final String DEFAULT_FROM = "david@spacedog.io";
	private static final String DEFAULT_TO = "platform@spacedog.io";
	private static final String DEFAULT_TEXT = "∆ hello ∆";
	private static final String DEFAULT_SUBJECT = "SpaceDog Email Test";

	@Test
	public void sendEmails() throws IOException {

		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");

		// by default only superadmins can send emails
		assertHttpError(403, () -> vince.mail().send(defaultMail()));

		// superadmin emails a simple text message
		superadmin.mail().send(defaultMail());

		// superadmin allows users to send emails
		MailSettings settings = new MailSettings();
		settings.authorizedRoles = Sets.newHashSet("user");
		superadmin.settings().save(settings);

		// now vince can email a simple html message
		EmailBasicRequest mail = defaultMail();
		mail.html = "<html><h1>" + DEFAULT_TEXT + "</h1></html>";
		vince.mail().send(mail);

		// vince can email with html without ending tags
		mail.html = "<html><h1>" + DEFAULT_TEXT + "</h1>";
		vince.mail().send(mail);

		// vince fails to email since no 'to' field
		assertHttpError(400, () -> vince.mail().send(defaultMail().to(null)));

		// superadmin sets specific mailgun settings with invalid key
		settings.mailgun = new MailSettings.MailGunSettings();
		settings.mailgun.domain = "api.spacedog.io";
		settings.mailgun.key = "123456789";
		superadmin.settings().save(settings);

		// superadmin fails to email since mailgun key is invalid
		assertHttpError(401, () -> superadmin.mail().send(defaultMail()));

		// superadmin sets smtp settings
		settings.mailgun = null;
		settings.smtp = smtpSettings();
		superadmin.settings().save(settings);

		// vince emails a text message via smtp
		ObjectNode response = vince.mail().send(defaultMail());
		assertNotNull(response.get("messageId").asText());

		// vince emails an html message via smtp
		mail = defaultMail();
		mail.html = Resources.toString(//
				Resources.getResource(this.getClass(), "email.html"), //
				Utils.UTF8);
		response = vince.mail().send(mail);
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
}
