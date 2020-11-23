package io.spacedog.services.email;

import java.net.URL;
import java.util.Map;

import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.resolver.DataSourceUrlResolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;

import io.spacedog.client.email.EmailBasicRequest;
import io.spacedog.client.email.EmailRequest;
import io.spacedog.client.email.EmailSettings;
import io.spacedog.client.email.EmailSettings.MailGunSettings;
import io.spacedog.client.email.EmailSettings.SmtpSettings;
import io.spacedog.client.email.EmailTemplate;
import io.spacedog.client.email.EmailTemplateRequest;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.services.PebbleTemplating;
import io.spacedog.services.Server;
import io.spacedog.services.ServerConfig;
import io.spacedog.services.Services;
import io.spacedog.services.SpaceResty;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

public class EmailService {

	public ObjectNode sendIfAuthorized(EmailRequest request) {

		if (request instanceof EmailBasicRequest)
			return sendIfAuthorized((EmailBasicRequest) request);

		if (request instanceof EmailTemplateRequest)
			return sendIfAuthorized((EmailTemplateRequest) request);

		throw Exceptions.illegalArgument("invalid email request type [%s]", //
				request.getClass().getSimpleName());
	}

	public ObjectNode sendIfAuthorized(EmailBasicRequest request) {
		Server.context().credentials().checkRoleAccess(settings().authorizedRoles);
		return send(request);
	}

	public ObjectNode send(EmailBasicRequest request) {

		if (Strings.isNullOrEmpty(request.html))
			request.html = request.text;

		if (request.html == null)
			request.html = "";

		EmailSettings settings = settings();

		if (settings.smtp != null)
			return sendViaSmtp(request, settings.smtp);

		if (settings.mailgun != null)
			return sendViaGun(request, settings.mailgun);

		// if no settings, use default ...
		return sendWithDefaultSettings(request);
	}

	public ObjectNode sendIfAuthorized(EmailTemplateRequest request) {
		EmailTemplate template = getTemplate(request.templateName);
		Server.context().credentials().checkRoleAccess(template.authorizedRoles);
		return send(request, template);
	}

	public ObjectNode send(EmailTemplateRequest request) {
		EmailTemplate template = getTemplate(request.templateName);
		return send(request, template);
	}

	public ObjectNode send(EmailTemplateRequest request, EmailTemplate template) {
		return send(toBasicRequest(request, template));
	}

	public EmailTemplate getTemplate(String templateName) {
		return Services.settings().get(//
				toSettingsId(templateName), EmailTemplate.class)//
				.orElseThrow(() -> Exceptions.illegalArgument(//
						"email template [%s] not found", templateName));
	}

	public void saveTemplate(EmailTemplate template) {
		Services.settings().save(toSettingsId(template.name), template);
	}

	public void deleteTemplate(String templateName) {
		Services.settings().delete(toSettingsId(templateName));
	}

	public EmailSettings settings() {
		return Services.settings().getOrThrow(EmailSettings.class);
	}

	//
	// Implementation
	//

	private static final String HTML = "html";
	private static final String TEXT = "text";
	private static final String SUBJECT = "subject";
	private static final String BCC = "bcc";
	private static final String CC = "cc";
	private static final String TO = "to";
	private static final String FROM = "from";

	private ObjectNode sendWithDefaultSettings(EmailBasicRequest request) {

		MailGunSettings settings = new MailGunSettings();
		settings.key = ServerConfig.mailGunKey();
		settings.domain = ServerConfig.mailDomain();

		String target = Server.backend().id();

		// ... add footnotes to the message
		if (!Strings.isNullOrEmpty(request.text))
			request.text = addFootnotesToTextMessage(request.text, target);
		if (!Strings.isNullOrEmpty(request.html))
			request.html = addFootnotesToHtmlMessage(request.html, target);

		// force the from
		request.from = target.toUpperCase() + " <no-reply@" + settings.domain + ">";

		return sendViaGun(request, settings);
	}

	private EmailBasicRequest toBasicRequest(EmailTemplateRequest request, EmailTemplate template) {

		PebbleTemplating pebble = PebbleTemplating.get();
		Map<String, Object> context = pebble.createContext(//
				template.model, request.parameters);

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

	private String toSettingsId(String templateName) {
		return "internal-email-template-" + templateName;
	}

	//
	// MailGun
	//

	private ObjectNode sendViaGun(EmailBasicRequest emailRequest, MailGunSettings settings) {

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

		JsonNode content = response.isJson() //
				? response.asJson()
				: TextNode.valueOf(response.asString());

		if (response.status() >= 400)
			throw Exceptions.exception(response.status(), //
					"send email via mailgun failed")//
					.withDetails(content);

		return Json.object("mailgun", content);
	}

	//
	// SMTP
	//

	private ObjectNode sendViaSmtp(EmailBasicRequest request, SmtpSettings settings) {

		try {
			ImageHtmlEmail email = new ImageHtmlEmail();
			email.setCharset("UTF-8");
			email.setHtmlMsg(request.html);
			email.setDataSourceResolver(new DataSourceUrlResolver(//
					new URL(SpaceResty.spaceRootUrl().toString())));
			if (!Strings.isNullOrEmpty(request.text))
				email.setTextMsg(request.text);

			email.setDebug(ServerConfig.mailSmtpDebug());

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

			return Json.object("smtp", Json.object("messageId", msgId));

		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw Exceptions.illegalArgument(e);
		}

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

}
