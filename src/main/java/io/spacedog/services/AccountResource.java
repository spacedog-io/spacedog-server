package io.spacedog.services;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
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
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;

import com.eclipsesource.json.JsonObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

@Prefix("/v1/account")
public class AccountResource extends AbstractResource {

	// singleton begins

	private static AccountResource singleton = new AccountResource();

	static AccountResource get() {
		return singleton;
	}

	private AccountResource() {
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
	@Get("")
	@Get("/")
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
	@Post("")
	@Post("/")
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

			return created("/v1", ACCOUNT_TYPE, account.backendId).withHeader(
					AccountResource.SPACEDOG_KEY_HEADER, account.spaceDogKey());

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	/**
	 * Internal web service. Not accessible by clients.
	 */
	@Get("/:id")
	@Get("/:id/")
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
	@Put("/:id")
	@Put("/:id/")
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
	@Delete("/:id")
	@Delete("/:id/")
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

	public static Credentials checkCredentials(Context context)
			throws JsonParseException, JsonMappingException, IOException {

		String keyRawValue = context.header(SPACEDOG_KEY_HEADER);

		if (Strings.isNullOrEmpty(keyRawValue))
			throw new AuthenticationException("[%s] header not specified",
					SPACEDOG_KEY_HEADER);

		String[] key = keyRawValue.split(":", 3);

		if (key.length != 3)
			throw new AuthenticationException(
					"invalid spacedog key [%s], no backend id, key id or key secret",
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

		String authzHeaderValue = context.header(AUTHORIZATION_HEADER);

		if (Strings.isNullOrEmpty(authzHeaderValue)) {

			if (Strings.isNullOrEmpty(keyId))
				throw new AuthenticationException(
						"invalid spacedog key [%s], no key id specified",
						keyRawValue);

			if (Strings.isNullOrEmpty(keySecret))
				throw new AuthenticationException(
						"invalid spacedog key [%s], no secret specified",
						keyRawValue);

			try {
				// check apikeys in account objects of spacedog internal index
				SearchResponse response = Start
						.getElasticClient()
						.prepareSearch(SPACEDOG_INDEX)
						.setTypes(ACCOUNT_TYPE)
						.setQuery(
								QueryBuilders.filteredQuery(QueryBuilders
										.matchAllQuery(), FilterBuilders
										.andFilter(FilterBuilders.termFilter(
												"backendId", backendId), //
												FilterBuilders.termFilter(
														"apiKey.id", keyId),//
												FilterBuilders.termFilter(
														"apiKey.secret",
														keySecret)))).get();

				if (response.getHits().getTotalHits() == 0)
					throw new AuthenticationException(
							"invalid username or password");

				Account account = getObjectMapper().readValue(
						response.getHits().getAt(0).getSourceAsString(),
						Account.class);

				// JsonObject userJson = JsonObject.readFrom(response.getHits()
				// .getAt(0).getSourceAsString());

				return new Credentials(backendId, account.apiKey);

			} catch (IndexMissingException e) {
				throw new AuthenticationException("invalid account id [%s]",
						backendId);
			}

		} else {

			String[] schemeAndValue = authzHeaderValue.split(" ", 2);

			if (schemeAndValue.length != 2)
				throw new AuthenticationException(
						"no authentication token specified");

			if (Strings.isNullOrEmpty(schemeAndValue[0]))
				throw new AuthenticationException(
						"authentication scheme not specified");

			if (!schemeAndValue[0]
					.equalsIgnoreCase(BASIC_AUTHENTICATION_SCHEME))
				throw new AuthenticationException(
						"authentication scheme [%s] not supported",
						schemeAndValue[0]);

			byte[] encodedBytes = schemeAndValue[1].getBytes(UTF_8);

			String decoded = null;

			try {
				decoded = new String(Base64.getDecoder().decode(encodedBytes));
			} catch (IllegalArgumentException e) {
				throw new AuthenticationException(
						"authentication token is not base 64 encoded", e);
			}

			String[] tokens = decoded.split(":", 2);

			if (tokens.length != 2)
				throw new AuthenticationException(
						"invalid authentication token");

			if (Strings.isNullOrEmpty(tokens[1]))
				throw new AuthenticationException("no password specified");

			try {
				// check users in app index
				SearchResponse response = Start
						.getElasticClient()
						.prepareSearch(backendId)
						.setTypes(UserResource.USER_TYPE)
						.setQuery(
								QueryBuilders.filteredQuery(QueryBuilders
										.matchAllQuery(), FilterBuilders
										.andFilter(FilterBuilders.termFilter(
												"username", tokens[0]),
												FilterBuilders.termFilter(
														"password", tokens[1]))))
						.get();

				if (response.getHits().getTotalHits() == 0)
					throw new AuthenticationException(
							"invalid username or password");

				JsonObject userJson = JsonObject.readFrom(response.getHits()
						.getAt(0).getSourceAsString());

				return new Credentials(backendId, User.fromJsonObject(userJson));

			} catch (IndexMissingException e) {
				throw new AuthenticationException("invalid account id [%s]",
						backendId);
			}
		}
	}
}
