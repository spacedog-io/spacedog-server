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
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import io.spacedog.utils.BackendKey;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/v1/admin")
public class AccountResource extends AbstractResource {

	public static final String ADMIN_INDEX = "spacedog";
	public static final String ACCOUNT_TYPE = "account";
	public static final Set<String> INTERNAL_INDICES = Sets.newHashSet(ADMIN_INDEX);

	//
	// properties
	//

	public static final String BACKEND_KEY = "backendKey";

	//
	// SpaceDog index init
	//

	void initSpacedogIndex() throws InterruptedException, ExecutionException, IOException {

		String accountMapping = Resources.toString(Resources.getResource(//
				"io/spacedog/services/account-mapping.json"), Utils.UTF8);

		String logMapping = Resources.toString(Resources.getResource(//
				"io/spacedog/services/log-mapping.json"), Utils.UTF8);

		IndicesAdminClient indices = Start.get().getElasticClient().admin().indices();

		if (!indices.prepareExists(ADMIN_INDEX).get().isExists()) {

			indices.prepareCreate(ADMIN_INDEX)//
					.addMapping(ACCOUNT_TYPE, accountMapping)//
					.addMapping(LogResource.TYPE, logMapping)//
					.get();
		} else {

			indices.preparePutMapping(ADMIN_INDEX)//
					.setType(ACCOUNT_TYPE)//
					.setSource(accountMapping)//
					.get();

			indices.preparePutMapping(ADMIN_INDEX)//
					.setType(LogResource.TYPE)//
					.setSource(logMapping)//
					.get();
		}
	}

	//
	// Routes
	//

	@Get("/account")
	@Get("/account/")
	public Payload getAll(Context context) throws JsonParseException, JsonMappingException, IOException {

		SpaceContext.checkSuperDogCredentials();

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(ADMIN_INDEX)//
				.setTypes(ACCOUNT_TYPE)//
				.setVersion(true)//
				.setQuery(QueryBuilders.matchAllQuery())//
				.get();

		JsonBuilder<ObjectNode> builder = Json.objectBuilder()//
				.put("took", response.getTookInMillis())//
				.put("total", response.getHits().getTotalHits())//
				.array("results");

		for (SearchHit hit : response.getHits().getHits()) {
			ObjectNode object = Json.readObjectNode(hit.sourceAsString());
			// TODO remove this when all passwords have moved
			// to dedicated indices
			object.remove("hashedPassword");
			object.with("meta")//
					.put("id", hit.id())//
					.put("type", hit.type())//
					.put("version", hit.version());
			builder.node(object);
		}

		return PayloadHelper.json(builder);
	}

	@Get("/account/username/:username")
	@Get("/account/username/:username/")
	public Payload getAccountUsernameExists(String username) {
		// TODO add a spacedog super admin key to log what admin app is checking
		// existence of an account by username. Everybody should not be able to
		// do this.
		if (Start.get().configuration().isSuperDog(username))
			return Payload.ok();
		long totalHits = ElasticHelper.get().search(ADMIN_INDEX, ACCOUNT_TYPE, "username", username).getTotalHits();
		return totalHits == 0 ? Payload.notFound() : Payload.ok();
	}

	@Get("/account/backendId/:id")
	@Get("/account/backendId/:id/")
	public Payload getAccountBackendIdExists(String id) {
		// TODO add a spacedog super admin key to log what admin app is checking
		// existence of an account by backend id. Everybody should not be able
		// to do this.
		long totalHits = ElasticHelper.get().search(ADMIN_INDEX, ACCOUNT_TYPE, "backendId", id).getTotalHits();
		return totalHits == 0 ? Payload.notFound() : Payload.ok();
	}

	@Post("/account")
	@Post("/account/")
	public Payload post(String body, Context context) throws IOException {
		ObjectNode input = Json.readObjectNode(body);

		Account account = new Account();
		account.backendId = input.get("backendId").textValue();
		account.username = input.get("username").textValue();
		account.email = input.get("email").textValue();
		account.hashedPassword = Passwords.checkAndHash(//
				input.get("password").textValue());
		account.backendKey = new BackendKey();
		account.checkAccountInputValidity();

		if (Start.get().configuration().isSuperDog(account.username))
			return PayloadHelper.invalidParameters("username", account.username,
					String.format("administrator username [%s] is not available", account.username));

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

		// Account is created, new account credentials are valid and can be set
		// in space context if none are set
		SpaceContext.setCredentials(Credentials.fromAdmin(//
				account.backendId, account.username, account.email, account.backendKey), false);

		ObjectNode payloadContent = PayloadHelper
				.savedBuilder(true, "/v1/admin", ACCOUNT_TYPE, account.backendId, version)//
				.put(AccountResource.BACKEND_KEY, account.defaultClientKey()).build();

		return PayloadHelper.json(payloadContent, HttpStatus.CREATED)//
				.withHeader(SpaceHeaders.BACKEND_KEY, account.defaultClientKey());
	}

	@Get("/account/:id")
	@Get("/account/:id/")
	public Payload getById(String backendId) throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = SpaceContext.checkAdminCredentialsFor(backendId);

		GetResponse response = Start.get().getElasticClient()//
				.prepareGet(ADMIN_INDEX, ACCOUNT_TYPE, credentials.backendId()).get();

		if (!response.isExists())
			return PayloadHelper.error(HttpStatus.INTERNAL_SERVER_ERROR,
					"no account found for backend [%s] and admin user [%s]", credentials.backendId(),
					credentials.name());

		ObjectNode object = Json.readObjectNode(response.getSourceAsString());
		// TODO remove this when all passwords have moved
		// to dedicated indices
		object.remove("hashedPassword");
		object.with("meta")//
				.put("id", response.getId())//
				.put("type", response.getType())//
				.put("version", response.getVersion());

		return PayloadHelper.json(object);
	}

	@Delete("/account/:id")
	@Delete("/account/:id/")
	public Payload deleteById(String backendId, Context context)
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
	public Payload getLogin() throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		String backendKey = credentials.backendKeyAsString().get();
		// TODO return backend key in json only?
		return PayloadHelper.json(PayloadHelper.minimalBuilder(HttpStatus.OK).put(BACKEND_KEY, backendKey))//
				.withHeader(SpaceHeaders.BACKEND_KEY, backendKey);
	}

	//
	// Singleton
	//

	private static AccountResource singleton = new AccountResource();

	static AccountResource get() {
		return singleton;
	}

	private AccountResource() {
	}
}
