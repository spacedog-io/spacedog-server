/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;

import org.elasticsearch.common.base.Strings;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.MultipartBody;

import net.codestory.http.Context;
import net.codestory.http.Part;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/v1/mail")
public class MailResource extends AbstractResource {

	//
	// MailGun parameter names
	//

	private static final String HTML = "html";
	private static final String TEXT = "text";
	private static final String SUBJECT = "subject";
	private static final String BCC = "bcc";
	private static final String CC = "cc";
	private static final String TO = "to";

	//
	// Routes
	//

	@Post("")
	@Post("/")
	public Payload post(Context context)
			throws JsonParseException, JsonMappingException, IOException, UnirestException {

		Credentials admin = SpaceContext.checkAdminCredentials();
		String from = admin.backendId().toUpperCase() + " <no-reply@api.spacedog.io>";
		String to = null, cc = null, bcc = null, subject = null, text = null, html = null;

		if (context.parts().isEmpty()) {
			to = context.get(TO);
			cc = context.get(CC);
			bcc = context.get(BCC);
			subject = context.get(SUBJECT);
			if (!Strings.isNullOrEmpty(context.get(TEXT)))
				text = addFooterToTextMessage(context.get(TEXT), admin);
			if (!Strings.isNullOrEmpty(context.get(HTML)))
				html = addFooterToHtmlMessage(context.get(HTML), admin);

		} else
			for (Part part : context.parts()) {
				if (part.name().equals(TO))
					to = part.content();
				else if (part.name().equals(CC))
					cc = part.content();
				else if (part.name().equals(BCC))
					bcc = part.content();
				else if (part.name().equals(SUBJECT))
					subject = part.content();
				else if (part.name().equals(TEXT))
					text = addFooterToTextMessage(part.content(), admin);
				else if (part.name().equals(HTML))
					html = addFooterToHtmlMessage(part.content(), admin);
			}

		ObjectNode response = send(from, to, cc, bcc, subject, text, html);
		return Payloads.json(response, response.get("status").asInt());
	}

	public ObjectNode send(String from, String to, String cc, String bcc, String subject, String text, String html)
			throws JsonParseException, JsonMappingException, IOException, UnirestException {

		String mailGunKey = Start.get().configuration().getMailGunKey().orElseThrow(//
				() -> new RuntimeException("No mailgun key set in configuration"));

		String mailDomain = Start.get().configuration().getMailDomain().orElseThrow(//
				() -> new RuntimeException("No mail domain set in configuration"));

		HttpRequestWithBody requestWithBody = Unirest.post("https://api.mailgun.net/v3/{domain}/messages")//
				.routeParam("domain", mailDomain)//
				// TODO Fix this since it does not work.
				// .queryString("o:testmode", context.query().getBoolean("test",
				// false))//
				.basicAuth("api", mailGunKey);

		MultipartBody multipartBody = requestWithBody.field("from", from)//
				.field(TO, to)//
				.field(CC, cc)//
				.field(BCC, bcc)//
				.field(SUBJECT, subject);

		if (!Strings.isNullOrEmpty(text))
			multipartBody.field(TEXT, text);
		if (!Strings.isNullOrEmpty(html))
			multipartBody.field(HTML, html);

		HttpResponse<String> response = requestWithBody.asString();
		return Payloads.minimalBuilder(response.getStatus()).node("mailgun", response.getBody()).build();
	}

	//
	// Implementation
	//

	private String addFooterToTextMessage(String text, Credentials admin) {
		if (Strings.isNullOrEmpty(text))
			throw new IllegalArgumentException("mail text is empty");

		return String.format(
				"%s\n\n---\nThis is an automatic email sent by the [%s] application.\nContact your administrator for more information [%s].",
				text, admin.backendId().toUpperCase(), admin.email().get());
	}

	private String addFooterToHtmlMessage(String html, Credentials admin) {
		if (Strings.isNullOrEmpty(html))
			throw new IllegalArgumentException("mail html is empty");

		int index = html.lastIndexOf("</html>");
		if (index < 0)
			throw new IllegalArgumentException("no html end tag");

		return String.format(
				"%s<p><p>---<p>This is an automatic email sent by the [%s] application.<p>Contact your administrator for more information [%s].</html>",
				html.substring(0, index), admin.backendId().toUpperCase(), admin.email().get());
	}

	//
	// singleton
	//

	private static MailResource singleton = new MailResource();

	static MailResource get() {
		return singleton;
	}

	private MailResource() {
	}
}
