/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.services.Credentials.Level;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.SchemaBuilder2;
import io.spacedog.utils.Usernames;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

//@Prefix("/1")
public class UserResource extends Resource {

	//
	// user constants and schema
	//

	static final String USER_TYPE = "user";

	public static final String PASSWORD = "password";
	public static final String EMAIL = "email";
	public static final String USERNAME = "username";
	public static final String HASHED_PASSWORD = "hashedPassword";
	public static final String PASSWORD_RESET_CODE = "passwordResetCode";

	public static SchemaBuilder2 getDefaultUserSchemaBuilder() {
		return SchemaBuilder2.builder(USER_TYPE, USERNAME)//
				.stringProperty(USERNAME, true)//
				.stringProperty(EMAIL, true);
	}

	public static ObjectNode getDefaultUserSchema() {
		return getDefaultUserSchemaBuilder().build();
	}

	public static String getDefaultUserMapping() {
		JsonNode schema = SchemaValidator.validate(USER_TYPE, getDefaultUserSchema());
		return SchemaTranslator.translate(USER_TYPE, schema).toString();
	}

	//
	// credentials constants and schema
	//

	public static final String CREDENTIALS_TYPE = "credentials";
	public static final String CREDENTIALS_LEVEL = "level";
	public static final String UPDATED_AT = "updatedAt";
	public static final String CREATED_AT = "createdAt";

	public static ObjectNode getCredentialsSchema() {
		return SchemaBuilder2.builder(CREDENTIALS_TYPE)//
				.stringProperty(USERNAME, true)//
				.stringProperty(BACKEND_ID, true)//
				.stringProperty(CREDENTIALS_LEVEL, true)//
				.stringProperty(HASHED_PASSWORD, false)//
				.stringProperty(PASSWORD_RESET_CODE, false)//
				.stringProperty(EMAIL, true)//
				.stringProperty(CREATED_AT, true)//
				.stringProperty(UPDATED_AT, true)//
				.build();
	}

	public static String getCredentialsMapping() {
		JsonNode schema = SchemaValidator.validate(CREDENTIALS_TYPE, getCredentialsSchema());
		return SchemaTranslator.translate(CREDENTIALS_TYPE, schema).toString();
	}

	//
	// init
	//

	void init() {
		ElasticClient elastic = Start.get().getElasticClient();

		if (elastic.existsIndex(SPACEDOG_BACKEND, CREDENTIALS_TYPE))
			elastic.putMapping(SPACEDOG_BACKEND, CREDENTIALS_TYPE, getCredentialsMapping());
		else
			elastic.createIndex(SPACEDOG_BACKEND, CREDENTIALS_TYPE, getCredentialsMapping());
	}

	//
	// Routes
	//

	@Get("/v1/login")
	@Get("/v1/login/")
	@Get("/1/login")
	@Get("/1/login/")
	public Payload login(Context context) {
		SpaceContext.checkUserCredentials();
		return Payloads.success();
	}

	@Get("/v1/logout")
	@Get("/v1/logout/")
	@Get("/1/logout")
	@Get("/1/logout/")
	public Payload logout(Context context) {
		SpaceContext.checkUserCredentials();
		return Payloads.success();
	}

	@Get("/v1/user")
	@Get("/v1/user/")
	@Get("/1/user")
	@Get("/1/user/")
	public Payload getAll(Context context) {
		// TODO access to /0/data/user and /0/user should be consistent
		SpaceContext.checkAdminCredentials();
		return DataResource.get().getByType(USER_TYPE, context);
	}

