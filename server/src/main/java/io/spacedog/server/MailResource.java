/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.mail.resolver.DataSourceUrlResolver;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.spacedog.model.MailSettings;
import io.spacedog.model.MailSettings.MailGunSettings;
import io.spacedog.model.MailSettings.SmtpSettings;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceResponse;
import io.spacedog.utils.Exceptions;
import net.codestory.http.Context;
import net.codestory.http.Part;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class MailResource extends Resource {

	//
	// MailGun parameter names
	//

	private static final String HTML = "html";
	private static final String TEXT = "text";
	private static final String SUBJECT = "subject";
	private static final String BCC = "bcc";
	private static final String CC = "cc";
	private static final String TO = "to";
	private static final String FROM = "from";

	//
	// Routes
	//

	@Post("/1/mail")
	@Post("/1/mail/")
	public Payload post(Context context) {

		MailSettings settings = SettingsResource.get().getAsObject(MailSettings.class);

		if (settings.enableUserFullAccess)
			SpaceContext.credentials().checkAtLeastUser();
		else
			SpaceContext.credentials().checkAtLeastAdmin();

		return email(toMessage(context));
	}

	//
	// Implementation
	//

	static class Message {
		public String from;
		public List<String> to;
		public List<String> cc;
		public List<String> bcc;
		public String subject;
		public String text;
		public String html;
	}

	private Message toMessage(Context context) {
		Message message = new Message();

		if (context.parts().isEmpty()) {
			message.from = context.get(FROM);
			message.to = toList(context.get(TO));
			message.cc = toList(context.get(CC));
			message.bcc = toList(context.get(BCC));
			message.subject = context.get(SUBJECT);
			message.text = context.get(TEXT);
			message.html = context.get(HTML);

		} else
			for (Part part : context.parts()) {
				try {
					if (part.name().equals(FROM))
						message.from = part.content();
					if (part.name().equals(TO))
						message.to = toList(part.content());
					else if (part.name().equals(CC))
						message.cc = toList(part.content());
					else if (part.name().equals(BCC))
						message.bcc = toList(part.content());
					else if (part.name().equals(SUBJECT))
						message.subject = part.content();
					else if (part.name().equals(TEXT))
						message.text = part.content();
					else if (part.name().equals(HTML))
						message.html = part.content();

				} catch (IOException e) {
					throw Exceptions.illegalArgument(e, "invalid [%s] part in multipart request", part.name());
				}
			}

		return message;
	}

	private List<String> toList(String value) {
		return value == null ? null : Lists.newArrayList(value);
	}

	public Payload email(Message message) {

		MailSettings settings = SettingsResource.get().getAsObject(MailSettings.class);

		if (settings.smtp != null)
			return emailViaSmtp(settings.smtp, message);

		if (settings.mailgun != null)
			return emailViaGun(settings.mailgun, message);

		// if no settings, use default ...
		return emailWithDefaultSettings(message);
	}

	Payload emailWithDefaultSettings(Message message) {

		MailGunSettings settings = new MailGunSettings();
		settings.key = Start.get().configuration().mailGunKey();
		settings.domain = Start.get().configuration().mailDomain();

		String target = SpaceContext.backendId();

		// ... add a footer to the message
		if (!Strings.isNullOrEmpty(message.text))
			message.text = addFooterToTextMessage(message.text, target);
		if (!Strings.isNullOrEmpty(message.html))
			message.html = addFooterToHtmlMessage(message.html, target);

		// force the from
		message.from = target.toUpperCase() + " <no-reply@" + settings.domain + ">";

		return emailViaGun(settings, message);
	}

	Payload emailViaGun(MailGunSettings settings, Message message) {
		return mailgun(message, settings);
	}

	Payload emailViaSmtp(SmtpSettings settings, Message message) {

		Email email = null;

		try {
			if (!Strings.isNullOrEmpty(message.html)) {
				ImageHtmlEmail imageHtmlEmail = new ImageHtmlEmail();
				imageHtmlEmail.setCharset("UTF-8");
				imageHtmlEmail.setHtmlMsg(message.html);
				imageHtmlEmail.setDataSourceResolver(new DataSourceUrlResolver(//
						new URL("http://www.apache.org")));
				if (!Strings.isNullOrEmpty(message.text))
					imageHtmlEmail.setTextMsg(message.text);
				email = imageHtmlEmail;

			} else if (!Strings.isNullOrEmpty(message.text)) {
				SimpleEmail simpleEmail = new SimpleEmail();
				simpleEmail.setMsg(message.text);
				email = simpleEmail;

			} else
				throw Exceptions.illegalArgument("no text or html message");

			email.setDebug(Start.get().configuration().mailSmtpDebug());

			if (message.from != null)
				email.setFrom(message.from);
			if (message.to != null)
				for (String to : message.to)
					email.addTo(to);
			if (message.cc != null)
				for (String cc : message.cc)
					email.addCc(cc);
			if (message.bcc != null)
				for (String bcc : message.bcc)
					email.addBcc(bcc);
			if (message.subject != null)
				email.setSubject(message.subject);

			if (!Strings.isNullOrEmpty(settings.login))
				email.setAuthentication(settings.login, settings.password);

			email.setSSLOnConnect(settings.sslOnConnect);
			email.setStartTLSRequired(settings.startTlsRequired);
			email.setHostName(settings.host);

			String msgId = email.send();

			return JsonPayload.ok().with("messageId", msgId).build();

		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw Exceptions.illegalArgument(e);
		}

	}

	private Payload mailgun(Message message, MailGunSettings settings) {

		SpaceRequest request = SpaceRequest.post("/v3/{domain}/messages")//
				.backend("https://api.mailgun.net")//
				.routeParam("domain", settings.domain)//
				.basicAuth("api", settings.key)//
				.formField(FROM, message.from)//
				.formField(SUBJECT, message.subject);

		if (message.to != null)
			for (String to : message.to)
				request.formField(TO, to);

		if (message.cc != null)
			for (String cc : message.cc)
				request.formField(CC, cc);

		if (message.bcc != null)
			for (String bcc : message.bcc)
				request.formField(BCC, bcc);

		if (!Strings.isNullOrEmpty(message.text))
			request.formField(TEXT, message.text);

		if (!Strings.isNullOrEmpty(message.html))
			request.formField(HTML, message.html);

		SpaceResponse response = request.go();

		return JsonPayload.status(response.status())//
				.with("mailgun", response.isJson() //
						? response.asJson()
						: response.asString())//
				.build();
	}

	private String addFooterToTextMessage(String text, String backendId) {
		if (Strings.isNullOrEmpty(text))
			throw Exceptions.illegalArgument("mail text is empty");

		return String.format(
				"%s\n\n---\nThis is an automatic email sent by the [%s] application.\nContact your administrator for more information.",
				text, backendId.toUpperCase());
	}

	private String addFooterToHtmlMessage(String html, String backendId) {
		if (Strings.isNullOrEmpty(html))
			throw Exceptions.illegalArgument("mail html is empty");

		int index = html.lastIndexOf("</html>");
		if (index < 0)
			throw Exceptions.illegalArgument("no html end tag");

		return String.format(
				"%s<p><p>---<p>This is an automatic email sent by the [%s] application.<p>Contact your administrator for more information.</html>",
				html.substring(0, index), backendId.toUpperCase());
	}

	//
	// singleton
	//

	private static MailResource singleton = new MailResource();

	static MailResource get() {
		return singleton;
	}

	private MailResource() {
		SettingsResource.get().registerSettings(MailSettings.class);
	}
}
