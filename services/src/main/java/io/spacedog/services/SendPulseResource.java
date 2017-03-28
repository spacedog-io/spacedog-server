package io.spacedog.services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.model.SendPulseSettings;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceResponse;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.SpaceException;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/1/sendpulse")
public class SendPulseResource extends Resource {

	//
	// Routes
	//

	@Get("/push/websites")
	@Get("/push/websites/")
	public Payload getAll(Context context) {
		SendPulseSettings settings = SettingsResource.get().load(SendPulseSettings.class);
		SpaceContext.getCredentials().checkRoles(settings.authorizedRoles);

		String body = SpaceRequest.get("/push/websites")//
				.baseUrl("https://api.sendpulse.com")//
				.bearerAuth(accessToken(settings))//
				.go()//
				.string();

		return JsonPayload.json(body, 200);
	}

	@Post("/push/tasks")
	@Post("/push/tasks/")
	public Payload postPush(Context context) {

		SendPulseSettings settings = SettingsResource.get().load(SendPulseSettings.class);
		SpaceContext.getCredentials().checkRoles(settings.authorizedRoles);

		SpaceRequest request = SpaceRequest.post("/push/tasks")//
				.baseUrl("https://api.sendpulse.com")//
				.bearerAuth(accessToken(settings));

		FormQuery formQuery = Resource.formQuery(context);

		for (String key : formQuery.keys())
			request.formField(key, formQuery.get(key));

		SpaceResponse response = request.go();
		checkSendPulseError(response, "error creating SendPulse push task");
		return JsonPayload.json(response.objectNode(), 200);
	}

	private String accessToken(SendPulseSettings settings) {

		if (settings.hasExpired()) {

			if (Strings.isNullOrEmpty(settings.clientId))
				throw Exceptions.illegalArgument("invalid SendPulse settings");

			SpaceResponse response = SpaceRequest.post("/oauth/access_token")//
					.baseUrl("https://api.sendpulse.com")//
					.formField("grant_type", "client_credentials")//
					.formField("client_id", settings.clientId)//
					.formField("client_secret", settings.clientSecret)//
					.go();

			checkSendPulseError(response, "error authenticating with SendPulse");
			settings.setToken(response.objectNode());
			SettingsResource.get().save(settings);
		}

		return settings.accessToken;
	}

	private void checkSendPulseError(SpaceResponse response, String messageIntro) {

		int httpStatus = response.status();

		if (httpStatus != 200) {

			if (!response.isJson())
				throw new SpaceException(httpStatus, messageIntro);

			ObjectNode body = response.objectNode();
			String code = "sendpulse-" + body.path("error_code").asInt(0);
			String message = messageIntro + ": " + body.path("message").asText();
			throw new SpaceException(code, httpStatus, message);
		}
	}

	//
	// singleton
	//

	private static SendPulseResource singleton = new SendPulseResource();

	static SendPulseResource get() {
		return singleton;
	}

	private SendPulseResource() {
		SettingsResource.get().registerSettingsClass(SendPulseSettings.class);
	}
}
