/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.net.URL;

import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.resolver.DataSourceUrlResolver;

import com.google.common.base.Strings;

import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceResponse;
import io.spacedog.model.Mail;
import io.spacedog.model.MailSettings;
import io.spacedog.model.MailSettings.MailGunSettings;
import io.spacedog.model.MailSettings.SmtpSettings;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class MailService extends SpaceService {

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
	public Payload post(Mail mail, Context context) {
		Credentials credentials = SpaceContext.credentials();
		MailSettings settings = SettingsService.get().getAsObject(MailSettings.class);

		if (!credentials.isAtLeastSuperAdmin())
			credentials.checkRoles(settings.authorizedRoles);

		return email(mail);
	}

	//
	// Implementation
	//

	public Payload email(Mail message) {

		if (Strings.isNullOrEmpty(message.html))
			message.html = message.text;
		if (Strings.isNullOrEmpty(message.html))
			throw Exceptions.illegalArgument("mail body is empty");

		MailSettings settings = SettingsService.get().getAsObject(MailSettings.class);

		if (settings.smtp != null)
			return emailViaSmtp(message, settings.smtp);

		if (settings.mailgun != null)
			return emailViaGun(message, settings.mailgun);

		// if no settings, use default ...
		return emailWithDefaultSettings(message);
	}

	Payload emailWithDefaultSettings(Mail message) {

		MailGunSettings settings = new MailGunSettings();
		settings.key = Start.get().configuration().mailGunKey();
		settings.domain = Start.get().configuration().mailDomain();

		String target = SpaceContext.backendId();

		// ... add footnotes to the message
		if (!Strings.isNullOrEmpty(message.text))
			message.text = addFootnotesToTextMessage(message.text, target);
		if (!Strings.isNullOrEmpty(message.html))
			message.html = addFootnotesToHtmlMessage(message.html, target);

		// force the from
		message.from = target.toUpperCase() + " <no-reply@" + settings.domain + ">";

		return emailViaGun(message, settings);
	}

	Payload emailViaGun(Mail message, MailGunSettings settings) {
		return mailgun(message, settings);
	}

	Payload emailViaSmtp(Mail message, SmtpSettings settings) {

		try {
			ImageHtmlEmail email = new ImageHtmlEmail();
			email.setCharset("UTF-8");
			email.setHtmlMsg(message.html);
			email.setDataSourceResolver(new DataSourceUrlResolver(//
					new URL(spaceRootUrl().toString())));
			if (!Strings.isNullOrEmpty(message.text))
				email.setTextMsg(message.text);

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

			return JsonPayload.ok().withFields("messageId", msgId).build();

		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw Exceptions.illegalArgument(e);
		}

	}

	private Payload mailgun(Mail message, MailGunSettings settings) {

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
				.withFields("mailgun", response.isJson() //
						? response.asJson()
						: response.asString())//
				.build();
	}

	//
	// Foot notes
	//

	private static final String FOOTNOTE_TEXT_TEMPLATE = "%s\n\n---\n%s\n%s";
	private static final String FOOTNOTE_HTML_TEMPLATE = "%s<p><p>---<p>%s<p>%s%s";
	private static final String FOOTNOTE_LINE_1 = "This is an automatic email sent by the [%s] application.";
	private static final String FOOTNOTE_LINE_2 = "Contact your administrator for more information.";

	private String addFootnotesToTextMessage(String text, String backendId) {
		text = String.format(FOOTNOTE_TEXT_TEMPLATE, text, FOOTNOTE_LINE_1, FOOTNOTE_LINE_2);
		return String.format(text, backendId.toUpperCase());
	}

	private String addFootnotesToHtmlMessage(String html, String backendId) {

		int index = html.lastIndexOf("</body>");
		if (index < 0)
			index = html.lastIndexOf("</html>");

		html = index < 0 //
				? String.format(FOOTNOTE_HTML_TEMPLATE, html, FOOTNOTE_LINE_1, FOOTNOTE_LINE_2, "") //
				: String.format(FOOTNOTE_HTML_TEMPLATE, html.substring(0, index), FOOTNOTE_LINE_1, FOOTNOTE_LINE_2,
						html.substring(index));

		return String.format(html, backendId.toUpperCase());
	}

	//
	// singleton
	//

	private static MailService singleton = new MailService();

	static MailService get() {
		return singleton;
	}

	private MailService() {
		SettingsService.get().registerSettings(MailSettings.class);
	}
}
