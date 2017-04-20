package io.spacedog.services;

import io.spacedog.model.CredentialsSettings;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceResponse;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
import io.spacedog.utils.Credentials.Session;
import io.spacedog.utils.Exceptions;
import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class EdfResourceV2 extends Resource {

	//
	//
	//

	private static final String PAAS_PP_BACKEND_URL = "https://paas-pp.edf.fr";

	//
	// Routes
	//

	@Post("/1/service/oauth/v2/accessToken")
	@Post("/1/service/oauth/v2/accessToken/")
	public Payload getLoginV2(Context context) {
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

		Session session = newEdfSession(context);
		String username = getEdfUsername(session);
		String backendId = SpaceContext.backendId();

		CredentialsResource credentialsResource = CredentialsResource.get();
		Credentials credentials = credentialsResource.getByName(backendId, username, false)//
				.orElseGet(() -> new Credentials(backendId, username, Level.USER));

		credentials.setCurrentSession(session);

		if (credentials.id() == null)
			credentials = credentialsResource.create(credentials);
		else
			credentials = credentialsResource.update(credentials);

		return credentials;
	}

	String getEdfUsername(Session session) {
		SpaceResponse response = SpaceRequest.get("/gardian/oauth2/v2/tokeninfo")//
				.backend(PAAS_PP_BACKEND_URL)//
				.bearerAuth(session.accessToken())//
				.go();

		checkEdfOAuthError(response, "EDF OAuth v2 error fetching username");
		return response.getString("uid");
	}

	private Session newEdfSession(Context context) {
		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);
		if (settings.oauth == null)
			throw Exceptions.illegalArgument("credentials OAuth settings are required");

		String code = Check.notNullOrEmpty(context.get("code"), "code");
		String redirectUri = Check.notNullOrEmpty(context.get("redirect_uri"), "redirect_uri");

		SpaceResponse response = SpaceRequest.post("/gardian/oauth2/v2/token")//
				.backend(PAAS_PP_BACKEND_URL)//
				.basicAuth(settings.oauth.clientId, settings.oauth.clientSecret)//
				.bodyJson("grant_type", "authorization_code", "code", code, //
						"client_id", settings.oauth.clientId, "redirect_uri", redirectUri)//
				.go();

		checkEdfOAuthError(response, "EDF OAuth v2 error fetching access token");
		String accessToken = response.getString("access_token");
		long expiresIn = settings.oauth.useExpiresIn //
				? response.get("expires_in").asLong() //
				: CredentialsResource.get().getCheckSessionLifetime(context);

		return Session.newSession(accessToken, expiresIn);
	}

	private void checkEdfOAuthError(SpaceResponse response, String message) {

		if (response.status() >= 400)
			throw Exceptions.space(response.status(), message)//
					.code("edf-oauth-error").details(response.asJsonObject());
	}

	//
	// singleton
	//

	private static EdfResourceV2 singleton = new EdfResourceV2();

	static EdfResourceV2 get() {
		return singleton;
	}

	private EdfResourceV2() {
	}
}
