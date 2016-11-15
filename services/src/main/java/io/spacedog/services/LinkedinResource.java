package io.spacedog.services;

import com.google.common.base.Strings;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
import io.spacedog.utils.Credentials.Session;
import io.spacedog.utils.CredentialsSettings;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.SpaceException;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class LinkedinResource extends Resource {

	//
	// Routes
	//

	@Get("/1/login/linkedin")
	@Get("/1/login/linkedin/")
	@Post("/1/login/linkedin")
	@Post("/1/login/linkedin/")
	// TODO deprecated
	@Post("/1/credentials/linkedin")
	@Post("/1/credentials/linkedin/")
	public Payload getLogin(Context context) {
		Credentials credentials = login(context);

		return JsonPayload.json(//
				JsonPayload.builder()//
						.put(ACCESS_TOKEN, credentials.accessToken()) //
						.put(EXPIRES_IN, credentials.accessTokenExpiresIn()) //
						// TODO deprecated
						// remove the id when colibee is fixed
						.put(ID, credentials.id()) //
						.node(CREDENTIALS, credentials.toJson()));
	}

	@Get("/1/login/linkedin/redirect")
	@Get("/1/login/linkedin/redirect/")
	public Payload getRedirectLogin(Context context) {

		String finalRedirectUri = context.get("redirect_uri");
		String state = context.get("state");

		if (Strings.isNullOrEmpty(finalRedirectUri)) {
			finalRedirectUri = SettingsResource.get()//
					.load(CredentialsSettings.class)//
					.linkedinFinalRedirectUri;

			if (Strings.isNullOrEmpty(finalRedirectUri))
				throw Exceptions.illegalArgument(//
						"credentials settings [linkedinFinalRedirectUri] is required");
		} else
			// TODO remove this when mikael finds out why
			// the redirect_uri is passed with a ';' suffix
			// in mobile apps
			finalRedirectUri = Utils.removeSuffix(finalRedirectUri, ";");

		Escaper paramEscaper = UrlEscapers.urlPathSegmentEscaper();
		StringBuilder location = new StringBuilder(finalRedirectUri)//
				.append("#state=");

		if (state != null)
			location.append(paramEscaper.escape(state));

		try {
			Credentials credentials = login(context);

			location.append("&access_token=")//
					.append(paramEscaper.escape(credentials.accessToken()))//
					.append("&expires=")//
					.append(credentials.accessTokenExpiresIn())//
					.append("&credentialsId=")//
					.append(paramEscaper.escape(credentials.id()));

		} catch (SpaceException e) {
			location.append("&error=")//
					.append(paramEscaper.escape(e.code()))//
					.append("&error_description=")//
					.append(paramEscaper.escape(e.getMessage()));

		} catch (Throwable t) {
			location.append("&error=internal-server-error")//
					.append("&error_description=")//
					.append(paramEscaper.escape(t.getMessage()));
		}

		return new Payload(302).withHeader(SpaceHeaders.LOCATION, location.toString());
	}

	@Get("/1/linkedin/people/me/:fields")
	@Get("/1/linkedin/people/me/:fields/")
	public Payload get(String fields, Context context) {

		Credentials credentials = SpaceContext.checkUserCredentials();

		SpaceResponse response = SpaceRequest//
				.get("https://api.linkedin.com/v1/people/~:({fields})")//
				.bearerAuth(credentials.accessToken())//
				.routeParam("fields", fields)//
				.queryParam("format", "json")//
				.go();

		checkLinkedinError(response, "linkedin error fetching your profil");
		return JsonPayload.json(response.objectNode());
	}

	//
	// implementation
	//

	private void checkLinkedinError(SpaceResponse response, String messageIntro) {

		int httpStatus = response.httpResponse().getStatus();

		if (httpStatus >= 400) {

			String error = response.getString("error");
			String description = response.getString("error_description");
			StringBuilder message = new StringBuilder(messageIntro);

			if (!Strings.isNullOrEmpty(error)) {
				message.append(": ").append(error);

				if (!Strings.isNullOrEmpty(description))
					message.append(" (").append(description).append(")");

				// linkedin return 500 for an invalid redirect_uri
				// WTF! let's consider this is a 400
				if (error.equals("invalid_redirect_uri"))
					httpStatus = 400;
			}

			throw Exceptions.space(httpStatus, message.toString());
		}
	}

	//
	// Implementation
	//

	private Credentials login(Context context) {

		String backendId = SpaceContext.backendId();
		String code = Check.notNullOrEmpty(context.get("code"), "code");

		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);
		if (Strings.isNullOrEmpty(settings.linkedinId))
			throw Exceptions.illegalArgument("credentials settings [linkedinId] is required");

		String redirectUri = context.get("redirect_uri");

		// no redirect_uri in context means
		// redirect_uri = this current resource uri
		// useful when spacedog is directly used as redirect_uri
		if (Strings.isNullOrEmpty(redirectUri))
			redirectUri = spaceUrl(backendId, context.uri()).toString();
		else
			// TODO remove this when mikael finds out why
			// the redirect_uri is passed with a ';' suffix
			// in mobile app
			redirectUri = Utils.removeSuffix(redirectUri, ";");

		SpaceResponse response = SpaceRequest//
				.post("https://www.linkedin.com/oauth/v2/accessToken")//
				.queryParam("grant_type", "authorization_code")//
				.queryParam("client_id", settings.linkedinId)//
				.queryParam("client_secret", settings.linkedinSecret)//
				.queryParam("redirect_uri", redirectUri)//
				.queryParam("code", code)//
				.go();

		checkLinkedinError(response, "linkedin error fetching access token");

		String accessToken = response.objectNode().get("access_token").asText();

		long expiresIn = settings.useLinkedinExpiresIn //
				? response.objectNode().get("expires_in").asLong() //
				: CredentialsResource.get().getCheckSessionLifetime(context);

		Session session = Session.newSession(accessToken, expiresIn);

		response = SpaceRequest//
				.get("https://api.linkedin.com/v1/people/~:(email-address)")//
				.bearerAuth(backendId, accessToken)//
				.queryParam("format", "json")//
				.go();

		checkLinkedinError(response, "linkedin error fetching email");
		String email = response.objectNode().get("emailAddress").asText();

		CredentialsResource credentialsResource = CredentialsResource.get();
		Credentials credentials = credentialsResource.getByName(backendId, email, false)//
				.orElse(new Credentials(backendId, email, Level.USER));

		credentials.setCurrentSession(session);
		credentials.email(email);

		boolean isNew = credentials.createdAt() == null;

		if (isNew) {
			if (settings.disableGuestSignUp)
				throw Exceptions.forbidden("guest sign up is disabled");

			credentials = credentialsResource.create(credentials);
		} else
			credentials = credentialsResource.update(credentials);

		return credentials;
	}

	//
	// singleton
	//

	private static LinkedinResource singleton = new LinkedinResource();

	static LinkedinResource get() {
		return singleton;
	}

	private LinkedinResource() {
	}
}
