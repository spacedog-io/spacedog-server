package io.spacedog.services;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
import io.spacedog.utils.CredentialsSettings;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceHeaders;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class LinkedinResource {

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

		JsonBuilder<ObjectNode> builder = JsonPayload//
				.builder(credentials.isBrandNew(), credentials.backendId(), "/1", //
						CredentialsResource.TYPE, credentials.id(), credentials.version())//
				.put(CredentialsResource.ACCESS_TOKEN, credentials.accessToken())//
				.put(CredentialsResource.EXPIRES_IN, credentials.accessTokenExpiresIn());

		return JsonPayload.json(builder, HttpStatus.CREATED)//
				.withHeader(SpaceHeaders.SPACEDOG_OBJECT_ID, credentials.id());
	}

	@Get("/1/login/linkedin/redirect")
	@Get("/1/login/linkedin/redirect/")
	public Payload getRedirectLogin(Context context) {

		Credentials credentials = login(context);
		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);

		String redirectUri = context.get("redirect_uri");
		if (Strings.isNullOrEmpty(redirectUri))
			redirectUri = settings.linkedinRedirectUri;
		Check.notNullOrEmpty(redirectUri, "redirect_uri");
		String state = context.get("state");
		Check.notNullOrEmpty(state, "state");

		StringBuilder location = new StringBuilder(redirectUri)//
				.append("#state=").append(state)//
				.append("&access_token=").append(credentials.accessToken())//
				.append("&expires=").append(credentials.accessTokenExpiresIn());

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
		if (response.httpResponse().getStatus() >= 400) {

			String details = response.has("error_description") //
					? response.getString("error_description")//
					: "no linkedin error description";

			throw Exceptions.space(response.httpResponse().getStatus(), //
					"%s: %s", messageIntro, details);
		}
	}

	//
	// Implementation
	//

	private Credentials login(Context context) {
		String backendId = SpaceContext.checkCredentials().backendId();
		String code = Check.notNullOrEmpty(context.get("code"), "code");

		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);
		if (Strings.isNullOrEmpty(settings.linkedinId))
			throw Exceptions.illegalArgument("no linkedin client id found in credentials settings");

		String redirectUri = context.get("redirect_uri");
		if (Strings.isNullOrEmpty(redirectUri))
			redirectUri = settings.linkedinRedirectUri;
		Check.notNullOrEmpty(redirectUri, "redirect_uri");

		DateTime expiresAt = DateTime.now();

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
		expiresAt = expiresAt.plus(response.objectNode().get("expires_in").asLong());

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

		credentials.email(email);
		credentials.setExternalAccessToken(accessToken, expiresAt);

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
