/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.base.Strings;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
	// Routes
	//

	@Get("/login")
	@Get("/login/")
	public Payload login(Context context) throws JsonParseException, JsonMappingException, IOException {
		AdminResource.checkUserCredentialsOnly(context);
		return Payload.ok();
	}

	@Get("/logout")
	@Get("/logout/")
	public Payload logout(Context context) throws JsonParseException, JsonMappingException, IOException {
		AdminResource.checkUserCredentialsOnly(context);
		return Payload.ok();
	}

	@Get("/user")
	@Get("/user/")
	public Payload getAll(Context context)
			throws NotFoundException, JsonProcessingException, InterruptedException, ExecutionException, IOException {
		return DataResource.get().getAllForType(USER_TYPE, context);
	}

	@Delete("/user")
	@Delete("/user/")
	public Payload deleteAll(Context context)
			throws NotFoundException, JsonProcessingException, InterruptedException, ExecutionException, IOException {
		return DataResource.get().deleteForType(USER_TYPE, context);
	}

	@Post("/user")
	@Post("/user/")
	public Payload signUp(String body, Context context) throws JsonParseException, JsonMappingException, IOException {
		/**
		 * TODO adjust this. Admin should be able to sign up users. But what
		 * backend id if many in account? Backend key should be able to sign up
		 * users. Should common users be able to?
		 */
		Credentials credentials = AdminResource.checkCredentials(context);

		ObjectNode user = Json.readObjectNode(body);
		checkStringNotNullOrEmpty(user, USERNAME);
		checkStringNotNullOrEmpty(user, EMAIL);
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

		IndexResponse response = ElasticHelper.get().createObject(credentials.getBackendId(), USER_TYPE, user,
				credentials.getName());

		JsonBuilder<ObjectNode> savedBuilder = PayloadHelper.savedBuilder("/v1", USER_TYPE, response.getId(), response.getVersion());

		passwordResetCode.ifPresent(code -> savedBuilder.put(PASSWORD_RESET_CODE, code));

		return PayloadHelper.json(savedBuilder, HttpStatus.CREATED)//
				.withHeader(PayloadHelper.HEADER_OBJECT_ID, response.getId());
	}

	@Get("/user/:id")
	@Get("/user/:id/")
	public Payload get(String id, Context context) throws JsonParseException, JsonMappingException, IOException {
		return DataResource.get().get(USER_TYPE, id, context);
	}

	@Put("/user/:id")
	@Put("/user/:id/")
	public Payload update(String id, String jsonBody, Context context)
			throws JsonParseException, JsonMappingException, IOException {
		return DataResource.get().update(USER_TYPE, id, jsonBody, context);
	}

	@Delete("/user/:id")
	@Delete("/user/:id/")
	public Payload delete(String id, Context context) throws JsonParseException, JsonMappingException, IOException {
		return DataResource.get().delete(USER_TYPE, id, context);
	}

	@Delete("/user/:id/password")
	@Delete("/user/:id/password")
	public Payload deletePassword(String id, Context context)
			throws JsonParseException, JsonMappingException, IOException {

		Account account = AdminResource.checkAdminCredentialsOnly(context);

		// UpdateResponse response =
		// Start.getElasticClient().prepareUpdate(account.backendId,
		// UserResource.USER_TYPE, id)//
		// .setScript(new Script(
		// "ctx._source.remove('hashedPassword');ctx._source.passwordResetCode=code;",//
		// ScriptType.INLINE,//
		// "groovy",//
		// Maps.))
		// .addScriptParam("code", UUID.randomUUID().toString())//
		// .get();

		ObjectNode user = ElasticHelper.get().getObject(account.backendId, USER_TYPE, id)//
				.orElseThrow(() -> new NotFoundException(account.backendId, USER_TYPE, id));

		String resetCode = UUID.randomUUID().toString();
		user.remove(HASHED_PASSWORD);
		user.put(PASSWORD_RESET_CODE, resetCode);

		long newVersion = ElasticHelper.get().updateObject(account.backendId, user, account.username).getVersion();

		return PayloadHelper.json(//
				PayloadHelper.savedBuilder("/v1", USER_TYPE, id, newVersion).put(PASSWORD_RESET_CODE, resetCode), //
				HttpStatus.OK);
	}

	@Post("/user/:id/password")
	@Post("/user/:id/password")
	public Payload initPassword(String id, String body, Context context)
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = AdminResource.checkCredentials(context);

		// TODO do we need a password reset expire date to limit the reset
		// time scope
		String passwordResetCode = context.query().get(PASSWORD_RESET_CODE);
		if (Strings.isNullOrEmpty(passwordResetCode))
			throw new IllegalArgumentException("password reset code is empty");

		String password = Json.readJsonNode(body).asText();
		UserUtils.checkPasswordValidity(password);

		GetResponse getResponse = Start.getElasticClient().prepareGet(credentials.getBackendId(), USER_TYPE, id).get();

		if (!getResponse.isExists())
			throw new NotFoundException(credentials.getBackendId(), USER_TYPE, id);

		ObjectNode user = Json.readObjectNode(getResponse.getSourceAsString());

		if (user.get(HASHED_PASSWORD) != null || user.get(PASSWORD_RESET_CODE) == null)
			throw new IllegalArgumentException(String.format("user [%s] password has not been deleted", id));

		if (!passwordResetCode.equals(user.get(PASSWORD_RESET_CODE).asText()))
			throw new IllegalArgumentException(String.format("invalid password reset code [%s]", passwordResetCode));

		user.remove(PASSWORD_RESET_CODE);
		user.put(HASHED_PASSWORD, UserUtils.hashPassword(password));

		IndexResponse indexResponse = ElasticHelper.get().updateObject(credentials.getBackendId(), USER_TYPE, id, 0,
				user, credentials.getName());

		return PayloadHelper.saved(false, "/v1", USER_TYPE, id, indexResponse.getVersion());
	}

	@Put("/user/:id/password")
	@Put("/user/:id/password")
	public Payload updatePassword(String id, String body, Context context)
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = AdminResource.checkCredentials(context);

		if (credentials.isAdmin() || (credentials.isUser() && id.equals(credentials.getName()))) {

			String password = Json.readJsonNode(body).asText();
			UserUtils.checkPasswordValidity(password);

			ObjectNode update = Json.objectBuilder()//
					.put(HASHED_PASSWORD, UserUtils.hashPassword(password))//
					.node(PASSWORD_RESET_CODE, NullNode.getInstance())//
					.build();

			UpdateResponse response = Start.getElasticClient()
					.prepareUpdate(credentials.getBackendId(), UserResource.USER_TYPE, id).setDoc(update.toString())
					.get();

			return PayloadHelper.saved(false, "/v1/user", response.getType(), response.getId(), response.getVersion());

		} else
			throw new AuthenticationException("only the owner or admin users can update user passwords");
	}

	public static String getDefaultUserMapping() {
		JsonNode schema = SchemaValidator.validate(USER_TYPE, getDefaultUserSchema());
		return SchemaTranslator.translate(USER_TYPE, schema).toString();
	}

}
