/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.joda.time.DateTime;

import com.beust.jcommander.internal.Sets;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.utils.Backends;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
import io.spacedog.utils.CredentialsSettings;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Roles;
import io.spacedog.utils.Schema;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Usernames;
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
		Schema schema = Schema.builder(TYPE)//
				.string(USERNAME)//
				.string(BACKEND_ID)//
				.string(CREDENTIALS_LEVEL)//
				.string(ROLES).array()//
				.string(ACCESS_TOKEN)//
				.string(ACCESS_TOKEN_EXPIRES_AT)//
				.string(HASHED_PASSWORD)//
				.string(PASSWORD_RESET_CODE)//
				.string(EMAIL)//
				.string(CREATED_AT)//
				.string(UPDATED_AT)//
				.build();

		schema.validate();
		String mapping = schema.translate().toString();

		ElasticClient elastic = Start.get().getElasticClient();

		if (elastic.existsIndex(SPACEDOG_BACKEND, TYPE))
			elastic.putMapping(SPACEDOG_BACKEND, TYPE, mapping);
		else
			elastic.createIndex(SPACEDOG_BACKEND, TYPE, mapping, false);
	}

	//
	// Routes
	//

	@Get("/1/login")
	@Get("/1/login/")
	@Post("/1/login")
	@Post("/1/login/")
	public Payload login(Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		if (credentials.isPasswordChecked()) {
			credentials.newAccessToken(//
					context.query().getBoolean("expiresEarly", false));
			credentials = update(credentials);
		}
		return JsonPayload.json(Json.object(//
				"accessToken", credentials.accessToken(), //
				"expiresIn", credentials.accessTokenExpiresIn(), //
				"credentials", fromCredentials(credentials)));
	}

	@Get("/1/logout")
	@Get("/1/logout/")
	@Post("/1/logout")
	@Post("/1/logout/")
	public Payload logout(Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		credentials.deleteAccessToken();
		update(credentials);
		return JsonPayload.success();
	}

	@Post("/1/credentials")
	@Post("/1/credentials/")
	public Payload post(String body, Context context) {

		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);
		if (settings.disableGuestSignUp)
			SpaceContext.checkAdminCredentials();

		Credentials credentials = create(SpaceContext.backendId(), Level.USER, body);

		JsonBuilder<ObjectNode> builder = JsonPayload //
				.builder(true, credentials.backendId(), "/1", TYPE, credentials.id());

		if (credentials.passwordResetCode() != null)
			builder.put(PASSWORD_RESET_CODE, credentials.passwordResetCode());

		return JsonPayload.json(builder, HttpStatus.CREATED)//
				.withHeader(SpaceHeaders.SPACEDOG_OBJECT_ID, credentials.id());
	}

	@Get("/1/credentials/:id")
	@Get("/1/credentials/:id/")
	public Payload getById(String id, Context context) {
		SpaceContext.checkUserCredentials(id);
		Credentials credentials = getById(id, true).get();
		return JsonPayload.json(fromCredentials(credentials));
	}

	@Delete("/1/credentials/:id")
	@Delete("/1/credentials/:id/")
	public Payload deleteById(String id) {
		SpaceContext.checkUserCredentials(id);
		delete(id);
		return JsonPayload.success();
	}

	@Delete("/1/credentials/:id/password")
	@Delete("/1/credentials/:id/password/")
	public Payload deletePassword(String id, Context context) {
		SpaceContext.checkAdminCredentials();

		Credentials credentials = getById(id, true).get();
		credentials.resetPassword();
		credentials = update(credentials);

		return JsonPayload.json(JsonPayload
				.builder(false, credentials.backendId(), "/1", TYPE, //
						credentials.id(), credentials.version())//
				.put(PASSWORD_RESET_CODE, credentials.passwordResetCode()));
	}

	@Post("/1/credentials/:id/password")
	@Post("/1/credentials/:id/password/")
	public Payload postPassword(String id, Context context) {
		// TODO do we need a password reset expire date to limit the reset
		// time scope
		String passwordResetCode = context.get(PASSWORD_RESET_CODE);
		String password = context.get(PASSWORD);

		Credentials credentials = getById(id, true).get();
		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);
		credentials.setPassword(password, passwordResetCode, //
				Optional.of(settings.passwordRegex()));
		credentials = update(credentials);

		return JsonPayload.saved(false, credentials.backendId(), //
				"/1", TYPE, credentials.id(), credentials.version());
	}

	@Put("/1/credentials/:id/password")
	@Put("/1/credentials/:id/password/")
	public Payload putPassword(String id, Context context) {
		SpaceContext.checkUserCredentials(id);
		String password = context.get(PASSWORD);

		Credentials credentials = getById(id, true).get();
		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);
		credentials.setPassword(password, Optional.of(settings.passwordRegex()));
		credentials = update(credentials);

		return JsonPayload.saved(false, credentials.backendId(), //
				"/1", TYPE, credentials.id(), credentials.version());
	}

	@Get("/1/credentials/:id/roles")
	@Get("/1/credentials/:id/roles/")
	public Object getRoles(String id, Context context) {
		SpaceContext.checkUserCredentials(id);
		return getById(id, true).get().roles();
	}

	@Delete("/1/credentials/:id/roles")
	@Delete("/1/credentials/:id/roles/")
	public Payload deleteAllRoles(String id, Context context) {
		String backendId = SpaceContext.checkAdminCredentials().backendId();
		Credentials credentials = getById(id, true).get();
		credentials.roles().clear();
		credentials = update(credentials);
		return JsonPayload.saved(false, backendId, "/1", TYPE, //
				credentials.id(), credentials.version());
	}

	@Put("/1/credentials/:id/roles/:role")
	@Put("/1/credentials/:id/roles/:role/")
	public Payload putRole(String id, String role, Context context) {
		String backendId = SpaceContext.checkAdminCredentials().backendId();
		Roles.checkIfValid(role);
		Credentials credentials = getById(id, true).get();

		if (!credentials.roles().contains(role)) {
			credentials.roles().add(role);
			credentials = update(credentials);
		}

		return JsonPayload.saved(false, backendId, "/1", TYPE, //
				credentials.id(), credentials.version());
	}

	@Delete("/1/credentials/:id/roles/:role")
	@Delete("/1/credentials/:id/roles/:role/")
	public Payload deleteRole(String id, String role, Context context) {
		String backendId = SpaceContext.checkAdminCredentials().backendId();
		Credentials credentials = getById(id, true).get();

		if (credentials.roles().contains(role)) {
			credentials.roles().remove(role);
			credentials = update(credentials);
			return JsonPayload.saved(false, backendId, "/1", TYPE, //
					credentials.id(), credentials.version());
		}
		return JsonPayload.error(HttpStatus.NOT_FOUND);
	}

	//
	// Internal services
	//

	Credentials create(String backendId, Level level, String body) {
		return create(backendId, level, body, false);
	}

	Credentials create(String backendId, Level level, String body, boolean legacyId) {

		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);
		Credentials credentials = new Credentials(backendId);

		ObjectNode data = Json.readObject(body);
		credentials.name(Json.checkStringNotNullOrEmpty(data, Resource.USERNAME));
		Usernames.checkValid(credentials.name(), Optional.of(settings.usernameRegex()));

		if (legacyId)
			credentials.setLegacyId();

		credentials.email(Json.checkStringNotNullOrEmpty(data, Resource.EMAIL));
		credentials.level(level);

		JsonNode password = data.get(Resource.PASSWORD);

		if (Json.isNull(password))
			credentials.resetPassword();
		else
			credentials.setPassword(password.asText(), Optional.of(settings.passwordRegex()));

		return create(credentials);
	}

	Optional<Credentials> checkUsernamePassword(String backendId, String username, String password) {
		Optional<Credentials> credentials = getByName(backendId, username, false);

		if (credentials.isPresent() //
				&& credentials.get().checkPassword(password))
			return credentials;

		return Optional.empty();
	}

	Optional<Credentials> checkToken(String backendId, String accessToken) {

		SearchHits hits = Start.get().getElasticClient()//
				.prepareSearch(SPACEDOG_BACKEND, TYPE)//
				.setQuery(QueryBuilders.boolQuery()//
						.must(QueryBuilders.termQuery(BACKEND_ID, backendId))//
						.must(QueryBuilders.termQuery(ACCESS_TOKEN, accessToken)))//
				.get()//
				.getHits();

		if (hits.totalHits() == 1) {
			Credentials credentials = toCredentials(hits.getAt(0));
			if (credentials.accessTokenExpiresIn() == 0)
				throw Exceptions.invalidAuthentication("access token has expired");
			return Optional.of(credentials);
		}

		if (hits.totalHits() == 0)
			return Optional.empty();

		throw Exceptions.runtime(//
				"access token [%s] associated with too many credentials [%s]", //
				accessToken, hits.totalHits());
	}

	Optional<Credentials> getById(String id, boolean throwNotFound) {
		Credentials credentials = SpaceContext.getCredentials();

		if (id.equals(credentials.id()))
			return Optional.of(credentials);

		GetResponse response = Start.get().getElasticClient().get(SPACEDOG_BACKEND, TYPE, id);

		if (response.isExists())
			return Optional.of(toCredentials(response));

		if (throwNotFound)
			throw Exceptions.notFound(credentials.backendId(), TYPE, id);
		else
			return Optional.empty();
	}

	Optional<Credentials> getByName(String backendId, String username, boolean throwNotFound) {
		Credentials credentials = SpaceContext.getCredentials();

		if (username.equals(credentials.name()) //
				&& backendId.equals(credentials.backendId()))
			return Optional.of(credentials);

		Optional<SearchHit> searchHit = Start.get().getElasticClient().get(//
				SPACEDOG_BACKEND, TYPE, toQuery(backendId, username));

		if (!searchHit.isPresent()) {
			if (throwNotFound)
				throw Exceptions.notFound(backendId, TYPE, username);
			else
				return Optional.empty();
		}
		return Optional.of(toCredentials(searchHit.get()));
	}

	Credentials create(Credentials credentials) {

		// This is the only place where name unicity is checked
		if (exists(credentials.backendId(), credentials.name()))
			throw Exceptions.illegalArgument(//
					"[%s][%s] credentials already exists", //
					credentials.backendId(), credentials.name());

		try {
			String now = DateTime.now().toString();
			credentials.updatedAt(now);
			credentials.createdAt(now);

			ElasticClient elastic = Start.get().getElasticClient();
			String json = Json.mapper().writeValueAsString(credentials);

			// refresh index after each index change
			IndexResponse response = Strings.isNullOrEmpty(credentials.id()) //
					? elastic.index(SPACEDOG_BACKEND, TYPE, json, true) //
					: elastic.index(SPACEDOG_BACKEND, TYPE, credentials.id(), json, true);

			credentials.id(response.getId());
			credentials.version(response.getVersion());
			return credentials;

		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e);
		}
	}

	Credentials update(Credentials credentials) {
		if (Strings.isNullOrEmpty(credentials.id()))
			throw Exceptions.illegalArgument(//
					"credentials update failed: credentials id is null");

		try {
			credentials.updatedAt(DateTime.now().toString());

			// refresh index after each index change
			IndexResponse response = Start.get().getElasticClient().index(//
					SPACEDOG_BACKEND, TYPE, credentials.id(), //
					Json.mapper().writeValueAsString(credentials), //
					true);

			credentials.version(response.getVersion());
			return credentials;

		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e);
		}
	}

	void delete(String id) {
		ElasticClient elastic = Start.get().getElasticClient();
		// index refresh before not necessary since delete by id
		// index refresh after delete is necessary
		elastic.delete(SPACEDOG_BACKEND, TYPE, id, true, true);
	}

	DeleteByQueryResponse deleteAll(String backendId) {
		ElasticClient elastic = Start.get().getElasticClient();

		// need to refresh index before and after delete
		elastic.refreshType(SPACEDOG_BACKEND, TYPE);

		DeleteByQueryResponse response = elastic.deleteByQuery(SPACEDOG_BACKEND,
				QueryBuilders.termQuery(BACKEND_ID, backendId), TYPE);

		elastic.refreshType(SPACEDOG_BACKEND, TYPE);
		return response;
	}

	Set<Credentials> getAllSuperAdmins(boolean refresh) {
		QueryBuilder query = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery(CREDENTIALS_LEVEL, Level.SUPER_ADMIN.toString()));

		return getSuperAdmins(query, refresh);
	}

	Set<Credentials> getBackendSuperAdmins(String backendId, boolean refresh) {
		QueryBuilder query = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery(BACKEND_ID, backendId))//
				.filter(QueryBuilders.termQuery(CREDENTIALS_LEVEL, Level.SUPER_ADMIN.toString()));

		return getSuperAdmins(query, refresh);
	}

	Credentials createSuperdog(String username, String password, String email) {
		Usernames.checkValid(username);
		Credentials credentials = new Credentials(Backends.ROOT_API, username, Level.SUPERDOG);
		credentials.email(email);
		credentials.setPassword(password, Optional.empty());
		return create(credentials);
	}

	//
	// Implementation
	//

	private QueryBuilder toQuery(String backendId, String username) {
		return QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery(BACKEND_ID, backendId)) //
				.must(QueryBuilders.termQuery(USERNAME, username));
	}

	private Credentials toCredentials(SearchHit hit) {
		try {
			Credentials credentials = Json.mapper()//
					.readValue(hit.getSourceAsString(), Credentials.class);
			credentials.id(hit.id());
			credentials.version(hit.version());
			return credentials;
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	private Credentials toCredentials(GetResponse response) {
		try {
			Credentials credentials = Json.mapper()//
					.readValue(response.getSourceAsString(), Credentials.class);
			credentials.id(response.getId());
			credentials.version(response.getVersion());
			return credentials;
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	private ObjectNode fromCredentials(Credentials credentials) {
		return Json.object(//
				ID, credentials.id(), //
				BACKEND_ID, credentials.backendId(), //
				USERNAME, credentials.name(), //
				EMAIL, credentials.email().get(), //
				CREDENTIALS_LEVEL, credentials.level().name(), //
				ROLES, credentials.roles(), //
				CREATED_AT, credentials.createdAt(), //
				UPDATED_AT, credentials.updatedAt());
	}

	private boolean exists(String backendId, String username) {
		return Start.get().getElasticClient().exists(SPACEDOG_BACKEND, TYPE, //
				toQuery(backendId, username));
	}

	private Set<Credentials> getSuperAdmins(QueryBuilder query, boolean refresh) {
		ElasticClient elastic = Start.get().getElasticClient();
		elastic.refreshType(SPACEDOG_BACKEND, TYPE);

		SearchHit[] hits = elastic//
				.prepareSearch(SPACEDOG_BACKEND, TYPE)//
				.setQuery(query)//
				.setSize(1000)//
				.get()//
				.getHits()//
				.hits();

		Set<Credentials> superAdmins = Sets.newHashSet();

		for (SearchHit hit : hits)
			superAdmins.add(toCredentials(hit));

		return superAdmins;
	}

	//
	// singleton
	//

	private static CredentialsResource singleton = new CredentialsResource();

	static CredentialsResource get() {
		return singleton;
	}

	private CredentialsResource() {
		SettingsResource.get().registerSettingsClass(CredentialsSettings.class);
	}
}