	@Delete("/v1/user")
	@Delete("/v1/user/")
	@Delete("/1/user")
	@Delete("/1/user/")
	public Payload deleteAll(Context context) {
		// TODO access to /0/data/user and /0/user should be consistent
		Credentials credentials = SpaceContext.checkSuperAdminCredentials();
		ElasticClient elastic = Start.get().getElasticClient();

		elastic.refreshType(SPACEDOG_BACKEND, CREDENTIALS_TYPE);

		// delete all backend credentials and users but super admins

		QuerySourceBuilder query = new QuerySourceBuilder().setQuery(//
				QueryBuilders.boolQuery()//
						.must(QueryBuilders.termQuery(Resource.BACKEND_ID, credentials.backendId()))//
						.mustNot(QueryBuilders.termQuery(CREDENTIALS_LEVEL, Level.SUPER_ADMIN.toString())));

		SearchResponse search = elastic.prepareSearch(SPACEDOG_BACKEND, CREDENTIALS_TYPE)//
				.setQuery(query.toString())//
				.setSize(1000)//
				.setFetchSource(false)//
				.setScroll(TimeValue.timeValueMinutes(1))//
				.get();

		SearchHit[] hits = search.getHits().getHits();
		int totalDeleted = hits.length;

		while (hits.length > 0) {
			BulkRequestBuilder bulk = elastic.prepareBulk();
			for (SearchHit hit : hits) {
				bulk.add(new DeleteRequest(//
						elastic.toAlias(SPACEDOG_BACKEND, CREDENTIALS_TYPE), CREDENTIALS_TYPE, hit.getId()));
				bulk.add(new DeleteRequest(//
						elastic.toAlias(credentials.backendId(), USER_TYPE), USER_TYPE,
						fromCredentialsId(hit.getId())[1]));
			}
			bulk.get();

			if (hits.length < 1000)
				break;

			search = elastic.prepareSearchScroll(search.getScrollId()).get();
			hits = search.getHits().hits();
			totalDeleted += hits.length;
		}

		return Payloads.json(Payloads.minimalBuilder(200).put("totalDeleted", totalDeleted));
	}

	@Post("/v1/user")
	@Post("/v1/user/")
	@Post("/1/user")
	@Post("/1/user/")
	public Payload signUp(String body, Context context) {

		/**
		 * TODO adjust this. Admin should be able to sign up users. But what
		 * backend id if many in account? Backend key should be able to sign up
		 * users. Should common users be able to?
		 */
		Credentials credentials = SpaceContext.checkCredentials();
		UserSignUp user = new UserSignUp(credentials.backendId(), Level.USER, body);

		if (credentials.isAdminAuthenticated())
			user.setCreatedBy(credentials.name());

		if (user.existsCredentials())
			throw Exceptions.illegalArgument(//
					"user credentials for backend [%s] with usename [%s] already exists", //
					user.backendId, user.username);

		// TODO
		// who cares? replace obsolete user object if the credentials part is ok
		if (user.existsUser())
			throw Exceptions.illegalArgument(//
					"user for backend [%s] with usename [%s] already exists", //
					user.backendId, user.username);

		user.indexCredentials();
		user.indexUser();

		JsonBuilder<ObjectNode> savedBuilder = Payloads.savedBuilder(true, credentials.backendId(), "/1", USER_TYPE,
				user.username);

		if (user.passwordResetCode.isPresent())
			savedBuilder.put(PASSWORD_RESET_CODE, user.passwordResetCode.get());

		return Payloads.json(savedBuilder, HttpStatus.CREATED)//
				.withHeader(Payloads.HEADER_OBJECT_ID, user.username);
	}

	@Get("/v1/user/:username")
	@Get("/v1/user/:username/")
	@Get("/1/user/:username")
	@Get("/1/user/:username/")
	public Payload get(String username, Context context) {
		// TODO access to /0/data/user and /0/user should be consistent
		SpaceContext.checkUserCredentials(username);
		return DataResource.get().getById(USER_TYPE, username, context);
	}

	@Put("/v1/user/:username")
	@Put("/v1/user/:username/")
	@Put("/1/user/:username")
	@Put("/1/user/:username/")
	public Payload put(String username, String jsonBody, Context context) {
		// TODO access to /0/data/user and /0/user should be consistent
		SpaceContext.checkUserCredentials(username);
		return DataResource.get().put(USER_TYPE, username, jsonBody, context);
	}

