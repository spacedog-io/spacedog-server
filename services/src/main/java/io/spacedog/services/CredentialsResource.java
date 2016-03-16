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

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
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
import net.codestory.http.annotations.Get;
import net.codestory.http.payload.Payload;

public class CredentialsResource extends Resource {

	public static final String TYPE = "credentials";

	//
	// Credentials constants and schema
	//

	public static ObjectNode getCredentialsSchema() {
		return SchemaBuilder2.builder(TYPE)//
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
		JsonNode schema = SchemaValidator.validate(TYPE, getCredentialsSchema());
		return SchemaTranslator.translate(TYPE, schema).toString();
	}

	//
	// init
	//

	void init() {
		ElasticClient elastic = Start.get().getElasticClient();

		if (elastic.existsIndex(SPACEDOG_BACKEND, TYPE))
			elastic.putMapping(SPACEDOG_BACKEND, TYPE, getCredentialsMapping());
		else
			elastic.createIndex(SPACEDOG_BACKEND, TYPE, getCredentialsMapping());
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

	public String doDeletePassword(String backendId, String username) {

		String passwordResetCode = UUID.randomUUID().toString();

		prepareUpdate(backendId, username)//
				.setDoc(HASHED_PASSWORD, null, //
						PASSWORD_RESET_CODE, passwordResetCode, //
						UPDATED_AT, DateTime.now())//
				.get();

		return passwordResetCode;
	}

	public IndexResponse doPostPassword(String backendId, String username, Context context) {
		// TODO do we need a password reset expire date to limit the reset
		// time scope
		String passwordResetCode = context.query().get(PASSWORD_RESET_CODE);
		Check.notNullOrEmpty(passwordResetCode, PASSWORD_RESET_CODE);

		String password = context.get(PASSWORD);
		Passwords.checkIfValid(password);

		GetResponse getResponse = get(backendId, username, true);
		ObjectNode credentials = Json.readObjectNode(getResponse.getSourceAsString());

		if (!Json.isNull(credentials.get(HASHED_PASSWORD)) || Json.isNull(credentials.get(PASSWORD_RESET_CODE)))
			throw Exceptions.illegalArgument("user [%s] password has not been deleted", username);

		if (!passwordResetCode.equals(credentials.get(PASSWORD_RESET_CODE).asText()))
			throw Exceptions.illegalArgument("invalid password reset code [%s]", passwordResetCode);

		credentials.remove(PASSWORD_RESET_CODE);
		credentials.put(HASHED_PASSWORD, Passwords.checkAndHash(password));

		return index(backendId, username, credentials);
	}

	public UpdateResponse doPutPassword(String backendId, String username, Context context) {

		String password = context.get(PASSWORD);
		Passwords.checkIfValid(password);

		return prepareUpdate(backendId, username)//
				.setDoc(HASHED_PASSWORD, Passwords.checkAndHash(password), //
						PASSWORD_RESET_CODE, null)//
				.get();
	}

	//
	// Implementation
	//

	static class SignUp {

		ObjectNode data;
		Credentials credentials;
		Optional<String> createdBy = Optional.empty();
		Optional<String> hashedPassword = Optional.empty();
		Optional<String> passwordResetCode = Optional.empty();

		SignUp(String backendId, Level level, String body) {

			data = Json.readObjectNode(body);
			String email = Json.checkStringNotNullOrEmpty(data, EMAIL);
			String username = Json.checkStringNotNullOrEmpty(data, USERNAME);
			Usernames.checkIfValid(username);

			credentials = new Credentials(backendId, username, email, level);
			JsonNode password = data.get(PASSWORD);

			if (Json.isNull(password))
				passwordResetCode = Optional.of(UUID.randomUUID().toString());
			else
				hashedPassword = Optional.of(Passwords.checkAndHash(password.asText()));

			data.remove(Arrays.asList(PASSWORD, CREDENTIALS_LEVEL, BACKEND_ID));
		}
	}

	UpdateRequestBuilder prepareUpdate(String backendId, String username) {
		return Start.get().getElasticClient().prepareUpdate(SPACEDOG_BACKEND, TYPE,
				toCredentialsId(backendId, username));
	}

	Optional<Credentials> check(String backendId, String username, String password) {

		GetResponse response = get(backendId, username, false);

		if (response.isExists()) {
			String providedPassword = Passwords.hash(password);
			Map<String, Object> credentials = response.getSourceAsMap();
			Object expectedPassword = credentials.get(HASHED_PASSWORD);

			if (expectedPassword != null && providedPassword.equals(expectedPassword.toString())) {
				Object email = credentials.get(EMAIL);
				Level level = Level.valueOf(credentials.get(CREDENTIALS_LEVEL).toString());
				return Optional.of(new Credentials(backendId, username, //
						email == null ? null : email.toString(), level));
			}
		}

		return Optional.empty();
	}

	boolean exists(String backendId, String username) {
		return Start.get().getElasticClient().exists(SPACEDOG_BACKEND, TYPE, //
				toCredentialsId(backendId, username));
	}

	GetResponse get(String backendId, String username, boolean throwNotFound) {
		GetResponse response = Start.get().getElasticClient().get(SPACEDOG_BACKEND, TYPE, //
				toCredentialsId(backendId, username));

		if (throwNotFound && !response.isExists())
			throw Exceptions.notFound("no credentials found for username [%s] in backend [%s]", username, backendId);

		return response;
	}

	IndexResponse create(SignUp signUp) {

		Credentials credentials = signUp.credentials;

		if (exists(credentials.backendId(), credentials.name()))
			throw Exceptions.illegalArgument(//
					"user credentials for backend [%s] with usename [%s] already exists", //
					credentials.backendId(), credentials.name());

		String now = DateTime.now().toString();
		ObjectNode json = Json.object(//
				BACKEND_ID, credentials.backendId(), //
				USERNAME, credentials.name(), //
				EMAIL, credentials.email().get(), //
				CREDENTIALS_LEVEL, credentials.level().toString(), //
				CREATED_AT, now, //
				UPDATED_AT, now);

		if (signUp.hashedPassword.isPresent())
			json.put(HASHED_PASSWORD, signUp.hashedPassword.get());
		else
			json.put(PASSWORD_RESET_CODE, signUp.passwordResetCode.get());

		return index(credentials.backendId(), credentials.name(), json);
	}

	IndexResponse index(String backendId, String username, JsonNode credentials) {
		return Start.get().getElasticClient().index(SPACEDOG_BACKEND, TYPE, //
				toCredentialsId(backendId, username), credentials.toString());
	}

	DeleteResponse delete(String backendId, String username) {
		return Start.get().getElasticClient().delete(SPACEDOG_BACKEND, TYPE, //
				toCredentialsId(backendId, username));
	}

	DeleteByQueryResponse deleteAll(String backendId) {
		String query = new QuerySourceBuilder().setQuery(//
				QueryBuilders.termQuery(BACKEND_ID, backendId)).toString();
		ElasticClient elastic = Start.get().getElasticClient();
		elastic.refreshType(SPACEDOG_BACKEND, TYPE);
		return elastic.deleteByQuery(SPACEDOG_BACKEND, TYPE, query);
	}

	public Payload getAllSuperAdmins(boolean refresh) {

		ElasticClient elastic = Start.get().getElasticClient();

		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery(CREDENTIALS_LEVEL, Level.SUPER_ADMIN.toString()));

		elastic.refreshType(SPACEDOG_BACKEND, TYPE);

		SearchHit[] hits = elastic.prepareSearch(SPACEDOG_BACKEND, TYPE)//
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

		elastic.refreshType(SPACEDOG_BACKEND, TYPE);

		SearchHit[] hits = elastic.prepareSearch(SPACEDOG_BACKEND, TYPE)//
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

	static String toCredentialsId(String backendId, String username) {
		return String.join("-", backendId, username);
	}

	static String[] fromCredentialsId(String id) {
		return id.split("-", 2);
	}

	//
	// singleton
	//

	private static CredentialsResource singleton = new CredentialsResource();

	static CredentialsResource get() {
		return singleton;
	}

	private CredentialsResource() {
	}
}
