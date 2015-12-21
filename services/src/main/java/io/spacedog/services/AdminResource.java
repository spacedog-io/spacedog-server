/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.IndicesAdminClient;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import io.spacedog.utils.BackendKey;
import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.UserUtils;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/v1/admin")
public class AdminResource extends AbstractResource {

	public static final String ADMIN_INDEX = "spacedog";
	public static final String ACCOUNT_TYPE = "account";
	public static final Set<String> INTERNAL_INDICES = Sets.newHashSet(ADMIN_INDEX);

	// properties

	public static final String BACKEND_KEY = "backendKey";

	void initSpacedogIndex() throws InterruptedException, ExecutionException, IOException {

		String accountMapping = Resources.toString(Resources.getResource("io/spacedog/services/account-mapping.json"),
				Utils.UTF8);

		IndicesAdminClient indices = Start.get().getElasticClient().admin().indices();

		if (!indices.prepareExists(ADMIN_INDEX).get().isExists()) {
			indices.prepareCreate(ADMIN_INDEX).addMapping(ACCOUNT_TYPE, accountMapping).get();
		}
	}

	/**
	 * Internal service only accessible to spacedog administrators.
	 */
	@Get("/account")
	@Get("/account/")
	public Payload getAll(Context context) {
		// TODO only spacedog super administrator authenticated should be able
		// to review all accounts from a super admin console.
		return Payload.unauthorized("spacedog.io");

		// checkAdminCredentialsOnly(context);
		//
		// SearchResponse response = Start.get().getElasticClient()
		// .prepareSearch(SPACEDOG_INDEX).setTypes(ACCOUNT_TYPE)
		// .setQuery(QueryBuilders.matchAllQuery()).get();
		//
		// return extractResults(response);
	}

	@Get("/user/:username/check")
	@Get("/user/:username/check")
	public Payload checkUsername(String username) {
		// TODO add a spacedog super admin key to log what admin app is checking
		// existence of an account by username. Everybody should not be able to
		// do this.
		return checkExistence(ADMIN_INDEX, ACCOUNT_TYPE, "username", username);
	}

	@Get("/backend/:id/check")
	@Get("/backend/:id/check")
	public Payload checkBackendId(String backendId) {
		// TODO add a spacedog super admin key to log what admin app is checking
		// existence of an account by backend id. Everybody should not be able
		// to do this.
		return checkExistence(ADMIN_INDEX, ACCOUNT_TYPE, "backendId", backendId);
	}

	@Post("/account")
	@Post("/account/")
	public Payload signUp(String body, Context context) throws IOException {
		ObjectNode input = Json.readObjectNode(body);

		Account account = new Account();
		account.backendId = input.get("backendId").textValue();
		account.username = input.get("username").textValue();
		account.email = input.get("email").textValue();
		String password = input.get("password").textValue();
		Account.checkPasswordValidity(password);
		account.hashedPassword = UserUtils.hashPassword(password);
		account.backendKey = new BackendKey();
		account.checkAccountInputValidity();

		ElasticHelper.get().refresh(true, ADMIN_INDEX);

		if (ElasticHelper.get().search(ADMIN_INDEX, ACCOUNT_TYPE, "username", account.username).getTotalHits() > 0)
			return PayloadHelper.invalidParameters("username", account.username,
					String.format("administrator username [%s] is not available", account.username));

		if (ElasticHelper.get().search(ADMIN_INDEX, ACCOUNT_TYPE, "backendId", account.backendId).getTotalHits() > 0)
			return PayloadHelper.invalidParameters("backendId", account.backendId,
					String.format("backend id [%s] is not available", account.backendId));

		byte[] accountBytes = Json.getMapper().writeValueAsBytes(account);
		long version = Start.get().getElasticClient().prepareIndex(ADMIN_INDEX, ACCOUNT_TYPE).setSource(accountBytes)
				.get().getVersion();

		// backend index is named after the backend id
		Start.get().getElasticClient().admin().indices().prepareCreate(account.backendId)
				.addMapping(UserResource.USER_TYPE, UserResource.getDefaultUserMapping()).get();

		ElasticHelper.get().refresh(true, ADMIN_INDEX);

		ObjectNode payloadContent = PayloadHelper
				.savedBuilder(true, "/v1/admin", ACCOUNT_TYPE, account.backendId, version)//
				.put(AdminResource.BACKEND_KEY, account.defaultClientKey()).build();

		return PayloadHelper.json(payloadContent, HttpStatus.CREATED)//
				.withHeader(SpaceHeaders.BACKEND_KEY, account.defaultClientKey());
	}

	@Get("/account/:id")
	@Get("/account/:id/")
	public Payload get(String backendId, Context context) throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = SpaceContext.checkAdminCredentialsFor(backendId);

		GetResponse response = Start.get().getElasticClient()//
				.prepareGet(ADMIN_INDEX, ACCOUNT_TYPE, credentials.backendId()).get();

		if (!response.isExists())
			return PayloadHelper.error(HttpStatus.INTERNAL_SERVER_ERROR,
					"no account found for backend [%s] and admin user [%s]", credentials.backendId(),
					credentials.name());

		return new Payload(PayloadHelper.JSON_CONTENT_UTF8, response.getSourceAsString());
	}

	@Delete("/account/:id")
	@Delete("/account/:id/")
	public Payload delete(String backendId, Context context)
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = SpaceContext.checkAdminCredentialsFor(backendId);

		ElasticHelper.get().refresh(true, ADMIN_INDEX);

		DeleteResponse accountDeleteResp = Start.get().getElasticClient()
				.prepareDelete(ADMIN_INDEX, ACCOUNT_TYPE, credentials.backendId()).get();

		if (!accountDeleteResp.isFound())
			return PayloadHelper.error(HttpStatus.INTERNAL_SERVER_ERROR,
					"no account found for backend [%s] and admin user [%s]", credentials.backendId(),
					credentials.name());

		DeleteIndexResponse indexDeleteResp = Start.get().getElasticClient().admin().indices()
				.prepareDelete(credentials.backendId()).get();

		if (!indexDeleteResp.isAcknowledged())
			return PayloadHelper.error(HttpStatus.INTERNAL_SERVER_ERROR,
					"internal index deletion not acknowledged for account with backend [%s] ", credentials.backendId());

		ElasticHelper.get().refresh(true, ADMIN_INDEX);

		return PayloadHelper.success();
	}

	@Get("/login")
	@Get("/login/")
	public Payload login(Context context) throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		String backendKey = credentials.backendKeyAsString().get();
		// TODO return backend key in json only?
		return PayloadHelper.json(PayloadHelper.minimalBuilder(HttpStatus.OK).put(BACKEND_KEY, backendKey))//
				.withHeader(SpaceHeaders.BACKEND_KEY, backendKey);
	}

	//
	// Singleton
	//

	private static AdminResource singleton = new AdminResource();

	static AdminResource get() {
		return singleton;
	}

	private AdminResource() {
	}
}
