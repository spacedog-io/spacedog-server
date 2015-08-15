package com.magiclabs.restapi;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Collections;
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

	private static final String ADMIN_INDEX = "admin";
	private static final String ACCOUNT_TYPE = "account";
	public static final String ACCOUNT_MAPPING = Json.builder()
			.stObj("account").add("dynamic", "strict").stObj("_id")
			.add("path", "id").end().stObj("properties").stObj("id")
			.add("type", "string").add("index", "not_analyzed").build()
			.toString();

	private static final Set<String> INTERNAL_INDICES = Sets
			.newHashSet(ADMIN_INDEX);

	public static final String ACCOUNT_ID_HEADER = "x-magic-app-id";
	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BASIC_AUTHENTICATION_SCHEME = "Basic";

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	void initAdminIndex() throws InterruptedException, ExecutionException {

		IndicesAdminClient indices = Start.getElasticClient().admin().indices();

		if (!indices.prepareExists(ADMIN_INDEX).get().isExists()) {
			indices.prepareCreate(ADMIN_INDEX)
					.addMapping(ACCOUNT_TYPE, ACCOUNT_MAPPING).get();
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
					.prepareSearch(ADMIN_INDEX).setTypes(ACCOUNT_TYPE)
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
			account.id = input.getString("id", null);
			account.checkUserInputValidity();

			byte[] accountBytes = getObjectMapper().writeValueAsBytes(account);
			Start.getElasticClient().prepareIndex(ADMIN_INDEX, ACCOUNT_TYPE)
					.setSource(accountBytes).get();

			User user = new User();
			user.username = input.getString("username", null);
			user.email = input.getString("email", null);
			user.password = input.getString("password", null);
			user.groups = Collections.singletonList("admin");
			user.checkUserInputValidity();

			// default app index is named after the account id
			Start.getElasticClient()
					.admin()
					.indices()
					.prepareCreate(account.id)
					.addMapping(UserResource.USER_TYPE,
							UserResource.getDefaultUserMapping()).get();

			DataResource.get().createInternal(account.id,
					UserResource.USER_TYPE, user.toJsonObject(), user.username);

			return created("/v1", ACCOUNT_TYPE, account.id);

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	/**
	 * Internal web service. Not accessible by clients.
	 */
	@Get("/:id")
	@Get("/:id/")
	public Payload get(String id, Context context) {
		try {
			GetResponse response = Start.getElasticClient()
					.prepareGet(ADMIN_INDEX, ACCOUNT_TYPE, id).get();

			if (!response.isExists())
				return error(HttpStatus.NOT_FOUND,
						"account for id [%s] not found", id);

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
	public Payload delete(String accountId, Context context) {
		try {
			DeleteResponse resp1 = Start.getElasticClient()
					.prepareDelete(ADMIN_INDEX, ACCOUNT_TYPE, accountId).get();

			if (!resp1.isFound())
				return error(HttpStatus.NOT_FOUND, "account id [%s] not found",
						accountId);

			DeleteIndexResponse resp2 = Start.getElasticClient().admin()
					.indices().prepareDelete(accountId).get();

			if (!resp2.isAcknowledged())
				return error(
						HttpStatus.INTERNAL_SERVER_ERROR,
						"account id [%s] internal index deletion not acknowledged",
						accountId);

			return success();
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	public static Credentials checkCredentials(Context context)
			throws JsonParseException, JsonMappingException, IOException {

		String accountId = context.header(ACCOUNT_ID_HEADER);

		if (Strings.isNullOrEmpty(accountId))
			throw new AuthenticationException(String.format(
					"account id header [%s] not specified", ACCOUNT_ID_HEADER));

		if (INTERNAL_INDICES.contains(accountId))
			throw new AuthenticationException(String.format(
					"account id [%s] is reserved for internal purposes",
					accountId));

		String authzHeaderValue = context.header(AUTHORIZATION_HEADER);

		if (Strings.isNullOrEmpty(authzHeaderValue))
			throw new AuthenticationException(String.format(
					"no authorization header [%s] specified",
					AUTHORIZATION_HEADER));

		String[] schemeAndValue = authzHeaderValue.split(" ", 2);

		if (schemeAndValue.length != 2)
			throw new AuthenticationException(
					"no authentication token specified");

		if (Strings.isNullOrEmpty(schemeAndValue[0]))
			throw new AuthenticationException(
					"authentication scheme not specified");

		if (!schemeAndValue[0].equalsIgnoreCase(BASIC_AUTHENTICATION_SCHEME))
			throw new AuthenticationException(String.format(
					"authentication scheme [%s] not supported",
					schemeAndValue[0]));

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
			throw new AuthenticationException("invalid authentication token");

		if (Strings.isNullOrEmpty(tokens[1]))
			throw new AuthenticationException("no password specified");

		try {
			SearchResponse response = Start
					.getElasticClient()
					.prepareSearch(accountId)
					.setTypes(UserResource.USER_TYPE)
					.setQuery(
							QueryBuilders.filteredQuery(QueryBuilders
									.matchAllQuery(), FilterBuilders.andFilter(
									FilterBuilders.termFilter("username",
											tokens[0]), FilterBuilders
											.termFilter("password", tokens[1]))))
					.get();

			if (response.getHits().getTotalHits() == 0)
				throw new AuthenticationException(
						"invalid username or password");

			JsonObject userJson = JsonObject.readFrom(response.getHits()
					.getAt(0).getSourceAsString());

			return new Credentials(accountId, User.fromJsonObject(userJson));

		} catch (IndexMissingException e) {
			throw new AuthenticationException(String.format(
					"invalid account id [%s]", accountId));
		}
	}

}
