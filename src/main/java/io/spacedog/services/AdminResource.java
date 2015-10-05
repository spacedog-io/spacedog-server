package io.spacedog.services;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;

import com.eclipsesource.json.JsonObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

@Prefix("/v1/admin")
public class AdminResource extends AbstractResource {

	// singleton begins

	private static AdminResource singleton = new AdminResource();

	static AdminResource get() {
		return singleton;
	}

	private AdminResource() {
	}

	// singleton ends

	public static final String SPACEDOG_INDEX = "spacedog";
	public static final String ACCOUNT_TYPE = "account";
	public static final String DEFAULT_API_KEY_ID = "client-app";

	private static final Set<String> INTERNAL_INDICES = Sets
			.newHashSet(SPACEDOG_INDEX);

	public static final String SPACEDOG_KEY_HEADER = "x-spacedog-key";
	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BASIC_AUTHENTICATION_SCHEME = "Basic";

	public static final Charset UTF_8 = Charset.forName("UTF-8");

	void initSpacedogIndex() throws InterruptedException, ExecutionException,
			IOException {

		String accountMapping = Resources.toString(Resources
				.getResource("io/spacedog/services/account-mapping.json"),
				UTF_8);

		IndicesAdminClient indices = Start.getElasticClient().admin().indices();

		if (!indices.prepareExists(SPACEDOG_INDEX).get().isExists()) {
			indices.prepareCreate(SPACEDOG_INDEX)
					.addMapping(ACCOUNT_TYPE, accountMapping).get();
		}
	}

