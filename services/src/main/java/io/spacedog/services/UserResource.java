/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;
import java.util.UUID;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.SchemaBuilder2;
import io.spacedog.utils.Usernames;
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

	public static final String ENDPOINT_ARN = "endpointArn";
	public static final String PASSWORD = "password";
	public static final String ACCOUNT_ID = "accountId";
	public static final String GROUPS = "groups";
	public static final String EMAIL = "email";
	public static final String USERNAME = "username";
	public static final String HASHED_PASSWORD = "hashedPassword";
	public static final String PASSWORD_RESET_CODE = "passwordResetCode";

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
				.stringProperty(GROUPS, false, true)//
				.stringProperty(ENDPOINT_ARN, false, false);
	}

	public static ObjectNode getDefaultUserSchema() {
		return getDefaultUserSchemaBuilder().build();
	}

	public static String getDefaultUserMapping() {
		JsonNode schema = SchemaValidator.validate(USER_TYPE, getDefaultUserSchema());
		return SchemaTranslator.translate(USER_TYPE, schema).toString();
	}

	//
	// Routes
	//

	@Get("/login")
	@Get("/login/")
	public Payload login(Context context) {
		SpaceContext.checkUserCredentials();
		return Payloads.success();
	}

	@Get("/logout")
	@Get("/logout/")
	public Payload logout(Context context) {
		SpaceContext.checkUserCredentials();
		return Payloads.success();
	}

	@Get("/user")
	@Get("/user/")
	public Payload getAll(Context context) {
		return DataResource.get().getByType(USER_TYPE, context);
	}

	@Delete("/user")
	@Delete("/user/")
	public Payload deleteAll(Context context) {
		return DataResource.get().deleteByType(USER_TYPE, context);
	}

	@Post("/user")
	@Post("/user/")
	public Payload signUp(String body, Context context) {
		/**
		 * TODO adjust this. Admin should be able to sign up users. But what
		 * backend id if many in account? Backend key should be able to sign up
		 * users. Should common users be able to?
		 */
		Credentials credentials = SpaceContext.checkCredentials();

		ObjectNode user = Json.readObjectNode(body);
		String username = Json.checkStringNotNullOrEmpty(user, USERNAME);
		Usernames.checkIfValid(username);
		Json.checkStringNotNullOrEmpty(user, EMAIL);
		Json.checkNotPresent(user, HASHED_PASSWORD, USER_TYPE);
		user.putArray(GROUPS).add(credentials.backendId());

		// password management

		JsonNode password = user.remove(PASSWORD);
		Optional<String> passwordResetCode = Optional.empty();
		if (password == null || password.equals(NullNode.getInstance())) {
			passwordResetCode = Optional.of(UUID.randomUUID().toString());
			user.put(PASSWORD_RESET_CODE, passwordResetCode.get());
		} else {
			user.put(HASHED_PASSWORD, Passwords.checkAndHash(password.asText()));
		}

		IndexResponse response = ElasticHelper.get().createObject(//
				credentials.backendId(), USER_TYPE, username, user, credentials.name());

		JsonBuilder<ObjectNode> savedBuilder = Payloads.savedBuilder(true, "/v1", USER_TYPE, response.getId(),
				response.getVersion());

		passwordResetCode.ifPresent(code -> savedBuilder.put(PASSWORD_RESET_CODE, code));

		return Payloads.json(savedBuilder, HttpStatus.CREATED)//
				.withHeader(Payloads.HEADER_OBJECT_ID, response.getId());
	}

	@Get("/user/:id")
	@Get("/user/:id/")
	public Payload get(String id, Context context) {
		return DataResource.get().getById(USER_TYPE, id, context);
	}

	@Put("/user/:id")
	@Put("/user/:id/")
	public Payload put(String id, String jsonBody, Context context) {
		return DataResource.get().put(USER_TYPE, id, jsonBody, context);
	}

	@Delete("/user/:id")
	@Delete("/user/:id/")
	public Payload delete(String id, Context context) {
		return DataResource.get().deleteById(USER_TYPE, id, context);
	}

	@Delete("/user/:id/password")
	@Delete("/user/:id/password")
	public Payload deletePassword(String id, Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials();

		// UpdateResponse response =
		// Start.get().getElasticClient().prepareUpdate(account.backendId,
		// UserResource.USER_TYPE, id)//
		// .setScript(new Script(
		// "ctx._source.remove('hashedPassword');ctx._source.passwordResetCode=code;",//
		// ScriptType.INLINE,//
		// "groovy",//
		// Maps.))
		// .addScriptParam("code", UUID.randomUUID().toString())//
		// .get();

		ObjectNode user = ElasticHelper.get().getObject(credentials.backendId(), USER_TYPE, id)//
				.orElseThrow(() -> NotFoundException.object(USER_TYPE, id));

		String resetCode = UUID.randomUUID().toString();
		user.remove(HASHED_PASSWORD);
		user.put(PASSWORD_RESET_CODE, resetCode);

		long newVersion = ElasticHelper.get().updateObject(credentials.backendId(), user, credentials.name())
				.getVersion();

		return Payloads.json(//
				Payloads.savedBuilder(false, "/v1", USER_TYPE, id, newVersion)//
						.put(PASSWORD_RESET_CODE, resetCode));
	}

	@Post("/user/:id/password")
	@Post("/user/:id/password")
	public Payload postPassword(String id, Context context) {
		Credentials credentials = SpaceContext.checkCredentials();

		// TODO do we need a password reset expire date to limit the reset
		// time scope
		String passwordResetCode = context.query().get(PASSWORD_RESET_CODE);
		if (Strings.isNullOrEmpty(passwordResetCode))
			throw new IllegalArgumentException("password reset code is empty");

		String password = context.get(PASSWORD);
		Passwords.checkIfValid(password);

		GetResponse getResponse = Start.get().getElasticClient()//
				.prepareGet(credentials.backendId(), USER_TYPE, id)//
				.get();

		if (!getResponse.isExists())
			throw NotFoundException.object(USER_TYPE, id);

		ObjectNode user = Json.readObjectNode(getResponse.getSourceAsString());

		if (user.get(HASHED_PASSWORD) != null || user.get(PASSWORD_RESET_CODE) == null)
			throw new IllegalArgumentException(String.format("user [%s] password has not been deleted", id));

		if (!passwordResetCode.equals(user.get(PASSWORD_RESET_CODE).asText()))
			throw new IllegalArgumentException(String.format("invalid password reset code [%s]", passwordResetCode));

		user.remove(PASSWORD_RESET_CODE);
		user.put(HASHED_PASSWORD, Passwords.checkAndHash(password));

		IndexResponse indexResponse = ElasticHelper.get().updateObject(credentials.backendId(), USER_TYPE, id, 0, user,
				credentials.name());

		return Payloads.saved(false, "/v1", USER_TYPE, id, indexResponse.getVersion());
	}

	@Put("/user/:id/password")
	@Put("/user/:id/password")
	public Payload putPassword(String id, Context context) {
		Credentials credentials = SpaceContext.checkCredentials();

		if (credentials.isAdminAuthenticated()
				|| (credentials.isUserAuthenticated() && id.equals(credentials.name()))) {

			String password = context.get(PASSWORD);
			Passwords.checkIfValid(password);

			ObjectNode update = Json.objectBuilder()//
					.put(HASHED_PASSWORD, Passwords.checkAndHash(password))//
					.node(PASSWORD_RESET_CODE, NullNode.getInstance())//
					.build();

			UpdateResponse response = Start.get().getElasticClient()
					.prepareUpdate(credentials.backendId(), UserResource.USER_TYPE, id).setDoc(update.toString()).get();

			return Payloads.saved(false, "/v1/user", response.getType(), response.getId(), response.getVersion());

		} else
			throw new AuthenticationException("only the owner or admin users can update user passwords");
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
