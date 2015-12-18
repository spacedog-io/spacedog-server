/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;

import org.elasticsearch.common.base.Strings;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
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

		String mailGunKey = Start.get().configuration().getMailGunKey().orElseThrow(//
				() -> new RuntimeException("No mailgun key set in configuration"));

		String mailGunDomain = Start.get().configuration().getMailGunDomain().orElseThrow(//
				() -> new RuntimeException("No mailgun domain set in configuration"));

		HttpRequestWithBody requestWithBody = Unirest.post("https://api.mailgun.net/v3/{domain}/messages")//
				.routeParam("domain", mailGunDomain)//
				// TODO Fix this since it does not work.
				// .queryString("o:testmode", context.query().getBoolean("test",
				// false))//
				.basicAuth("api", mailGunKey);

		MultipartBody multipartBody = requestWithBody.field("from",
				admin.backendId().toUpperCase() + " <no-reply@spacedog.io>");

		if (context.parts().isEmpty()) {
			multipartBody.field(TO, context.get(TO));
			multipartBody.field(CC, context.get(CC));
			multipartBody.field(BCC, context.get(BCC));
			multipartBody.field(SUBJECT, context.get(SUBJECT));
			if (!Strings.isNullOrEmpty(context.get(TEXT)))
				multipartBody.field(TEXT, addFooterToTextMessage(context.get(TEXT), admin));
			if (!Strings.isNullOrEmpty(context.get(HTML)))
				multipartBody.field(HTML, addFooterToHtmlMessage(context.get(HTML), admin));

		} else
			for (Part part : context.parts()) {
				if (part.name().equals(TO))
					multipartBody.field(part.name(), part.content(), part.contentType());
				else if (part.name().equals(CC))
					multipartBody.field(part.name(), part.content(), part.contentType());
				else if (part.name().equals(BCC))
					multipartBody.field(part.name(), part.content(), part.contentType());
				else if (part.name().equals(SUBJECT))
					multipartBody.field(part.name(), part.content(), part.contentType());
				else if (part.name().equals(TEXT))
					multipartBody.field(part.name(), //
							addFooterToTextMessage(part.content(), admin), part.contentType());
				else if (part.name().equals(HTML))
					multipartBody.field(part.name(), //
							addFooterToHtmlMessage(part.content(), admin), part.contentType());
			}

		HttpResponse<String> response = requestWithBody.asString();
		return PayloadHelper.json(
				PayloadHelper.minimalBuilder(response.getStatus()).node("mailgun", response.getBody()),
				response.getStatus());
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
