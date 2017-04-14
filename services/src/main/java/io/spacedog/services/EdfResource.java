package io.spacedog.services;

import io.spacedog.model.CredentialsSettings;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceResponse;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
import io.spacedog.utils.Credentials.Session;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.SpaceHeaders;
import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class EdfResource extends Resource {

	//
	// Routes
	//

	@Post("/1/service/oauth/accessToken")
	@Post("/1/service/oauth/accessToken/")
	public Payload getLogin(Context context) {
		Credentials credentials = login(context);

		return JsonPayload.json(//
				JsonPayload.builder()//
						.put(FIELD_ACCESS_TOKEN, credentials.accessToken()) //
						.put(FIELD_EXPIRES_IN, credentials.accessTokenExpiresIn()) //
						.node(FIELD_CREDENTIALS, credentials.toJson()));
	}

	//
	// implementation
	//

	private Credentials login(Context context) {

		String backendId = SpaceContext.backendId();
		String code = Check.notNullOrEmpty(context.get("code"), "code");
		String username = Check.notNullOrEmpty(context.get("username"), "username");
		String redirectUri = Check.notNullOrEmpty(context.get("redirect_uri"), "redirect_uri");

		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);
		if (settings.oauth == null)
			throw Exceptions.illegalArgument("credentials OAuth settings are required");

		SpaceResponse response = SpaceRequest.post("/ws/iOAuthGetToken/do")//
				.backend("https://noefy5jt.noe.edf.fr:5641")//
				.basicAuth(settings.oauth.clientId, settings.oauth.clientSecret)//
				.setHeader(SpaceHeaders.ACCEPT, "application/json")//
				.bodyJson("grant_type", "authorization_code", //
						"client_id", settings.oauth.clientId, //
						"redirect_uri", redirectUri, "code", code)//
				.go();

		checkEdfOAuthError(response, "EDF OAuth error fetching access token");

		String accessToken = response.objectNode().get("access_token").asText();

		long expiresIn = settings.oauth.useExpiresIn //
				? response.objectNode().get("expires_in").asLong() //
				: CredentialsResource.get().getCheckSessionLifetime(context);

		Session session = Session.newSession(accessToken, expiresIn);

		CredentialsResource credentialsResource = CredentialsResource.get();
		Credentials credentials = credentialsResource.getByName(backendId, username, false)//
				.orElse(new Credentials(backendId, username, Level.USER));

		credentials.setCurrentSession(session);

		if (credentials.id() == null) {
			credentials.email(username);
			credentials = credentialsResource.create(credentials);
		} else
			credentials = credentialsResource.update(credentials);

		return credentials;
	}

	private void checkEdfOAuthError(SpaceResponse response, String messageIntro) {

		int httpStatus = response.status();

		if (httpStatus >= 400) {
			throw Exceptions.space(httpStatus, messageIntro + ": " //
					+ response.string());
		}
	}

	//
	// singleton
	//

	private static EdfResource singleton = new EdfResource();

	static EdfResource get() {
		return singleton;
	}

	private EdfResource() {
	}
}