	/**
	 * Internal web service. Not accessible by clients.
	 */
	@Get("/account")
	@Get("/account/")
	public Payload getAll(Context context) {
		try {
			SearchResponse response = Start.getElasticClient()
					.prepareSearch(SPACEDOG_INDEX).setTypes(ACCOUNT_TYPE)
					.setQuery(QueryBuilders.matchAllQuery()).get();

			return extractResults(response);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	/**
	 * Internal web service. Not accessible by clients.
	 */
	@Get("/user/:username")
	@Get("/user/:username/")
	public Payload isUserRegistered(String username) {
		return checkExistence(SPACEDOG_INDEX, ACCOUNT_TYPE, "username",
				username);
	}

	/**
	 * Internal web service. Not accessible by clients.
	 */
	@Get("/backend/:id")
	@Get("/backend/:id/")
	public Payload isBackendRegistered(String backendId) {
		return checkExistence(SPACEDOG_INDEX, ACCOUNT_TYPE, "backendId",
				backendId);
	}

	/**
	 * Internal web service. Not accessible by clients.
	 */
	@Post("/account")
	@Post("/account/")
	public Payload signUp(String body, Context context) {
		try {
			JsonObject input = JsonObject.readFrom(body);

			Account account = new Account();
			account.backendId = input.getString("backendId", null);
			account.username = input.getString("username", null);
			account.email = input.getString("email", null);
			account.password = input.getString("password", null);
			account.apiKey = new ApiKey(DEFAULT_API_KEY_ID);
			account.checkAccountInputValidity();

			if (ElasticHelper.search(SPACEDOG_INDEX, ACCOUNT_TYPE, "username",
					account.username).getTotalHits() > 0)
				return badParameters("username", account.username,
						account.username + " is already used");

			byte[] accountBytes = getObjectMapper().writeValueAsBytes(account);
			Start.getElasticClient().prepareIndex(SPACEDOG_INDEX, ACCOUNT_TYPE)
					.setSource(accountBytes).get();

			// backend index is named after the backend id
			Start.getElasticClient()
					.admin()
					.indices()
					.prepareCreate(account.backendId)
					.addMapping(UserResource.USER_TYPE,
							UserResource.getDefaultUserMapping()).get();

			return created("/v1/admin", ACCOUNT_TYPE, account.backendId)
					.withHeader(AdminResource.SPACEDOG_KEY_HEADER,
							account.spaceDogKey());

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	/**
	 * Internal web service. Not accessible by clients.
	 */
	@Get("/account/:id")
	@Get("/account/:id/")
	public Payload get(String backendId, Context context) {
		try {
			GetResponse response = Start.getElasticClient()
					.prepareGet(SPACEDOG_INDEX, ACCOUNT_TYPE, backendId).get();

			if (!response.isExists())
				return error(HttpStatus.NOT_FOUND,
						"account with id [%s] not found", backendId);

			return new Payload(JSON_CONTENT, response.getSourceAsBytes(),
					HttpStatus.OK);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	/**
	 * Internal web service. Not accessible by clients.
	 */
	@Put("/account/:id")
	@Put("/account/:id/")
	public Payload put(String id, String body, Context context) {
		try {
			return new Payload(HttpStatus.NOT_IMPLEMENTED);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	/**
	 * Internal web service. Not accessible by clients.
	 */
	@Delete("/account/:id")
	@Delete("/account/:id/")
	public Payload delete(String backendId, Context context) {
		try {
			DeleteResponse resp1 = Start.getElasticClient()
					.prepareDelete(SPACEDOG_INDEX, ACCOUNT_TYPE, backendId)
					.get();

			if (!resp1.isFound())
				return error(HttpStatus.NOT_FOUND,
						"account with id [%s] not found", backendId);

			DeleteIndexResponse resp2 = Start.getElasticClient().admin()
					.indices().prepareDelete(backendId).get();

			if (!resp2.isAcknowledged())
				return error(
						HttpStatus.INTERNAL_SERVER_ERROR,
						"internal index deletion not acknowledged for account and backend with id [%s] ",
						backendId);

			return success();
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Get("/login")
	@Get("/login/")
	public Payload login(Context context) {
		try {

			Optional<String[]> tokens = decodeAuthorizationHeader(context);

			if (tokens.isPresent()) {

				SearchHits hits = ElasticHelper.search(SPACEDOG_INDEX,
						ACCOUNT_TYPE, "username", tokens.get()[0], "password",
						tokens.get()[1]);

				if (hits.getTotalHits() == 0)
					throw new AuthenticationException(
							"invalid username or password");

				if (hits.getTotalHits() > 1)
					throw new RuntimeException(String.format(
							"more than one admin with username [%s]",
							tokens.get()[0]));

				Account account = getObjectMapper().readValue(
						hits.getAt(0).getSourceAsString(), Account.class);

				return Payload.ok().withHeader(
						AdminResource.SPACEDOG_KEY_HEADER,
						account.spaceDogKey());

			} else {
				throw new AuthenticationException(
						"no authorization header specified");
			}
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	public static Credentials checkCredentials(Context context)
			throws JsonParseException, JsonMappingException, IOException {

		String keyRawValue = context.header(SPACEDOG_KEY_HEADER);

		if (Strings.isNullOrEmpty(keyRawValue))
			throw new AuthenticationException("%s header not specified",
					SPACEDOG_KEY_HEADER);

		String[] key = keyRawValue.split(":", 3);

		if (key.length != 3)
			throw new AuthenticationException(
					"malformed spacedog key [%s], should be <backendId>:<apiKeyId>:<apiKeySecret>",
					keyRawValue);

		String backendId = key[0];
		String keyId = key[1];
		String keySecret = key[2];

		if (Strings.isNullOrEmpty(backendId))
			throw new AuthenticationException(
					"invalid spacedog key [%s], no backend id specified",
					keyRawValue);

		if (INTERNAL_INDICES.contains(backendId))
			throw new AuthenticationException(
					"this backend id [%s] is reserved", backendId);

		if (Strings.isNullOrEmpty(keyId))
			throw new AuthenticationException(
					"invalid spacedog key [%s], no apikey id specified",
					keyRawValue);

		if (Strings.isNullOrEmpty(keySecret))
			throw new AuthenticationException(
					"invalid spacedog key [%s], no apikey secret specified",
					keyRawValue);

		// check apikeys in account objects of spacedog internal index

		SearchHits accountHits = ElasticHelper.search(SPACEDOG_INDEX,
				ACCOUNT_TYPE, "backendId", backendId, "apiKey.id", keyId,
				"apiKey.secret", keySecret);

		if (accountHits.getTotalHits() == 0)
			throw new AuthenticationException("spacedog key [%s] not found",
					keyRawValue);

		if (accountHits.getTotalHits() > 1)
			throw new RuntimeException(
					String.format(
							"more than one apikey for backend id [%s] and apikey id [%s]",
							backendId, keyId));

		Optional<String[]> tokens = decodeAuthorizationHeader(context);

		if (tokens.isPresent()) {

			// check users in app index

			SearchHits userHits = ElasticHelper.search(backendId,
					UserResource.USER_TYPE, "username", tokens.get()[0],
					"password", tokens.get()[1]);

			if (userHits.getTotalHits() == 0)
				throw new AuthenticationException(
						"invalid username or password");

			if (userHits.getTotalHits() > 1)
				throw new RuntimeException(String.format(
						"more than one user with username [%s]",
						tokens.get()[0]));

			JsonObject userJson = JsonObject.readFrom(userHits.getAt(0)
					.getSourceAsString());

			return new Credentials(backendId, User.fromJsonObject(userJson));

		} else {

			Account account = getObjectMapper().readValue(
					accountHits.getAt(0).getSourceAsString(), Account.class);

			return new Credentials(backendId, account.apiKey);
		}
	}

	private static Optional<String[]> decodeAuthorizationHeader(Context context) {

		String authzHeaderValue = context.header(AUTHORIZATION_HEADER);

		if (Strings.isNullOrEmpty(authzHeaderValue))
			return Optional.empty();

		String[] schemeAndTokens = authzHeaderValue.split(" ", 2);

		if (schemeAndTokens.length != 2)
			throw new AuthenticationException("invalid authorization header");

		if (Strings.isNullOrEmpty(schemeAndTokens[0]))
			throw new AuthenticationException(
					"no authorization scheme specified");

		if (!schemeAndTokens[0].equalsIgnoreCase(BASIC_AUTHENTICATION_SCHEME))
			throw new AuthenticationException(
					"authorization scheme [%s] not supported",
					schemeAndTokens[0]);

		byte[] encodedBytes = schemeAndTokens[1].getBytes(UTF_8);

		String decoded = null;

		try {
			decoded = new String(Base64.getDecoder().decode(encodedBytes));
		} catch (IllegalArgumentException e) {
			throw new AuthenticationException(
					"authorization token is not base 64 encoded", e);
		}

		String[] tokens = decoded.split(":", 2);

		if (tokens.length != 2)
			throw new AuthenticationException("invalid authorization token");

		if (Strings.isNullOrEmpty(tokens[1]))
			throw new AuthenticationException("no password specified");

		return Optional.of(tokens);
	}
}
