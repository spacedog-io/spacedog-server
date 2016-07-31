/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.MultipartBody;

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

	//
	// Routes
	//

	@Post("/1/mail")
	@Post("/1/mail/")
	public Payload post(Context context) {

		try {
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
			return JsonPayload.json(response, response.get("status").asInt());

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	//
	// Implementation
	//

	public ObjectNode send(String from, String to, String cc, String bcc, String subject, String text, String html) {

		String mailGunKey = Start.get().configuration().mailGunKey();
		String mailDomain = Start.get().configuration().mailDomain();

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

		try {
			HttpResponse<String> response = requestWithBody.asString();
			return JsonPayload.minimalBuilder(response.getStatus()).node("mailgun", response.getBody()).build();
		} catch (UnirestException e) {
			throw new RuntimeException(e);
		}
	}

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
