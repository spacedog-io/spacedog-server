package io.spacedog.test.mail;

import java.io.IOException;

import org.junit.Test;

import com.google.common.io.Resources;

import io.spacedog.model.MailSettings;
import io.spacedog.model.MailSettings.SmtpSettings;
import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Utils;

public class MailResourceTest extends SpaceTest {

	private static final String DEFAULT_FROM = "david@spacedog.io";
	private static final String DEFAULT_TO = "platform@spacedog.io";
	private static final String DEFAULT_TEXT = "So don't bother read this!";
	private static final String DEFAULT_SUBJECT = "SpaceDog Email Test ...";

	@Test
	public void sendEmails() throws IOException {

		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog vince = signUp(test, "vince", "hi vince");

		// by default users can not send emails
		SpaceRequest.post("/1/mail").auth(vince).go(403);

		// admin emails a simple text message
		SpaceRequest.post("/1/mail").auth(test)//
				.formField("to", DEFAULT_TO)//
				.formField("subject", DEFAULT_SUBJECT)//
				.formField("text", DEFAULT_TEXT)//
				.go(200);

		// admin allows users to send emails
		MailSettings settings = new MailSettings();
		settings.enableUserFullAccess = true;
		test.settings().save(settings);

		// now vince can email a simple html message
		SpaceRequest.post("/1/mail").auth(vince)//
				.formField("to", DEFAULT_TO)//
				.formField("subject", DEFAULT_SUBJECT)//
				.formField("html", "<html><h1>So don't bother read this!</h1></html>")//
				.go(200);

		// vince fails to email since no 'to' field
		SpaceRequest.post("/1/mail").auth(vince)//
				.formField("subject", DEFAULT_SUBJECT)//
				.formField("text", DEFAULT_TEXT)//
				.go(400);

		// vince fails to email since no html end tag
		SpaceRequest.post("/1/mail").auth(vince)//
				.formField("to", DEFAULT_TO)//
				.formField("subject", DEFAULT_SUBJECT)//
				.formField("html", "<html><h1>XXX</h1>")//
				.go(400);

		// admin sets specific mailgun settings with invalid key
		settings.mailgun = new MailSettings.MailGunSettings();
		settings.mailgun.domain = "api.spacedog.io";
		settings.mailgun.key = "123456789";
		test.settings().save(settings);

		// admin fails to email since mailgun key is invalid
		SpaceRequest.post("/1/mail").auth(test)//
				.formField("from", DEFAULT_FROM)//
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
		settings.smtp.login = SpaceEnv.defaultEnv().get("spacedog.test.smtp.login");
		settings.smtp.password = SpaceEnv.defaultEnv().get("spacedog.test.smtp.password");
		test.settings().save(settings);

		// load your HTML email template
		String emailBody = Resources.toString(//
				Resources.getResource(this.getClass(), "email.html"), //
				Utils.UTF8);

		// vince emails a text message via smtp
		SpaceRequest.post("/1/mail").auth(vince)//
				.formField("from", DEFAULT_FROM)//
				.formField("to", DEFAULT_TO)//
				.formField("subject", DEFAULT_SUBJECT)//
				.formField("text", DEFAULT_TEXT)//
				.go(200)//
				.assertPresent("messageId");

		// vince emails an html message via smtp
		SpaceRequest.post("/1/mail").auth(vince)//
				.formField("from", DEFAULT_FROM)//
				.formField("to", DEFAULT_TO)//
				.formField("subject", DEFAULT_SUBJECT)//
				.formField("html", emailBody)//
				.formField("text", DEFAULT_TEXT)//
				.go(200)//
				.assertPresent("messageId");
	}
}
