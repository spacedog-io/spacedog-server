package io.spacedog.watchdog;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.Resources;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.MailSettings;
import io.spacedog.utils.MailSettings.SmtpSettings;
import io.spacedog.utils.Utils;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class MailResourceTestOncePerDay extends Assert {

	private static final String DEFAULT_FROM = "david@spacedog.io";
	private static final String DEFAULT_TO = "platform@spacedog.io";
	private static final String DEFAULT_TEXT = "So don't bother read this!";
	private static final String DEFAULT_SUBJECT = "SpaceDog Email Test ...";

	@Test
	public void sendEmails() throws IOException {

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User vince = SpaceClient.newCredentials(test, "vince", "hi vince");

		// by default users can not send emails
		SpaceRequest.post("/1/mail").userAuth(vince).go(403);

		// admin emails a simple text message
		SpaceRequest.post("/1/mail").adminAuth(test)//
				.formField("to", DEFAULT_TO)//
				.formField("subject", DEFAULT_SUBJECT)//
				.formField("text", DEFAULT_TEXT)//
				.go(200);

		// admin allows users to send emails
		MailSettings settings = new MailSettings();
		settings.enableUserFullAccess = true;
		SpaceClient.saveSettings(test, settings);

		// now vince can email a simple html message
		SpaceRequest.post("/1/mail").userAuth(vince)//
				.formField("to", DEFAULT_TO)//
				.formField("subject", DEFAULT_SUBJECT)//
				.formField("html", "<html><h1>So don't bother read this!</h1></html>")//
				.go(200);

		// vince fails to email since no 'to' field
		SpaceRequest.post("/1/mail").userAuth(vince)//
				.formField("subject", DEFAULT_SUBJECT)//
				.formField("text", DEFAULT_TEXT)//
				.go(400);

		// vince fails to email since no html end tag
		SpaceRequest.post("/1/mail").userAuth(vince)//
				.formField("to", DEFAULT_TO)//
				.formField("subject", DEFAULT_SUBJECT)//
				.formField("html", "<html><h1>XXX</h1>")//
				.go(400);

		// admin sets specific mailgun settings with invalid key
		settings.mailgun = new MailSettings.MailGunSettings();
		settings.mailgun.domain = "api.spacedog.io";
		settings.mailgun.key = "123456789";
		SpaceClient.saveSettings(test, settings);

		// admin fails to email since mailgun key is invalid
		SpaceRequest.post("/1/mail").adminAuth(test)//
				.formField("to", DEFAULT_TO)//
				.formField("subject", DEFAULT_SUBJECT)//
				.formField("text", DEFAULT_TEXT)//
				.go(401);

		// admin sets smtp settings
		settings.mailgun = null;
		settings.smtp = new SmtpSettings();
		settings.smtp.host = "mail.gandi.net";
		settings.smtp.startTlsRequired = false;
		settings.smtp.sslOnConnect = true;
		settings.smtp.login = SpaceRequest.configuration().testSmtpLogin();
		settings.smtp.password = SpaceRequest.configuration().testSmtpPassword();
		SpaceClient.saveSettings(test, settings);

		// load your HTML email template
		String emailBody = Resources.toString(//
				Resources.getResource("io/spacedog/watchdog/email.html"), Utils.UTF8);

		// vince emails a text message via smtp
		SpaceRequest.post("/1/mail").userAuth(vince)//
				.formField("to", DEFAULT_TO)//
				.formField("from", DEFAULT_FROM)//
				.formField("subject", DEFAULT_SUBJECT)//
				.formField("text", DEFAULT_TEXT)//
				.go(200)//
				.assertPresent("messageId");

		// vince emails an html message via smtp
		SpaceRequest.post("/1/mail").userAuth(vince)//
				.formField("to", DEFAULT_TO)//
				.formField("from", DEFAULT_FROM)//
				.formField("subject", DEFAULT_SUBJECT)//
				.formField("html", emailBody)//
				.formField("text", DEFAULT_TEXT)//
				.go(200)//
				.assertPresent("messageId");
	}
}
