/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.services.Credentials.Level;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.Roles;
import io.spacedog.utils.SchemaBuilder2;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class CredentialsResource extends Resource {

	public static final String TYPE = "credentials";

	//
	// init
	//

	void init() {
		ObjectNode schema = SchemaBuilder2.builder(TYPE)//
				.stringProperty(USERNAME, true)//
				.stringProperty(BACKEND_ID, true)//
				.stringProperty(CREDENTIALS_LEVEL, true)//
				.stringProperty(ROLES, false, true)//
				.stringProperty(HASHED_PASSWORD, false)//
				.stringProperty(PASSWORD_RESET_CODE, false)//
				.stringProperty(EMAIL, true)//
				.stringProperty(CREATED_AT, true)//
				.stringProperty(UPDATED_AT, true)//
				.build();

		SchemaValidator.validate(TYPE, schema);
		String mapping = SchemaTranslator.translate(TYPE, schema).toString();

		ElasticClient elastic = Start.get().getElasticClient();

		if (elastic.existsIndex(SPACEDOG_BACKEND, TYPE))
			elastic.putMapping(SPACEDOG_BACKEND, TYPE, mapping);
		else
			elastic.createIndex(SPACEDOG_BACKEND, TYPE, mapping, false);
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
		return JsonPayload.success();
	}

	@Get("/v1/logout")
	@Get("/v1/logout/")
	@Get("/1/logout")
	@Get("/1/logout/")
	public Payload logout(Context context) {
		SpaceContext.checkUserCredentials();
		return JsonPayload.success();
	}

	@Post("/1/credentials")
	@Post("/1/credentials/")
	public Payload post(String body, Context context) {

		String backendId = SpaceContext.checkCredentials().backendId();

		Credentials credentials = Credentials.signUp(backendId, //
				Level.USER, Json.readObject(body));

		if (exists(credentials))
			throw Exceptions.illegalArgument(//
					"username [%s] for backend [%s] already exists", //
					credentials.name(), credentials.backendId());

		index(credentials);

		JsonBuilder<ObjectNode> savedBuilder = JsonPayload.savedBuilder(true, //
				credentials.backendId(), "/1", TYPE, credentials.name());

		if (credentials.passwordResetCode() != null)
			savedBuilder.put(PASSWORD_RESET_CODE, credentials.passwordResetCode());

		return JsonPayload.json(savedBuilder, HttpStatus.CREATED)//
				.withHeader(JsonPayload.HEADER_OBJECT_ID, credentials.name());
	}

	@Get("/1/credentials/:username")
	@Get("/1/credentials/:username/")
	public Payload getById(String username, Context context) {
		String backendId = SpaceContext.checkUserCredentials(username).backendId();
		Credentials credentials = get(backendId, username, true).get();
		return JsonPayload.json(//
				Json.object(USERNAME, credentials.name(), //
						EMAIL, credentials.email().get(), //
						CREDENTIALS_LEVEL, credentials.level().name(), //
						ROLES, credentials.roles(), //
						CREATED_AT, credentials.createdAt(), //
						UPDATED_AT, credentials.updatedAt()));
	}

	@Delete("/1/credentials/:username")
	@Delete("/1/credentials/:username/")
	public Payload deleteById(String username, Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials(username);
		delete(credentials.backendId(), username);
		return JsonPayload.success();
	}

	@Delete("/1/credentials/:username/password")
	@Delete("/1/credentials/:username/password/")
	public Payload deletePassword(String username, Context context) {
		String backendId = SpaceContext.checkAdminCredentials().backendId();
		Credentials credentials = get(backendId, username, true).get();

		credentials.hashedPassword(null);
		credentials.passwordResetCode(UUID.randomUUID().toString());
		index(credentials);

		return JsonPayload.json(//
				JsonPayload.savedBuilder(false, backendId, "/1", TYPE, username)//
						.put(PASSWORD_RESET_CODE, credentials.passwordResetCode()));
	}

	@Post("/1/credentials/:username/password")
	@Post("/1/credentials/:username/password/")
	public Payload postPassword(String username, Context context) {
		String backendId = SpaceContext.checkCredentials().backendId();

		// TODO do we need a password reset expire date to limit the reset
		// time scope
		String passwordResetCode = context.query().get(PASSWORD_RESET_CODE);
		Check.notNullOrEmpty(passwordResetCode, PASSWORD_RESET_CODE);

		String password = context.get(PASSWORD);
		Passwords.checkIfValid(password);

		Credentials credentials = get(backendId, username, true).get();

		if (credentials.hashedPassword() != null || credentials.passwordResetCode() == null)
			throw Exceptions.illegalArgument("[%s] password must be deleted before reset", username);

		if (!passwordResetCode.equals(credentials.passwordResetCode()))
			throw Exceptions.illegalArgument("invalid password reset code [%s]", passwordResetCode);

		credentials.passwordResetCode(null);
		credentials.hashedPassword(Passwords.checkAndHash(password));
		IndexResponse indexResponse = index(credentials);

		return JsonPayload.saved(false, backendId, "/1", TYPE, username, indexResponse.getVersion());
	}

	@Put("/1/credentials/:username/password")
	@Put("/1/credentials/:username/password/")
	public Payload putPassword(String username, Context context) {

		String backendId = SpaceContext.checkUserCredentials(username).backendId();
		Credentials credentials = get(backendId, username, true).get();

		String password = context.get(PASSWORD);
		Passwords.checkIfValid(password);

		credentials.passwordResetCode(null);
		credentials.hashedPassword(Passwords.checkAndHash(password));
		IndexResponse response = index(credentials);

		return JsonPayload.saved(false, credentials.backendId(), "/1", TYPE, username, //
				response.getVersion());
	}

	@Get("/1/credentials/:username/roles")
	@Get("/1/credentials/:username/roles/")
	public Object getRoles(String username, Context context) {
		String backendId = SpaceContext.checkUserCredentials(username).backendId();
		return get(backendId, username, true).get().roles();
	}

	@Delete("/1/credentials/:username/roles")
	@Delete("/1/credentials/:username/roles/")
	public Payload deleteAllRoles(String username, Context context) {
		String backendId = SpaceContext.checkAdminCredentials().backendId();
		Credentials credentials = get(backendId, username, true).get();
		credentials.roles().clear();
		IndexResponse response = index(credentials);
		return JsonPayload.saved(false, backendId, "/1", TYPE, username, //
				response.getVersion());
	}

	@Put("/1/credentials/:username/roles/:role")
	@Put("/1/credentials/:username/roles/:role/")
	public Payload putRole(String username, String role, Context context) {

		String backendId = SpaceContext.checkAdminCredentials().backendId();
		Roles.checkIfValid(role);
		Credentials credentials = get(backendId, username, true).get();

		if (!credentials.roles().contains(role)) {
			credentials.roles().add(role);
			index(credentials);
		}

		return JsonPayload.saved(false, backendId, "/1", TYPE, username);
	}

	@Delete("/1/credentials/:username/roles/:role")
	@Delete("/1/credentials/:username/roles/:role/")
	public Payload deleteRole(String username, String role, Context context) {
		String backendId = SpaceContext.checkAdminCredentials().backendId();
		Credentials credentials = get(backendId, username, true).get();

		if (credentials.roles().contains(role)) {
			credentials.roles().remove(role);
			IndexResponse response = index(credentials);
			return JsonPayload.saved(false, backendId, "/1", TYPE, username, //
					response.getVersion());
		}

		return JsonPayload.error(HttpStatus.NOT_FOUND);
	}

	//
	// Implementation
	//

	UpdateRequestBuilder prepareUpdate(String backendId, String username) {
		return Start.get().getElasticClient().prepareUpdate(SPACEDOG_BACKEND, TYPE,
				toCredentialsId(backendId, username));
	}

	Optional<Credentials> check(String backendId, String username, String password) {

		Optional<Credentials> credentials = get(backendId, username, false);

		if (credentials.isPresent()) {
			String providedPassword = Passwords.hash(password);
			if (credentials.get().hashedPassword() != null //
					&& providedPassword.equals(credentials.get().hashedPassword())) {
				return credentials;
			}
		}

		return Optional.empty();
	}

	boolean exists(Credentials credentials) {
		return exists(credentials.backendId(), credentials.name());
	}

	boolean exists(String backendId, String username) {
		return Start.get().getElasticClient().exists(SPACEDOG_BACKEND, TYPE, //
				toCredentialsId(backendId, username));
	}

	Optional<Credentials> get(String backendId, String username, boolean throwNotFound) {
		Credentials credentials = SpaceContext.getCredentials();

		if (credentials.name().equals(username) //
				&& credentials.backendId().equals(backendId))
			return Optional.of(credentials);

		GetResponse response = Start.get().getElasticClient().get(//
				SPACEDOG_BACKEND, TYPE, toCredentialsId(backendId, username));

		if (!response.isExists()) {
			if (throwNotFound)
				throw Exceptions.notFound(//
						"no credentials found for username [%s] in backend [%s]", //
						username, backendId);
			else
				return Optional.empty();
		}

		try {
			return Optional.of(Json.mapper().readValue(//
					response.getSourceAsString(), Credentials.class));
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	IndexResponse index(Credentials credentials) {
		try {
			credentials.updatedAt(DateTime.now().toString());
			if (credentials.createdAt() == null)
				credentials.createdAt(credentials.updatedAt());

			return Start.get().getElasticClient().index(SPACEDOG_BACKEND, TYPE, //
					toCredentialsId(credentials.backendId(), credentials.name()), //
					Json.mapper().writeValueAsString(credentials));

		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e);
		}
	}

	void delete(String backendId, String username) {
		Start.get().getElasticClient().delete(SPACEDOG_BACKEND, TYPE, //
				toCredentialsId(backendId, username), true);
	}

	DeleteByQueryResponse deleteAll(String backendId) {
		String query = new QuerySourceBuilder().setQuery(//
				QueryBuilders.termQuery(BACKEND_ID, backendId)).toString();
		ElasticClient elastic = Start.get().getElasticClient();
		elastic.refreshType(SPACEDOG_BACKEND, TYPE);
		return elastic.deleteByQuery(query, SPACEDOG_BACKEND, TYPE);
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

		return JsonPayload.json(builder);
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

		return JsonPayload.json(builder);
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
