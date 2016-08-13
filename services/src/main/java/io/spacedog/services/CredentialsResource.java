/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
import io.spacedog.utils.CredentialsSettings;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Passwords;
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
			index(credentials);
			return JsonPayload.json(Json.object(//
					"accessToken", credentials.accessToken(), //
					"expiresIn", credentials.accessTokenExpiresIn()));
		}
		return JsonPayload.success();
	}

	@Get("/1/logout")
	@Get("/1/logout/")
	@Post("/1/logout")
	@Post("/1/logout/")
	public Payload logout(Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		credentials.deleteAccessToken();
		index(credentials);
		return JsonPayload.success();
	}

	@Post("/1/credentials")
	@Post("/1/credentials/")
	public Payload post(String body, Context context) {

		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);
		if (settings.disableGuestSignUp)
			SpaceContext.checkAdminCredentials();

		Credentials credentials = create(SpaceContext.backendId(), body, Level.USER);

		JsonBuilder<ObjectNode> builder = JsonPayload //
				.builder(true, credentials.backendId(), "/1", TYPE, credentials.name());

		if (credentials.passwordResetCode() != null)
			builder.put(PASSWORD_RESET_CODE, credentials.passwordResetCode());

		return JsonPayload.json(builder, HttpStatus.CREATED)//
				.withHeader(SpaceHeaders.SPACEDOG_OBJECT_ID, credentials.name());
	}

	@Get("/1/login/linkedin")
	@Get("/1/login/linkedin/")
	@Post("/1/login/linkedin")
	@Post("/1/login/linkedin/")
	// route '/1/credentials/linkedin' is deprecated
	@Post("/1/credentials/linkedin")
	@Post("/1/credentials/linkedin/")
	public Payload linkedinLogin(Context context) {

		String backendId = SpaceContext.checkCredentials().backendId();
		String code = Check.notNullOrEmpty(context.get("code"), "code");

		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);
		if (Strings.isNullOrEmpty(settings.linkedinId))
			throw Exceptions.illegalArgument("no linkedin client id found in credentials settings");

		String redirectUri = context.get("redirect_uri");
		if (Strings.isNullOrEmpty(redirectUri))
			redirectUri = settings.linkedinRedirectUri;
		Check.notNullOrEmpty(redirectUri, "redirect_uri");

		DateTime expiresAt = DateTime.now();
		SpaceResponse response = SpaceRequest//
				.post("https://www.linkedin.com/oauth/v2/accessToken")//
				.queryParam("grant_type", "authorization_code")//
				.queryParam("client_id", settings.linkedinId)//
				.queryParam("client_secret", settings.linkedinSecret)//
				.queryParam("redirect_uri", redirectUri)//
				.queryParam("code", code)//
				.go();

		if (response.httpResponse().getStatus() >= 400)
			return JsonPayload.error(response.httpResponse().getStatus(), //
					response.getFromJson("error_description").asText());

		String accessToken = response.objectNode().get("access_token").asText();
		expiresAt = expiresAt.plus(response.objectNode().get("expires_in").asLong());

		response = SpaceRequest//
				.get("https://api.linkedin.com/v1/people/~:(id,email-address)")//
				.bearerAuth(backendId, accessToken)//
				.queryParam("format", "json")//
				.go();

		if (response.httpResponse().getStatus() >= 400)
			throw Exceptions.runtime(//
					"linkedin error when fetching id and email: " + //
							"linkedin http status [%s], linkedin error message [%s]", //
					response.httpResponse().getStatus(), //
					response.getFromJson("error_description").asText());

		String id = response.objectNode().get("id").asText();
		String email = response.objectNode().get("emailAddress").asText();

		Credentials credentials = get(backendId, id, false)//
				.orElse(new Credentials(backendId, id, Level.USER));

		boolean isNew = credentials.createdAt() == null;

		credentials.email(email);
		credentials.setAccessToken(accessToken, expiresAt);

		index(credentials);

		JsonBuilder<ObjectNode> builder = JsonPayload//
				.builder(isNew, backendId, "/1", TYPE, id)//
				.put(ACCESS_TOKEN, credentials.accessToken())//
				.put(EXPIRES_IN, credentials.accessTokenExpiresIn());

		return JsonPayload.json(builder, HttpStatus.CREATED)//
				.withHeader(SpaceHeaders.SPACEDOG_OBJECT_ID, credentials.name());
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
	public Payload deleteById(String username) {
		Credentials credentials = SpaceContext.checkUserCredentials(username);
		delete(credentials.backendId(), username);
		return JsonPayload.success();
	}

	@Delete("/1/credentials/:username/password")
	@Delete("/1/credentials/:username/password/")
	public Payload deletePassword(String username, Context context) {

		String backendId = SpaceContext.checkAdminCredentials().backendId();

		Credentials credentials = get(backendId, username, true).get();
		credentials.resetPassword();
		index(credentials);

		return JsonPayload.json(JsonPayload//
				.builder(false, backendId, "/1", TYPE, username)//
				.put(PASSWORD_RESET_CODE, credentials.passwordResetCode()));
	}

	@Post("/1/credentials/:username/password")
	@Post("/1/credentials/:username/password/")
	public Payload postPassword(String username, Context context) {
		String backendId = SpaceContext.checkCredentials().backendId();

		// TODO do we need a password reset expire date to limit the reset
		// time scope
		String passwordResetCode = context.get(PASSWORD_RESET_CODE);
		Check.notNullOrEmpty(passwordResetCode, PASSWORD_RESET_CODE);

		String password = context.get(PASSWORD);
		Passwords.checkIfValid(password);

		Credentials credentials = get(backendId, username, true).get();
		credentials.setPassword(password, passwordResetCode);
		IndexResponse indexResponse = index(credentials);

		return JsonPayload.json(JsonPayload.builder(//
				false, backendId, "/1", TYPE, username, indexResponse.getVersion()));
	}

	@Put("/1/credentials/:username/password")
	@Put("/1/credentials/:username/password/")
	public Payload putPassword(String username, Context context) {

		String backendId = SpaceContext.checkUserCredentials(username).backendId();
		Credentials credentials = get(backendId, username, true).get();

		String password = context.get(PASSWORD);
		Passwords.checkIfValid(password);

		credentials.setPassword(password);
		IndexResponse response = index(credentials);

		return JsonPayload.json(JsonPayload.builder(//
				false, backendId, "/1", TYPE, username, response.getVersion()));
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
	// Internal services
	//

	Credentials create(String backendId, String body, Level level) {

		Credentials credentials = new Credentials(backendId);

		ObjectNode data = Json.readObject(body);
		credentials.name(Json.checkStringNotNullOrEmpty(data, Resource.USERNAME));
		Usernames.checkIfValid(credentials.name());

		credentials.email(Json.checkStringNotNullOrEmpty(data, Resource.EMAIL));
		credentials.level(level);

		JsonNode password = data.get(Resource.PASSWORD);

		if (Json.isNull(password))
			credentials.resetPassword();
		else
			credentials.setPassword(password.asText());

		if (exists(credentials))
			throw Exceptions.illegalArgument(//
					"[%s][%s] credentials already exists", //
					credentials.backendId(), credentials.name());

		index(credentials);
		return credentials;
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

		if (credentials.isPresent() //
				&& credentials.get().checkPassword(password))
			return credentials;

		return Optional.empty();
	}

	Optional<Credentials> check(String backendId, String accessToken) {

		Start.get().getElasticClient()//
				.refreshType(SPACEDOG_BACKEND, TYPE);

		SearchHits hits = Start.get().getElasticClient()//
				.prepareSearch(SPACEDOG_BACKEND, TYPE)//
				.setQuery(QueryBuilders.boolQuery()//
						.must(QueryBuilders.termQuery(BACKEND_ID, backendId))//
						.must(QueryBuilders.termQuery(ACCESS_TOKEN, accessToken)))//
				.get()//
				.getHits();

		if (hits.totalHits() == 1) {
			Credentials credentials = toCredentials(hits.getAt(0).getSourceAsString());
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
				throw Exceptions.notFound(backendId, TYPE, username);
			else
				return Optional.empty();
		}
		return Optional.of(toCredentials(response.getSourceAsString()));
	}

	private Credentials toCredentials(String json) {
		try {
			return Json.mapper().readValue(json, Credentials.class);
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
		SettingsResource.get().registerSettingsClass(CredentialsSettings.class);
	}
}
