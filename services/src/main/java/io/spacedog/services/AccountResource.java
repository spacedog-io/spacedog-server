/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import io.spacedog.utils.BackendKey;
import io.spacedog.utils.Internals;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.SpaceParams;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/v1")
public class AccountResource extends AbstractResource {

	public static final String ADMIN_BACKEND = "spacedog";
	public static final String ACCOUNT_TYPE = "account";
	public static final Set<String> INTERNAL_BACKENDS = Sets.newHashSet(ADMIN_BACKEND);

	//
	// properties
	//

	public static final String BACKEND_KEY = "backendKey";

	//
	// SpaceDog index init
	//

	void initSpacedogBackend() throws InterruptedException, ExecutionException, IOException {

		ElasticClient client = Start.get().getElasticClient();

		String accountMapping = Resources.toString(Resources.getResource(//
				"io/spacedog/services/account-mapping.json"), Utils.UTF8);

		if (client.exists(ADMIN_BACKEND, ACCOUNT_TYPE))
			client.putMapping(ADMIN_BACKEND, ACCOUNT_TYPE, accountMapping);
		else
			client.createIndex(ADMIN_BACKEND, ACCOUNT_TYPE, accountMapping, 1, 1);

		String logMapping = Resources.toString(Resources.getResource(//
				"io/spacedog/services/log-mapping.json"), Utils.UTF8);

		if (client.exists(ADMIN_BACKEND, LogResource.TYPE))
			client.putMapping(ADMIN_BACKEND, LogResource.TYPE, logMapping);
		else
			client.createIndex(ADMIN_BACKEND, LogResource.TYPE, logMapping, 1, 1);
	}

	//
	// Routes
	//

	@Get("/account")
	@Get("/account/")
	@Get("/admin/account")
	@Get("/admin/account/")
	public Payload getAll(Context context) {

		SpaceContext.checkSuperDogCredentials();

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(ADMIN_BACKEND, ACCOUNT_TYPE)//
				.setTypes(ACCOUNT_TYPE)//
				.setVersion(true)//
				.setQuery(QueryBuilders.matchAllQuery())//
				.setFrom(context.request().query().getInteger("from", 0))//
				.setSize(context.request().query().getInteger("size", 10))//
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

		return Payloads.json(builder);
	}

	@Get("/account/username/:username")
	@Get("/account/username/:username/")
	@Get("/admin/account/username/:username")
	@Get("/admin/account/username/:username/")
	public Payload getAccountUsernameExists(String username) {
		// TODO add a spacedog super admin key to log what admin app is checking
		// existence of an account by username. Everybody should not be able to
		// do this.
		if (Start.get().configuration().isSuperDog(username))
			return Payload.ok();
		long totalHits = DataStore.get().search(ADMIN_BACKEND, ACCOUNT_TYPE, "username", username).getTotalHits();
		return totalHits == 0 ? Payload.notFound() : Payload.ok();
	}

	@Get("/account/backendId/:id")
	@Get("/account/backendId/:id/")
	@Get("/admin/account/backendId/:id")
	@Get("/admin/account/backendId/:id/")
	public Payload getAccountBackendIdExists(String id) {
		// TODO add a spacedog super admin key to log what admin app is checking
		// existence of an account by backend id. Everybody should not be able
		// to do this.
		long totalHits = DataStore.get().search(ADMIN_BACKEND, ACCOUNT_TYPE, "backendId", id).getTotalHits();
		return totalHits == 0 ? Payload.notFound() : Payload.ok();
	}

