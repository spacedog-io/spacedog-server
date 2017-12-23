package io.spacedog.server;

import com.google.common.base.Strings;

import io.spacedog.http.SpaceException;
import io.spacedog.http.SpaceHeaders;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceResponse;
import io.spacedog.http.Uris;
import io.spacedog.model.Credentials;
import io.spacedog.model.CredentialsSettings;
import io.spacedog.model.Roles;
import io.spacedog.model.Credentials.Session;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class LinkedinService extends SpaceService {

	//
	// Routes
	//

	@Post("/1/login/linkedin")
	@Post("/1/login/linkedin/")
	public Payload getLogin(Context context) {
		Credentials credentials = login(context);

		return JsonPayload.ok()//
				.withFields(ACCESS_TOKEN_FIELD, credentials.accessToken()) //
				.withFields(EXPIRES_IN_FIELD, credentials.accessTokenExpiresIn()) //
				.withFields(CREDENTIALS_FIELD, credentials.toJson())//
				.build();
	}

	@Get("/1/login/linkedin/redirect")
	@Get("/1/login/linkedin/redirect/")
	public Payload getRedirectLogin(Context context) {

		String finalRedirectUri = context.get("redirect_uri");
		String state = context.get("state");

		if (Strings.isNullOrEmpty(finalRedirectUri)) {
			finalRedirectUri = getCredentialsSettings()//
					.linkedin.finalRedirectUri;

			if (Strings.isNullOrEmpty(finalRedirectUri))
				throw Exceptions.illegalArgument(//
						"credentials settings [linkedin.finalRedirectUri] is required");
		} else
			// TODO remove this when mikael finds out why
			// the redirect_uri is passed with a ';' suffix
			// in mobile apps
			finalRedirectUri = Utils.removeSuffix(finalRedirectUri, ";");

		StringBuilder location = new StringBuilder(finalRedirectUri)//
				.append("#state=");

		// TODO why the hell do i escape path segment style
		// instead of parameter escaping
		if (state != null)
			location.append(Uris.escapeSegment(state));

		try {
			Credentials credentials = login(context);

			location.append("&access_token=")//
					.append(Uris.escapeSegment(credentials.accessToken()))//
					.append("&expires=")//
					.append(credentials.accessTokenExpiresIn())//
					.append("&credentialsId=")//
					.append(Uris.escapeSegment(credentials.id()));

		} catch (SpaceException e) {
			location.append("&error=")//
					.append(Uris.escapeSegment(e.code()))//
					.append("&error_description=")//
					.append(Uris.escapeSegment(e.getMessage()));

		} catch (Throwable t) {
			location.append("&error=internal-server-error")//
					.append("&error_description=")//
					.append(Uris.escapeSegment(t.getMessage()));
		}

		return new Payload(302).withHeader(SpaceHeaders.LOCATION, location.toString());
	}

	// client can directly access linkedin api
	// using spacedog access token since token comes from linkedin
	// this is only request forwarding
	@Deprecated
	@Get("/1/linkedin/me/:fields")
	@Get("/1/linkedin/me/:fields/")
	public Payload get(String fields, Context context) {

		Credentials credentials = SpaceContext.credentials().checkAtLeastUser();

		SpaceResponse response = SpaceRequest.get("/v1/people/~:({fields})")//
				.backend("https://api.linkedin.com")//
				.bearerAuth(credentials.accessToken())//
				.routeParam("fields", fields)//
				.queryParam("format", "json")//
				.go();

		checkLinkedinError(response, "linkedin error fetching your profil");
		return JsonPayload.ok().withObject(response.asJsonObject()).build();
	}

	//
	// implementation
	//

	private void checkLinkedinError(SpaceResponse response, String messageIntro) {

		int httpStatus = response.status();

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
		String code = Check.notNullOrEmpty(context.get("code"), "code");

		CredentialsSettings settings = getCredentialsSettings();
		String redirectUri = context.get("redirect_uri");

		// no redirect_uri in context means
		// redirect_uri = this current resource uri
		// useful when spacedog is directly used as redirect_uri
		if (Strings.isNullOrEmpty(redirectUri))
			redirectUri = spaceUrl(context.uri()).toString();
		else
			// TODO remove this when mikael finds out why
			// the redirect_uri is passed with a ';' suffix
			// in mobile app
			redirectUri = Utils.removeSuffix(redirectUri, ";");

		SpaceResponse response = SpaceRequest.post("/oauth/v2/accessToken")//
				.backend(settings.linkedin.backendUrl)//
				.queryParam("grant_type", "authorization_code")//
				.queryParam("client_id", settings.linkedin.clientId)//
				.queryParam("client_secret", settings.linkedin.clientSecret)//
				.queryParam("redirect_uri", redirectUri)//
				.queryParam("code", code)//
				.go();

		checkLinkedinError(response, "linkedin error fetching access token");

		String accessToken = response.asJsonObject().get("access_token").asText();

		long expiresIn = settings.linkedin.useExpiresIn //
				? response.asJsonObject().get("expires_in").asLong() //
				: CredentialsService.get().getCheckSessionLifetime(context);

		Session session = Session.newSession(accessToken, expiresIn);

		response = SpaceRequest.get("/v1/people/~:(email-address)")//
				.backend("https://api.linkedin.com")//
				.bearerAuth(accessToken)//
				.queryParam("format", "json")//
				.go();

		checkLinkedinError(response, "linkedin error fetching email");
		String email = response.asJsonObject().get("emailAddress").asText();

		CredentialsService credentialsResource = CredentialsService.get();
		Credentials credentials = credentialsResource.getByName(email, false)//
				.orElse(new Credentials(email).addRoles(Roles.user));

		credentials.setCurrentSession(session);
		credentials.email(email);

		boolean isNew = credentials.createdAt() == null;

		if (isNew) {
			if (!settings.guestSignUpEnabled)
				throw Exceptions.forbidden("guest sign up is disabled");

			credentials = credentialsResource.create(credentials);
		} else
			credentials = credentialsResource.update(credentials);

		return credentials;
	}

	private CredentialsSettings getCredentialsSettings() {
		CredentialsSettings settings = SettingsService.get().getAsObject(CredentialsSettings.class);
		Check.notNull(settings.linkedin, "linkedin");
		Check.notNull(settings.linkedin.clientId, "linkedin.clientId");
		Check.notNull(settings.linkedin.clientSecret, "linkedin.clientSecret");
		Check.notNull(settings.linkedin.backendUrl, "linkedin.backendUrl");
		return settings;
	}

	//
	// singleton
	//

	private static LinkedinService singleton = new LinkedinService();

	static LinkedinService get() {
		return singleton;
	}

	private LinkedinService() {
	}
}
