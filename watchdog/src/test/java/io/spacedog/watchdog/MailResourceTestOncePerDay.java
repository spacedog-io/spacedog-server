package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceRequest;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class MailResourceTestOncePerDay extends Assert {

	@Test
	public void postMails() {

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// should succeed to mail a simple text message

		SpaceRequest.post("/1/mail").adminAuth(test)//
				.queryParam("test", "true")//
				.formField("to", "platform@spacedog.io")//
				.formField("subject", "This is a test...")//
				.formField("text", "So don't bother read this!")//
				.go(200);

		// should succeed to mail a simple html message

		SpaceRequest.post("/1/mail").adminAuth(test)//
				.queryParam("test", "true")//
				.formField("to", "platform@spacedog.io")//
				.formField("subject", "This is a test...")//
				.formField("html", "<html><h1>So don't bother read this!</h1></html>")//
				.go(200);

		// should fail since no 'to' field

		SpaceRequest.post("/1/mail").adminAuth(test)//
				.queryParam("test", "true")//
				.formField("subject", "This is a test...")//
				.formField("text", "So don't bother read this!")//
				.go(400);

		// should fail since no html end tag

		SpaceRequest.post("/1/mail").adminAuth(test)//
				.queryParam("test", "true")//
				.formField("to", "platform@spacedog.io")//
				.formField("subject", "This is a test...")//
				.formField("html", "<html><h1>So don't bother read this!</h1>")//
				.go(400);

	}

}