	@Delete("/v1/user/:username")
	@Delete("/v1/user/:username/")
	@Delete("/1/user/:username")
	@Delete("/1/user/:username/")
	public Payload delete(String username, Context context) {
		// TODO access to /0/data/user and /0/user should be consistent
		Credentials credentials = SpaceContext.checkUserCredentials(username);
		deleteCredentials(credentials.backendId(), username);
		return DataResource.get().deleteById(USER_TYPE, username, context);
	}

	@Delete("/v1/user/:username/password")
	@Delete("/v1/user/:username/password")
	@Delete("/1/user/:username/password")
	@Delete("/1/user/:username/password")
	public Payload deletePassword(String username, Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials();

		String passwordResetCode = UUID.randomUUID().toString();
		UpdateResponse response = prepareCredentialsUpdate(credentials.backendId(), username)//
				.setDoc(HASHED_PASSWORD, null, //
						PASSWORD_RESET_CODE, passwordResetCode, //
						UPDATED_AT, DateTime.now())//
				.get();

		return Payloads.json(//
				Payloads.savedBuilder(false, credentials.backendId(), "/1", USER_TYPE, username)//
						.put(PASSWORD_RESET_CODE, passwordResetCode));
	}

	@Post("/v1/user/:username/password")
	@Post("/v1/user/:username/password")
	@Post("/1/user/:username/password")
	@Post("/1/user/:username/password")
	public Payload postPassword(String username, Context context) {

		String backendId = SpaceContext.checkCredentials().backendId();

		// TODO do we need a password reset expire date to limit the reset
		// time scope
		String passwordResetCode = context.query().get(PASSWORD_RESET_CODE);
		Check.notNullOrEmpty(passwordResetCode, PASSWORD_RESET_CODE);

		String password = context.get(PASSWORD);
		Passwords.checkIfValid(password);

		GetResponse getResponse = getCredentials(backendId, username, true);
		ObjectNode credentials = Json.readObjectNode(getResponse.getSourceAsString());

		if (!Json.isNull(credentials.get(HASHED_PASSWORD)) || Json.isNull(credentials.get(PASSWORD_RESET_CODE)))
			throw Exceptions.illegalArgument("user [%s] password has not been deleted", username);

		if (!passwordResetCode.equals(credentials.get(PASSWORD_RESET_CODE).asText()))
			throw Exceptions.illegalArgument("invalid password reset code [%s]", passwordResetCode);

		credentials.remove(PASSWORD_RESET_CODE);
		credentials.put(HASHED_PASSWORD, Passwords.checkAndHash(password));

		IndexResponse indexResponse = indexCredentials(backendId, username, credentials);

		return Payloads.saved(false, backendId, "/1", USER_TYPE, username, indexResponse.getVersion());
	}

	@Put("/v1/user/:id/password")
	@Put("/v1/user/:id/password")
	@Put("/1/user/:id/password")
	@Put("/1/user/:id/password")
	public Payload putPassword(String username, Context context) {

		Credentials credentials = SpaceContext.checkUserCredentials(username);

		String password = context.get(PASSWORD);
		Passwords.checkIfValid(password);

		UpdateResponse response = prepareCredentialsUpdate(credentials.backendId(), username)//
				.setDoc(HASHED_PASSWORD, Passwords.checkAndHash(password), //
						PASSWORD_RESET_CODE, null)//
				.get();

		return Payloads.saved(false, credentials.backendId(), "/1/user", USER_TYPE, username, response.getVersion());
	}

	//
	// Implementation
	//

	UpdateRequestBuilder prepareCredentialsUpdate(String backendId, String username) {
		return Start.get().getElasticClient().prepareUpdate(SPACEDOG_BACKEND, CREDENTIALS_TYPE,
				toCredentialsId(backendId, username));
	}

	class UserSignUp {

