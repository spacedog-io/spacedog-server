package io.spacedog.examples;

import java.net.URL;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.resolver.DataSourceUrlResolver;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.Resources;
import com.sun.mail.smtp.SMTPTransport;

import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Utils;

public class SmtpEmail extends Assert {

	@Test
	public void test() throws Exception {
		Properties props = System.getProperties();
		props.put("mail.smtps.host", "smtp.gmail.com");
		props.put("mail.smtps.auth", "true");
		Session session = Session.getInstance(props, null);
		Message msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress("davattias@gmail.com"));

		msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("hello@spacedog.io", false));
		msg.setSubject("Javax Mail test " + System.currentTimeMillis());
		msg.setText("Les oiseaux font cui cui");
		msg.setHeader("X-Mailer", "Space Example");
		msg.setSentDate(new Date());
		SMTPTransport t = (SMTPTransport) session.getTransport("smtps");
		t.connect("smtp.gmail.com", //
				SpaceRequest.configuration().smtpLogin(), //
				SpaceRequest.configuration().smtpPassword());
		t.sendMessage(msg, msg.getAllRecipients());
		System.out.println("Response: " + t.getLastServerResponse());
		t.close();
	}

	@Test
	public void testApacheCommons() throws Exception {

		// load your HTML email template
		String htmlEmailTemplate = Resources.toString(//
				Resources.getResource("io/spacedog/examples/email.html"), Utils.UTF8);

		// define you base URL to resolve relative resource locations
		URL url = new URL("http://www.apache.org");

		// create the email message
		ImageHtmlEmail email = new ImageHtmlEmail();
		email.setDebug(true);
		email.setDataSourceResolver(new DataSourceUrlResolver(url));
		email.setHostName("smtp.gmail.com");
		email.addTo("hello@spacedog.io", "Hello SpaceDog");
		email.setFrom("davattias@gmail.com", "David Attias");
		email.setSubject("Test email with inline image");

		// set the html message
		email.setHtmlMsg(htmlEmailTemplate);

		// set the alternative message
		email.setTextMsg("Your email client does not support HTML messages");

		// send the email
		email.setAuthentication(SpaceRequest.configuration().smtpLogin(), //
				SpaceRequest.configuration().smtpPassword());
		email.setStartTLSRequired(true);
		email.send();
	}
}
