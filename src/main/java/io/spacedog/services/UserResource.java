/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;
import java.util.UUID;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.script.ScriptService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/v1")
public class UserResource extends AbstractResource {

	private static final String ACCOUNT_ID = "accountId";
	private static final String GROUPS = "groups";
	private static final String EMAIL = "email";
	private static final String USERNAME = "username";
	private static final String HASHED_PASSWORD = "hashedPassword";
	private static final String PASSWORD_RESET_CODE = "passwordResetCode";

	//
	// singleton
	//

	private static UserResource singleton = new UserResource();

	static UserResource get() {
		return singleton;
	}

	private UserResource() {
	}

	//
	// default user type and schema
	//

	static final String USER_TYPE = "user";

	public static SchemaBuilder2 getDefaultUserSchemaBuilder() {
		return SchemaBuilder2.builder(USER_TYPE, USERNAME)//
				.stringProperty(USERNAME, true)//
				.stringProperty(HASHED_PASSWORD, false)//
				.stringProperty(PASSWORD_RESET_CODE, false)//
				.stringProperty(EMAIL, true)//
				.stringProperty(ACCOUNT_ID, true)//
				.stringProperty(GROUPS, false, true);
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
		return DataResource.get().externalGetAll(USER_TYPE, context);
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
			checkNotNullOrEmpty(user, USERNAME, USER_TYPE);
			checkNotNullOrEmpty(user, EMAIL, USER_TYPE);
			checkNotPresent(user, HASHED_PASSWORD, USER_TYPE);
			user.putArray(GROUPS).add(credentials.getBackendId());

			// password management

			JsonNode password = user.remove("password");
			Optional<String> passwordResetCode = Optional.empty();
			if (password == null || password.equals(NullNode.getInstance())) {
				passwordResetCode = Optional.of(UUID.randomUUID().toString());
				user.put(PASSWORD_RESET_CODE, passwordResetCode.get());
			} else {
				UserUtils.checkPasswordValidity(password.asText());
				user.put(HASHED_PASSWORD, UserUtils.hashPassword(password.asText()));
			}

			IndexResponse response = DataResource.get().createInternal(credentials.getBackendId(), USER_TYPE, user,
					credentials.getName());

			JsonBuilder<ObjectNode> savedBuilder = initSavedBuilder("/v1", USER_TYPE, response.getId(),
					response.getVersion());

			passwordResetCode.ifPresent(code -> savedBuilder.put(PASSWORD_RESET_CODE, code));

			return new Payload(JSON_CONTENT, savedBuilder.toString(), HttpStatus.CREATED)
					.withHeader(AbstractResource.HEADER_OBJECT_ID, response.getId());

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

	@Delete("/user/:id/password")
	@Delete("/user/:id/password")
	public Payload deletePassword(String id, Context context) {
		try {
			Account account = AdminResource.checkAdminCredentialsOnly(context);

			UpdateResponse response = SpaceDogServices.getElasticClient()
					.prepareUpdate(account.backendId, UserResource.USER_TYPE, id)//
					.setScript("ctx._source.remove('hashedPassword');ctx._source.passwordResetCode=code;",
							ScriptService.ScriptType.INLINE)//
					.addScriptParam("code", UUID.randomUUID().toString())//
					.get();

			return saved(false, "/v1/user", response.getType(), response.getId(), response.getVersion());

		} catch (Throwable throwable) {
			return error(throwable);
		}

	}

	@Post("/user/:id/password")
	@Post("/user/:id/password")
	public Payload initPassword(String id, String body, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// TODO do we need a password reset expire date to limit the reset
			// time scope
			String passwordResetCode = context.query().get(PASSWORD_RESET_CODE);
			if (Strings.isNullOrEmpty(passwordResetCode))
				throw new IllegalArgumentException("password reset code is empty");

			String password = Json.readJsonNode(body).asText();
			UserUtils.checkPasswordValidity(password);

			GetResponse getResponse = SpaceDogServices.getElasticClient()
					.prepareGet(credentials.getBackendId(), USER_TYPE, id).get();

			if (!getResponse.isExists())
				throw new NotFoundException(credentials.getBackendId(), USER_TYPE, id);

			ObjectNode user = Json.readObjectNode(getResponse.getSourceAsString());

			if (user.get(HASHED_PASSWORD) != null || user.get(PASSWORD_RESET_CODE) == null)
				throw new IllegalArgumentException(String.format("user [%s] password has not been deleted", id));

			if (!passwordResetCode.equals(user.get(PASSWORD_RESET_CODE).asText()))
				throw new IllegalArgumentException(
						String.format("invalid password reset code [%s]", passwordResetCode));

			user.remove(PASSWORD_RESET_CODE);
			user.put(HASHED_PASSWORD, UserUtils.hashPassword(password));

			IndexResponse indexResponse = DataResource.get().fullUpdateInternal(USER_TYPE, id, 0, user, credentials);

			return saved(false, "/v1", USER_TYPE, id, indexResponse.getVersion());

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Put("/user/:id/password")
	@Put("/user/:id/password")
	public Payload updatePassword(String id, String body, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			if (credentials.isAdmin() || (credentials.isUser() && id.equals(credentials.getName()))) {

				String password = Json.readJsonNode(body).asText();
				UserUtils.checkPasswordValidity(password);

				ObjectNode update = Json.startObject()//
						.put(HASHED_PASSWORD, UserUtils.hashPassword(password))//
						.putNode(PASSWORD_RESET_CODE, NullNode.getInstance())//
						.build();

				UpdateResponse response = SpaceDogServices.getElasticClient()
						.prepareUpdate(credentials.getBackendId(), UserResource.USER_TYPE, id).setDoc(update.toString())
						.get();

				return saved(false, "/v1/user", response.getType(), response.getId(), response.getVersion());

			} else
				throw new AuthenticationException("only the owner or admin users can update user passwords");

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	public static String getDefaultUserMapping() {
		JsonNode schema = SchemaValidator.validate(USER_TYPE, getDefaultUserSchema());
		return SchemaTranslator.translate(USER_TYPE, schema).toString();
	}

}