	@Post("/account")
	@Post("/account/")
	@Post("/admin/account")
	@Post("/admin/account/")
	public Payload post(String body, Context context) {
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
			return Payloads.invalidParameters("username", account.username,
					String.format("administrator username [%s] is not available", account.username));

		DataStore.get().refreshType(ADMIN_BACKEND, ACCOUNT_TYPE);

		if (DataStore.get().search(ADMIN_BACKEND, ACCOUNT_TYPE, "username", account.username).getTotalHits() > 0)
			return Payloads.invalidParameters("username", account.username,
					String.format("administrator username [%s] is not available", account.username));

		if (DataStore.get().search(ADMIN_BACKEND, ACCOUNT_TYPE, "backendId", account.backendId).getTotalHits() > 0)
			return Payloads.invalidParameters("backendId", account.backendId,
					String.format("backend id [%s] is not available", account.backendId));

		byte[] accountBytes;

		try {
			accountBytes = Json.getMapper().writeValueAsBytes(account);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

		long version = Start.get().getElasticClient()//
				.index(ADMIN_BACKEND, ACCOUNT_TYPE, account.backendId, accountBytes)//
				.getVersion();

		int shards = context.query().getInteger(SpaceParams.SHARDS, 1);
		int replicas = context.query().getInteger(SpaceParams.REPLICAS, 1);

		Start.get().getElasticClient().createIndex(//
				account.backendId, UserResource.USER_TYPE, UserResource.getDefaultUserMapping(), shards, replicas);

		DataStore.get().refreshType(ADMIN_BACKEND, ACCOUNT_TYPE);

		// Account is created, new account credentials are valid and can be set
		// in space context if none are set
		SpaceContext.setCredentials(Credentials.fromAdmin(//
				account.backendId, account.username, account.email, account.backendKey), false);

		if (!isTest(context))
			Internals.get().notify(//
					Start.get().configuration().superdogNotificationTopic(), //
					String.format("New account (%s)",
							AbstractResource.spaceUrl("/v1", ACCOUNT_TYPE, account.backendId).toString()), //
					String.format("account backend = %s\naccount email = %s", account.backendId, account.email));

		ObjectNode payloadContent = Payloads.savedBuilder(true, "/v1", ACCOUNT_TYPE, account.backendId, version)//
				.put(AccountResource.BACKEND_KEY, account.defaultClientKey()).build();

		return Payloads.json(payloadContent, HttpStatus.CREATED)//
				.withHeader(SpaceHeaders.BACKEND_KEY, account.defaultClientKey());
	}

	@Get("/account/:id")
	@Get("/account/:id/")
	@Get("/admin/account/:id")
	@Get("/admin/account/:id/")
	public Payload getById(String backendId) {
		Credentials credentials = SpaceContext.checkAdminCredentialsFor(backendId);

		GetResponse response = Start.get().getElasticClient()//
				.get(ADMIN_BACKEND, ACCOUNT_TYPE, credentials.backendId());

		if (!response.isExists())
			return Payloads.error(HttpStatus.INTERNAL_SERVER_ERROR,
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

		return Payloads.json(object);
	}

	@Delete("/account/:id")
	@Delete("/account/:id/")
	@Delete("/admin/account/:id")
	@Delete("/admin/account/:id/")
	public Payload deleteById(String backendId, Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentialsFor(backendId);

		DataStore.get().refreshType(ADMIN_BACKEND, ACCOUNT_TYPE);

		DeleteResponse accountDeleteResp = Start.get().getElasticClient()//
				.delete(ADMIN_BACKEND, ACCOUNT_TYPE, credentials.backendId());

		if (!accountDeleteResp.isFound())
			return Payloads.error(HttpStatus.INTERNAL_SERVER_ERROR,
					"no account found for backend [%s] and admin user [%s]", credentials.backendId(),
					credentials.name());

		Start.get().getElasticClient()//
				.deleteAllIndices(credentials.backendId());

		DataStore.get().refreshType(ADMIN_BACKEND, ACCOUNT_TYPE);

		if (!isTest(context) || !Start.get().configuration().isOffline()) {
			FileResource.get().deleteAll();
			ShareResource.get().deleteAll();
		}

		return Payloads.success();
	}

	@Get("/account/login")
	@Get("/account/login/")
	@Get("/admin/login")
	@Get("/admin/login/")
	public Payload getLogin() {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		String backendKey = credentials.backendKeyAsString().get();
		// TODO return backend key in json only?
		return Payloads.json(Payloads.minimalBuilder(HttpStatus.OK).put(BACKEND_KEY, backendKey))//
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
