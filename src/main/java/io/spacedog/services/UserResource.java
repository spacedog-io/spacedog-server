/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.elasticsearch.action.index.IndexResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/v1")
public class UserResource extends AbstractResource {

	//
	// singleton
	//

	private static AbstractResource singleton = new UserResource();

	static AbstractResource get() {
		return singleton;
	}

	private UserResource() {
	}

	//
	// default user type and schema
	//

	static final String USER_TYPE = "user";

	public static SchemaBuilder2 getDefaultUserSchemaBuilder() {
		return SchemaBuilder2.builder(USER_TYPE, "username")//
				.stringProperty("username", true)//
				.stringProperty("hashedPassword", true)//
				.stringProperty("email", true)//
				.stringProperty("accountId", true)//
				.stringProperty("groups", false, true);
	}

	public static ObjectNode getDefaultUserSchema() {
		return getDefaultUserSchemaBuilder().build();
	}

	//
	// services
	//

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

			ObjectNode user = Json.readObjectNode(body);
			checkNotNullOrEmpty(user, "username", USER_TYPE);
			checkNotNullOrEmpty(user, "email", USER_TYPE);
			checkNotPresent(user, "hashedPassword", USER_TYPE);
			String password = checkNotNullOrEmpty(user, "password", USER_TYPE).asText();
			UserUtils.checkPasswordValidity(password);

			user.remove("password");
			user.put("hashedPassword", UserUtils.hashPassword(password));
			user.putArray("groups").add(credentials.getBackendId());

			IndexResponse response = DataResource.get().createInternal(credentials.getBackendId(), USER_TYPE, user,
					credentials.getName());

			return saved(true, "/v1", USER_TYPE, response.getId(), response.getVersion());

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	protected ObjectNode checkObjectNode(JsonNode json) {
		if (!json.isObject())
			throw new IllegalArgumentException(String.format("json not an object but [%s]", json.getNodeType()));
		return (ObjectNode) json;
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
		JsonNode schema = SchemaValidator.validate(USER_TYPE, getDefaultUserSchema());
		return SchemaTranslator.translate(USER_TYPE, schema).toString();
	}

}
