package io.spacedog.services;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.core.Json8;
import io.spacedog.model.CredentialsSettings;
import io.spacedog.model.CredentialsSettings.OAuthSettings;
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

		OAuthSettings oauthSetting = checkOAuthSetting();
		Session session = newEdfSession(context, oauthSetting);
		String username = getEdfUsername(session, oauthSetting);
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

	private OAuthSettings checkOAuthSetting() {
		OAuthSettings oauthSettings = SettingsResource.get()//
				.load(CredentialsSettings.class).oauth;

		if (oauthSettings == null)
			throw Exceptions.illegalArgument("credentials OAuth settings are required");
		if (oauthSettings.clientId == null)
			throw Exceptions.illegalArgument("credentials OAuth [clientId] is required");
		if (oauthSettings.clientSecret == null)
			throw Exceptions.illegalArgument("credentials OAuth [clientSecret] is required");
		if (oauthSettings.backendUrl == null)
			throw Exceptions.illegalArgument("credentials OAuth [backend] is required");

		return oauthSettings;
	}

	String getEdfUsername(Session session, OAuthSettings oauthSetting) {
		SpaceResponse response = SpaceRequest.get("/gardian/oauth2/v2/tokeninfo")//
				.backend(oauthSetting.backendUrl)//
				.bearerAuth(session.accessToken())//
				.go();

		checkEdfOAuthError(response, "EDF OAuth v2 error fetching username");
		return response.getString("uid");
	}

	private Session newEdfSession(Context context, OAuthSettings oauthSetting) {
		String code = Check.notNullOrEmpty(context.get("code"), "code");
		String redirectUri = Check.notNullOrEmpty(context.get("redirect_uri"), "redirect_uri");

		SpaceResponse response = SpaceRequest.post("/gardian/oauth2/v2/token")//
				.backend(oauthSetting.backendUrl)//
				.basicAuth(oauthSetting.clientId, oauthSetting.clientSecret)//
				.bodyJson("grant_type", "authorization_code", "code", code, //
						"client_id", oauthSetting.clientId, "redirect_uri", redirectUri)//
				.go();

		checkEdfOAuthError(response, "EDF OAuth v2 error fetching access token");
		String accessToken = response.getString("access_token");
		long expiresIn = oauthSetting.useExpiresIn //
				? response.get("expires_in").asLong() //
				: CredentialsResource.get().getCheckSessionLifetime(context);

		return Session.newSession(accessToken, expiresIn);
	}

	private void checkEdfOAuthError(SpaceResponse response, String message) {

		if (response.status() >= 400) {

			ObjectNode details = response.isJson() ? response.asJsonObject() //
					: Json8.object("description", response.asString());

			throw Exceptions.space(response.status(), message)//
					.code("edf-oauth-error").details(details);
		}
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
