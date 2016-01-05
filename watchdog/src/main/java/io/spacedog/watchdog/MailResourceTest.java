package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceRequest;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class MailResourceTest extends Assert {

	@Test
	public void postMails() throws Exception {

		SpaceDogHelper.printTestHeader();
		Account account = SpaceDogHelper.resetTestAccount();

		// should succeed to mail a simple text message

		SpaceRequest.post("/v1/mail").basicAuth(account)//
				.queryString("test", "true")//
				.field("to", "platform@spacedog.io")//
				.field("subject", "This is a test...")//
				.field("text", "So don't bother read this!")//
				.go(200);

		// should succeed to mail a simple html message

		SpaceRequest.post("/v1/mail").basicAuth(account)//
				.queryString("test", "true")//
				.field("to", "platform@spacedog.io")//
				.field("subject", "This is a test...")//
				.field("html", "<html><h1>So don't bother read this!</h1></html>")//
				.go(200);

		// should fail since no 'to' field

		SpaceRequest.post("/v1/mail").basicAuth(account)//
				.queryString("test", "true")//
				.field("subject", "This is a test...")//
				.field("text", "So don't bother read this!")//
				.go(400);

		// should fail since no html end tag

		SpaceRequest.post("/v1/mail").basicAuth(account)//
				.queryString("test", "true")//
				.field("to", "platform@spacedog.io")//
				.field("subject", "This is a test...")//
				.field("html", "<html><h1>So don't bother read this!</h1>")//
				.go(400);

	}

}
