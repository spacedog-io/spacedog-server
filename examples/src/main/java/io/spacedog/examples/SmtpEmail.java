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
	public void testJavaxMail() throws Exception {
		Properties props = System.getProperties();
		props.put("mail.smtps.host", "mail.gandi.net");
		props.put("mail.smtps.auth", "true");
		Session session = Session.getInstance(props, null);
		Message msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress("david@spacedog.io"));

		msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("hello@spacedog.io", false));
		msg.setSubject("Javax Mail test " + System.currentTimeMillis());
		msg.setText("Les oiseaux font cui cui");
		msg.setHeader("X-Mailer", "Space Example");
		msg.setSentDate(new Date());
		SMTPTransport t = (SMTPTransport) session.getTransport("smtps");
		t.connect("mail.gandi.net", //
				SpaceRequest.env().get("spacedog.test.smtp.login"), //
				SpaceRequest.env().get("spacedog.test.smtp.password"));
		t.sendMessage(msg, msg.getAllRecipients());
		System.out.println("Response: " + t.getLastServerResponse());
		t.close();
	}

	@Test
	public void testApacheCommons() throws Exception {

		System.setProperty("http.agent", "SpaceDog Server");

		// load your HTML email template
		String htmlEmailTemplate = Resources.toString(//
				Resources.getResource(this.getClass(), "caremen-welcome.html"), //
				Utils.UTF8);

		// define you base URL to resolve relative resource locations
		URL url = new URL("http://www.apache.org");

		// create the email message
		ImageHtmlEmail email = new ImageHtmlEmail();
		email.setDebug(true);
		email.setDataSourceResolver(new DataSourceUrlResolver(url));
		email.setHostName("mail.gandi.net");
		email.addTo("attias666@gmail.com", "David Attias");
		email.setFrom("david@spacedog.io", "David Attias");
		email.setSubject("Test email with inline image");

		// set the html message
		email.setHtmlMsg(htmlEmailTemplate);

		// set the alternative message
		email.setTextMsg("Your email client does not support HTML messages");

		// send the email
		email.setAuthentication(SpaceRequest.env().get("spacedog.test.smtp.login"), //
				SpaceRequest.env().get("spacedog.test.smtp.password"));
		email.setStartTLSRequired(false);
		email.setSSLOnConnect(true);
		email.send();
	}

}
