package io.spacedog.examples;

import java.io.IOException;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class MailGun {

	public static void main(String[] args) throws IOException {
		try {
			sendSandBoxedMail();
		} catch (Throwable t) {
			t.printStackTrace();
			Unirest.shutdown();
			System.exit(-1);
		}
	}

	static void sendSandBoxedMail() throws UnirestException {
		System.out.println(Unirest
				.post("https://api.mailgun.net/v3/sandboxeb1564fe7eb04d7eab56e0c7e7684c65.mailgun.org/messages")//
				.basicAuth("api", "key-04557379769f2de1d01fb12f7e837e1c")//
				.field("from", "Mailgun Sandbox <postmaster@sandboxeb1564fe7eb04d7eab56e0c7e7684c65.mailgun.org>")//
				.field("to", "David <david@spacedog.io>")//
				.field("subject", "Hello David")//
				.field("text",
						"Congratulations David, you just sent an email with Mailgun!  You are truly awesome!  You can see a record of this email in your logs: https://mailgun.com/cp/log .  You can send up to 300 emails/day from this sandbox server.  Next, you should add your own domain so you can send 10,000 emails/month for free.")//
				.asJson().getBody());
	}

	static void sendDomainCheckedMail() throws UnirestException {
		System.out.println(Unirest.post("https://api.mailgun.net/v3/api.spacedog.io/messages")//
				.basicAuth("api", "key-04557379769f2de1d01fb12f7e837e1c")//
				.field("from", "SpaceDog Platform <platform@spacedog.io>")//
				.field("to", "David <attias666@gmail.com>")//
				.field("subject", "My first gunned mail")//
				.field("text", "We did it!!!")//
				.asJson().getBody());
	}
}
