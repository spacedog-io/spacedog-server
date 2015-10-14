/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Collections;

import org.elasticsearch.action.index.IndexResponse;

import com.eclipsesource.json.JsonObject;

import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

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

	static final JsonObject USER_DEFAULT_SCHEMA = SchemaBuilder.builder(USER_TYPE).id("username")
			.add("username", "string").required().add("hashedPassword", "string").required().add("email", "string")
			.required().add("accountId", "string").required().add("groups", "string").build();

	@Get("/login")
	@Get("/login/")
	public Payload login(Context context) {
		try {
			AdminResource.checkUserCredentialsOnly(context);
			return Payload.ok();
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Get("/logout")
	@Get("/logout/")
	public Payload logout(Context context) {
		try {
			AdminResource.checkUserCredentialsOnly(context);
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
			/**
			 * TODO adjust this. Admin should be able to sign up users. But what
			 * backend id if many in account? Backend key should be able to sign
			 * up users. Should common users be able to?
			 */
			Credentials credentials = AdminResource.checkCredentials(context);
			JsonObject input = JsonObject.readFrom(body);

			User user = new User();
			user.username = input.getString("username", null);
			user.email = input.getString("email", null);
			String password = input.getString("password", null);
			User.checkPasswordValidity(password);
			user.hashedPassword = User.hashPassword(password);
			user.groups = Collections.singletonList(credentials.getBackendId());
			user.checkUserInputValidity();

			IndexResponse response = DataResource.get().createInternal(credentials.getBackendId(), USER_TYPE,
					// TODO find something better to avoid to many object format
					// transformations
					JsonObject.readFrom(getObjectMapper().writeValueAsString(user)), credentials.getName());

			return saved(true, "/v1", USER_TYPE, response.getId(), response.getVersion());

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
		JsonObject schema = SchemaValidator.validate(USER_TYPE, USER_DEFAULT_SCHEMA);
		return SchemaTranslator.translate(USER_TYPE, schema).toString();
	}

}
