/**
 * © David Attias 2015
 */
package io.spacedog.services;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Schema;
import io.spacedog.utils.SpaceHeaders;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class UserResource extends Resource {

	//
	// User constants and schema
	//

	public static final String TYPE = "user";

	public static Schema getDefaultUserSchema() {
		return Schema.builder(TYPE)//
				.id(USERNAME)//
				.string(USERNAME)//
				.string(EMAIL)//
				.build();
	}

	//
	// Routes
	//

	@Get("/1/user")
	@Get("/1/user/")
	public Payload getAll(Context context) {
		return DataResource.get().getByType(TYPE, context);
	}

	@Post("/1/user")
	@Post("/1/user/")
	@Post("/1/data/user")
	@Post("/1/data/user/")
	public Payload signUp(String body, Context context) {
		ObjectNode node = Json.readObject(body);
		Credentials credentials = CredentialsResource.get()//
				.create(SpaceContext.target(), Level.USER, node, true);
		SpaceContext.setCredentials(credentials);

		node.remove(PASSWORD);
		body = node.toString();
		DataResource.get().post(TYPE, body, context);

		JsonBuilder<ObjectNode> builder = JsonPayload.builder(true, //
				credentials.backendId(), "/1", TYPE, credentials.name());

		if (credentials.passwordResetCode() != null)
			builder.put(PASSWORD_RESET_CODE, credentials.passwordResetCode());

		return JsonPayload.json(builder, HttpStatus.CREATED)//
				.withHeader(SpaceHeaders.SPACEDOG_OBJECT_ID, credentials.name());
	}

	@Get("/1/user/:username")
	@Get("/1/user/:username/")
	public Payload get(String username, Context context) {
		return DataResource.get().getById(TYPE, username, context);
	}

	@Put("/1/user/:username")
	@Put("/1/user/:username/")
	public Payload put(String username, String jsonBody, Context context) {
		return DataResource.get().put(TYPE, username, jsonBody, context);
	}

	@Delete("/1/user/:username")
	@Delete("/1/user/:username/")
	@Delete("/1/data/user/:username")
	@Delete("/1/data/user/:username/")
	public Payload delete(String username, Context context) {
		CredentialsResource.get().deleteById(//
				Credentials.toLegacyId(SpaceContext.target(), username));
		return DataResource.get().deleteById(TYPE, username, context);
	}

	@Delete("/1/user/:username/password")
	@Delete("/1/user/:username/password")
	public Payload deletePassword(String username, Context context) {
		return CredentialsResource.get().deletePassword(//
				Credentials.toLegacyId(SpaceContext.target(), username), //
				context);
	}

	@Post("/1/user/:username/password")
	@Post("/1/user/:username/password")
	public Payload postPassword(String username, Context context) {
		return CredentialsResource.get().postPassword(//
				Credentials.toLegacyId(SpaceContext.target(), username), //
				context);
	}

	@Put("/1/user/:username/password")
	@Put("/1/user/:username/password")
	public Payload putPassword(String username, Context context) {
		return CredentialsResource.get().putPassword(//
				Credentials.toLegacyId(SpaceContext.target(), username), //
				context);
	}

	//
	// singleton
	//

	private static UserResource singleton = new UserResource();

	static UserResource get() {
		return singleton;
	}

	private UserResource() {
	}
}