		String username;
		String backendId;
		String email;
		Level level;
		Optional<String> hashedPassword = Optional.empty();
		Optional<String> passwordResetCode = Optional.empty();
		Optional<String> createdBy = Optional.empty();
		private ObjectNode user;

		UserSignUp(String backendId, Level level, String body) {

			user = Json.readObjectNode(body);

			this.level = level;
			this.backendId = backendId;
			this.email = Json.checkStringNotNullOrEmpty(user, EMAIL);
			this.username = Json.checkStringNotNullOrEmpty(user, USERNAME);

			Usernames.checkIfValid(username);
			JsonNode password = user.get(PASSWORD);

			if (Json.isNull(password))
				this.passwordResetCode = Optional.of(UUID.randomUUID().toString());
			else
				this.hashedPassword = Optional.of(Passwords.checkAndHash(password.asText()));
		}

		void setCreatedBy(String createdBy) {
			this.createdBy = Optional.of(createdBy);
		}

		boolean existsCredentials() {
			return UserResource.this.existsCredentials(backendId, username);
		}

		boolean existsUser() {
			return UserResource.this.existsUser(backendId, username);
		}

		void indexCredentials() {
			String now = DateTime.now().toString();
			ObjectNode credentials = Json.object(//
					BACKEND_ID, backendId, //
					USERNAME, username, //
					CREDENTIALS_LEVEL, level.toString(), //
					EMAIL, email, //
					CREATED_AT, now, //
					UPDATED_AT, now);

			if (hashedPassword.isPresent())
				credentials.put(HASHED_PASSWORD, hashedPassword.get());
			else
				credentials.put(PASSWORD_RESET_CODE, passwordResetCode.get());

			UserResource.this.indexCredentials(backendId, username, credentials);
		}

		void indexUser() {
			user.remove(Arrays.asList(PASSWORD, CREDENTIALS_LEVEL, BACKEND_ID));
			DataStore.get().createObject(backendId, USER_TYPE, //
					username, user, createdBy.isPresent() ? createdBy.get() : username);
		}
	}

	Optional<Credentials> checkCredentials(String backendId, String username, String password) {

		GetResponse response = UserResource.get().getCredentials(backendId, username, false);

		if (response.isExists()) {
			String providedPassword = Passwords.hash(password);
			Map<String, Object> credentials = response.getSourceAsMap();
			Object expectedPassword = credentials.get(UserResource.HASHED_PASSWORD);

			if (expectedPassword != null && providedPassword.equals(expectedPassword.toString())) {
				Object email = credentials.get(UserResource.EMAIL);
				Level level = Level.valueOf(credentials.get(UserResource.CREDENTIALS_LEVEL).toString());
				return Optional.of(Credentials.fromUser(backendId, username, //
						email == null ? null : email.toString(), level));
			}
		}

		return Optional.empty();
	}

	boolean existsCredentials(String backendId, String username) {
		return Start.get().getElasticClient().exists(SPACEDOG_BACKEND, CREDENTIALS_TYPE, //
				toCredentialsId(backendId, username));
	}

	boolean existsUser(String backendId, String username) {
		return Start.get().getElasticClient().exists(backendId, USER_TYPE, username);
	}

	GetResponse getCredentials(String backendId, String username, boolean throwNotFound) {
		GetResponse response = Start.get().getElasticClient().get(SPACEDOG_BACKEND, CREDENTIALS_TYPE, //
				toCredentialsId(backendId, username));

		if (throwNotFound && !response.isExists())
			throw Exceptions.notFound("no credentials found for username [%s] in backend [%s]", username, backendId);

		return response;
	}

	GetResponse getUser(String backendId, String username, boolean throwNotFound) {
		GetResponse response = Start.get().getElasticClient().get(backendId, USER_TYPE, username);

		if (throwNotFound && !response.isExists())
			throw Exceptions.notFound("no user found for username [%s] in backend [%s]", username, backendId);

		return response;

	}

