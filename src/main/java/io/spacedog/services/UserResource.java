package io.spacedog.services;

import java.util.Collections;

import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

import com.eclipsesource.json.JsonObject;

@Prefix("/v1")
public class UserResource extends AbstractResource {

	// singleton begin

	private static UserResource singleton = new UserResource();

	static UserResource get() {
		return singleton;
	}

	private UserResource() {
	}

	// singleton end

	static final String USER_TYPE = "user";

	static final JsonObject USER_DEFAULT_SCHEMA = SchemaBuilder
			.builder(USER_TYPE).id("username").add("username", "string")
			.required().add("password", "string").required()
			.add("email", "string").required().add("accountId", "string")
			.required().add("groups", "string").build();

	@Get("/login")
	@Get("/login/")
	public Payload login(Context context) {
		try {
			AccountResource.checkCredentials(context);
			return Payload.ok();
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Get("/logout")
	@Get("/logout/")
	public Payload logout(Context context) {
		try {
			AccountResource.checkCredentials(context);
			return Payload.ok();
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Get("/user")
	@Get("/user/")
	public Payload getAll(Context context) {
		return DataResource.get().search(USER_TYPE, context);
	}

	@Post("/user")
	@Post("/user/")
	public Payload signUp(String body, Context context) {
		try {
			JsonObject input = JsonObject.readFrom(body);
			Credentials credentials = AccountResource.checkCredentials(context);

			User user = new User();
			user.username = input.getString("username", null);
			user.email = input.getString("email", null);
			user.password = input.getString("password", null);
			user.groups = Collections.singletonList(credentials.getAccountId());
			user.checkUserInputValidity();

			String userId = DataResource.get().createInternal(
					credentials.getAccountId(), USER_TYPE, user.toJsonObject(),
					credentials.getId());

			return created("/v1", USER_TYPE, userId);

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Get("/user/:id")
	@Get("/user/:id/")
	public Payload get(String id, Context context) {
		return DataResource.get().get(USER_TYPE, id, context);
	}

	@Put("/user/:id")
	@Put("/user/:id/")
	public Payload update(String id, String jsonBody, Context context) {
		return DataResource.get().update(USER_TYPE, id, jsonBody, context);
	}

	@Delete("/user/:id")
	@Delete("/user/:id/")
	public Payload delete(String id, Context context) {
		return DataResource.get().delete(USER_TYPE, id, context);
	}

	public static String getDefaultUserMapping() {
		JsonObject schema = SchemaValidator.validate(USER_TYPE,
				USER_DEFAULT_SCHEMA);
		return SchemaTranslator.translate(USER_TYPE, schema).toString();
	}

}
