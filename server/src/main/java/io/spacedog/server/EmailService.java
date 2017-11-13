/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.net.URL;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.resolver.DataSourceUrlResolver;

import com.google.common.base.Strings;

import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceResponse;
import io.spacedog.model.EmailBasicRequest;
import io.spacedog.model.EmailRequest;
import io.spacedog.model.EmailSettings;
import io.spacedog.model.EmailSettings.MailGunSettings;
import io.spacedog.model.EmailSettings.SmtpSettings;
import io.spacedog.model.EmailTemplate;
import io.spacedog.model.EmailTemplateRequest;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.NotFoundException;
import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class EmailService extends SpaceService {

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

	@Post("/1/emails")
	@Post("/1/emails/")
	public Payload post(EmailRequest email, Context context) {

		if (email instanceof EmailBasicRequest) {
			Credentials credentials = SpaceContext.credentials();
			EmailSettings settings = SettingsService.get().getAsObject(EmailSettings.class);

			if (!credentials.isAtLeastSuperAdmin())
				credentials.checkRoles(settings.authorizedRoles);

			return email((EmailBasicRequest) email);
		}

		if (email instanceof EmailTemplateRequest) {
			EmailTemplateRequest templateRequest = (EmailTemplateRequest) email;

			EmailTemplate template = getTemplate(templateRequest.templateName)//
					.orElseThrow(() -> new NotFoundException(//
							"mail template [%s] not found", templateRequest.templateName));

			SpaceContext.credentials().checkRoles(template.roles);

			return email(templateRequest, template);
		}

		throw Exceptions.illegalArgument("invalid email request type [%s]", //
				email.getClass().getSimpleName());
	}

	//
	// Basic Request Interface and Implementation
	//

	public Payload email(EmailBasicRequest request) {

		if (Strings.isNullOrEmpty(request.html))
			request.html = request.text;

		if (Strings.isNullOrEmpty(request.html))
			throw Exceptions.illegalArgument("mail body is empty");

		EmailSettings settings = SettingsService.get().getAsObject(EmailSettings.class);

		if (settings.smtp != null)
			return emailViaSmtp(request, settings.smtp);

		if (settings.mailgun != null)
			return emailViaGun(request, settings.mailgun);

		// if no settings, use default ...
		return emailWithDefaultSettings(request);
	}

	Payload emailWithDefaultSettings(EmailBasicRequest request) {

		MailGunSettings settings = new MailGunSettings();
		settings.key = Start.get().configuration().mailGunKey();
		settings.domain = Start.get().configuration().mailDomain();

		String target = SpaceContext.backendId();

		// ... add footnotes to the message
		if (!Strings.isNullOrEmpty(request.text))
			request.text = addFootnotesToTextMessage(request.text, target);
		if (!Strings.isNullOrEmpty(request.html))
			request.html = addFootnotesToHtmlMessage(request.html, target);

		// force the from
		request.from = target.toUpperCase() + " <no-reply@" + settings.domain + ">";

		return emailViaGun(request, settings);
	}

	Payload emailViaGun(EmailBasicRequest request, MailGunSettings settings) {
		return mailgun(request, settings);
	}

	Payload emailViaSmtp(EmailBasicRequest request, SmtpSettings settings) {

		try {
			ImageHtmlEmail email = new ImageHtmlEmail();
			email.setCharset("UTF-8");
			email.setHtmlMsg(request.html);
			email.setDataSourceResolver(new DataSourceUrlResolver(//
					new URL(spaceRootUrl().toString())));
			if (!Strings.isNullOrEmpty(request.text))
				email.setTextMsg(request.text);

			email.setDebug(Start.get().configuration().mailSmtpDebug());

			if (request.from != null)
				email.setFrom(request.from);
			if (request.to != null)
				for (String to : request.to)
					email.addTo(to);
			if (request.cc != null)
				for (String cc : request.cc)
					email.addCc(cc);
			if (request.bcc != null)
				for (String bcc : request.bcc)
					email.addBcc(bcc);
			if (request.subject != null)
				email.setSubject(request.subject);

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

	private Payload mailgun(EmailBasicRequest emailRequest, MailGunSettings settings) {

		SpaceRequest httpRequest = SpaceRequest.post("/v3/{domain}/messages")//
				.backend("https://api.mailgun.net")//
				.routeParam("domain", settings.domain)//
				.basicAuth("api", settings.key)//
				.formField(FROM, emailRequest.from)//
				.formField(SUBJECT, emailRequest.subject);

		if (emailRequest.to != null)
			for (String to : emailRequest.to)
				httpRequest.formField(TO, to);

		if (emailRequest.cc != null)
			for (String cc : emailRequest.cc)
				httpRequest.formField(CC, cc);

		if (emailRequest.bcc != null)
			for (String bcc : emailRequest.bcc)
				httpRequest.formField(BCC, bcc);

		if (!Strings.isNullOrEmpty(emailRequest.text))
			httpRequest.formField(TEXT, emailRequest.text);

		if (!Strings.isNullOrEmpty(emailRequest.html))
			httpRequest.formField(HTML, emailRequest.html);

		SpaceResponse response = httpRequest.go();

		return JsonPayload.status(response.status())//
				.withFields("mailgun", response.isJson() //
						? response.asJson()
						: response.asString())//
				.build();
	}

	//
	// Footnotes
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
	// Template Request Interface and Implementation
	//

	public Payload email(EmailTemplateRequest request, EmailTemplate template) {

		Map<String, Object> context = PebbleTemplating.get()//
				.createContext(template.model, request.parameters);

		return email(template, context);
	}

	public Payload email(EmailTemplate template, Map<String, Object> context) {
		EmailBasicRequest basicRequest = toBasicRequest(template, context);
		return email(basicRequest);
	}

	public Optional<EmailTemplate> getTemplate(String name) {

		EmailSettings settings = SettingsService.get().getAsObject(EmailSettings.class);

		if (settings.templates == null)
			return Optional.empty();

		return Optional.ofNullable(settings.templates.get(name));
	}

	private EmailBasicRequest toBasicRequest(EmailTemplate template, Map<String, Object> context) {

		PebbleTemplating pebble = PebbleTemplating.get();

		EmailBasicRequest basicRequest = new EmailBasicRequest();
		basicRequest.from = pebble.render("from", template.from, context);
		basicRequest.to = pebble.render("to", template.to, context);
		basicRequest.cc = pebble.render("cc", template.cc, context);
		basicRequest.bcc = pebble.render("bcc", template.bcc, context);
		basicRequest.subject = pebble.render("subject", template.subject, context);
		basicRequest.text = pebble.render("text", template.text, context);
		basicRequest.html = pebble.render("html", template.html, context);
		return basicRequest;
	}

	//
	// singleton
	//

	private static EmailService singleton = new EmailService();

	static EmailService get() {
		return singleton;
	}

	private EmailService() {
		SettingsService.get().registerSettings(EmailSettings.class);
	}
}