	IndexResponse indexCredentials(String backendId, String username, JsonNode credentials) {
		return Start.get().getElasticClient().index(SPACEDOG_BACKEND, CREDENTIALS_TYPE, //
				toCredentialsId(backendId, username), credentials.toString());
	}

	DeleteResponse deleteCredentials(String backendId, String username) {
		return Start.get().getElasticClient().delete(SPACEDOG_BACKEND, CREDENTIALS_TYPE, //
				toCredentialsId(backendId, username));
	}

	void deleteAllBackendCredentials(String backendId) {
		String query = new QuerySourceBuilder().setQuery(//
				QueryBuilders.termQuery(Resource.BACKEND_ID, backendId)).toString();
		ElasticClient elastic = Start.get().getElasticClient();
		elastic.refreshType(SPACEDOG_BACKEND, CREDENTIALS_TYPE);
		elastic.deleteByQuery(SPACEDOG_BACKEND, CREDENTIALS_TYPE, query);
	}

	public Payload getAllSuperAdmins(boolean refresh) {

		ElasticClient elastic = Start.get().getElasticClient();

		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery(CREDENTIALS_LEVEL, Level.SUPER_ADMIN.toString()));

		elastic.refreshType(SPACEDOG_BACKEND, CREDENTIALS_TYPE);

		SearchHit[] hits = elastic.prepareSearch(SPACEDOG_BACKEND, CREDENTIALS_TYPE)//
				.setQuery(new QuerySourceBuilder().setQuery(boolQueryBuilder).toString())//
				.setSize(1000)//
				.get()//
				.getHits()//
				.hits();

		Map<String, List<ObjectNode>> backends = Maps.newHashMap();

		for (SearchHit hit : hits) {
			String[] strings = fromCredentialsId(hit.getId());
			List<ObjectNode> superAdmins = backends.get(strings[0]);
			if (superAdmins == null) {
				superAdmins = Lists.newArrayList();
				backends.put(strings[0], superAdmins);
			}
			superAdmins.add(Json.object(USERNAME, strings[0], EMAIL, hit.getSource().get(EMAIL).toString()));
		}

		JsonBuilder<ObjectNode> builder = Json.objectBuilder()//
				.put("total", hits.length)//
				.array("results");

		for (Entry<String, List<ObjectNode>> entry : backends.entrySet()) {
			builder.object()//
					.put(BACKEND_ID, entry.getKey())//
					.array("superAdmins");
			for (ObjectNode superAdmin : entry.getValue())
				builder.node(superAdmin);
			builder.end().end();
		}

		return Payloads.json(builder);
	}

	public Payload getAllSuperAdmins(String backendId, boolean refresh) {

		ElasticClient elastic = Start.get().getElasticClient();

		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery(BACKEND_ID, backendId))//
				.filter(QueryBuilders.termQuery(CREDENTIALS_LEVEL, Level.SUPER_ADMIN.toString()));

		elastic.refreshType(SPACEDOG_BACKEND, CREDENTIALS_TYPE);

		SearchHit[] hits = elastic.prepareSearch(SPACEDOG_BACKEND, CREDENTIALS_TYPE)//
				.setQuery(new QuerySourceBuilder().setQuery(boolQueryBuilder).toString())//
				.setSize(1000)//
				.get()//
				.getHits()//
				.hits();

		JsonBuilder<ObjectNode> builder = Json.objectBuilder()//
				.put(BACKEND_ID, backendId)//
				.array("superAdmins");

		for (SearchHit hit : hits)
			builder.node(Json.object(USERNAME, hit.getSource().get(USERNAME).toString(), //
					EMAIL, hit.getSource().get(EMAIL).toString()));

		return Payloads.json(builder);
	}

	//
	// Implementation
	//

	static String toCredentialsId(String backendId, String username) {
		return String.join("-", backendId, username);
	}

	static String[] fromCredentialsId(String id) {
		return id.split("-", 2);
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
