package io.spacedog.examples;

import java.io.IOException;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceResponse;

public class MailGun {

	public static void main(String[] args) throws IOException {
		try {
			sendSandBoxedMail();
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	static void sendSandBoxedMail() {
		SpaceResponse response = SpaceRequest//
				.post("/v3/sandboxeb1564fe7eb04d7eab56e0c7e7684c65.mailgun.org/messages")//
				.backend("https://api.mailgun.net")//
				.basicAuth("api", "key-04557379769f2de1d01fb12f7e837e1c")//
				.formField("from", "Mailgun Sandbox <postmaster@sandboxeb1564fe7eb04d7eab56e0c7e7684c65.mailgun.org>")//
				.formField("to", "David <david@spacedog.io>")//
				.formField("subject", "Hello David")//
				.formField("text",
						"Congratulations David, you just sent an email with Mailgun!  You are truly awesome!  You can see a record of this email in your logs: https://mailgun.com/cp/log .  You can send up to 300 emails/day from this sandbox server.  Next, you should add your own domain so you can send 10,000 emails/month for free.")//
				.go();

		System.out.println(response.jsonNode());
	}

	static void sendDomainCheckedMail() {
		SpaceResponse response = SpaceRequest.post("/v3/api.spacedog.io/messages")//
				.backend("https://api.mailgun.net")//
				.basicAuth("api", "key-04557379769f2de1d01fb12f7e837e1c")//
				.formField("from", "SpaceDog Platform <platform@spacedog.io>")//
				.formField("to", "David <attias666@gmail.com>")//
				.formField("subject", "My first gunned mail")//
				.formField("text", "We did it!!!")//
				.go();

		System.out.println(response.jsonNode());
	}
}
