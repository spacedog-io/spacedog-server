package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.MailSettings;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class MailResourceTestOncePerDay extends Assert {

	@Test
	public void postMails() {

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User vince = SpaceClient.newCredentials(test, "vince", "hi vince");

		// by default users can not send emails
		SpaceRequest.post("/1/mail").userAuth(vince).go(403);

		// admin emails a simple text message
		SpaceRequest.post("/1/mail").adminAuth(test)//
				.formField("to", "platform@spacedog.io")//
				.formField("subject", "This is a test...")//
				.formField("text", "So don't bother read this!")//
				.go(200);

		// admin allows users to send emails
		MailSettings settings = new MailSettings();
		settings.enableUserFullAccess = true;
		SpaceClient.saveSettings(test, settings);

		// now vince can email a simple html message
		SpaceRequest.post("/1/mail").userAuth(vince)//
				.formField("to", "platform@spacedog.io")//
				.formField("subject", "This is a test...")//
				.formField("html", "<html><h1>So don't bother read this!</h1></html>")//
				.go(200);

		// vince fails to email since no 'to' field
		SpaceRequest.post("/1/mail").userAuth(vince)//
				.formField("subject", "This is a test...")//
				.formField("text", "So don't bother read this!")//
				.go(400);

		// vince fails to email since no html end tag
		SpaceRequest.post("/1/mail").userAuth(vince)//
				.formField("to", "platform@spacedog.io")//
				.formField("subject", "This is a test...")//
				.formField("html", "<html><h1>So don't bother read this!</h1>")//
				.go(400);
	}
}
