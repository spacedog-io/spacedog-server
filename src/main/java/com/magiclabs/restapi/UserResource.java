package com.magiclabs.restapi;

import java.util.concurrent.ExecutionException;

import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;

import com.eclipsesource.json.JsonObject;

@Prefix("/v1")
public class UserResource extends AbstractResource {

	static final String USER_TYPE = "user";

	static final String USER_DEFAULT_MAPPING = Json.builder().stObj("user")
			.add("dynamic", "strict").stObj("_id").add("path", "username")
			.end().stObj("properties").stObj("username").add("type", "string")
			.add("index", "not_analyzed").end().stObj("accountId")
			.add("type", "string").add("index", "not_analyzed").end()
			.stObj("email").add("type", "string").add("index", "not_analyzed")
			.end().stObj("password").add("type", "string")
			.add("index", "not_analyzed").end().stObj("groups")
			.add("type", "string").add("index", "not_analyzed").end().build()
			.toString();

	static final JsonObject USER_DEFAULT_SCHEMA_OBJECT = SchemaBuilder
			.builder(USER_TYPE).id("username").add("username", "code", true)
			.add("password", "code", true).add("email", "code", true)
			.add("accountId", "code", true).add("groups", "code", false)
			.build();

	static final String USER_DEFAULT_SCHEMA = USER_DEFAULT_SCHEMA_OBJECT
			.toString();

	void initSchema(String accountId) throws InterruptedException,
			ExecutionException {
		new MetaResource().upsertSchemaInternal(USER_TYPE, USER_DEFAULT_SCHEMA,
				accountId);
	}

	@Get("/login")
	@Get("/login/")
	public Payload login(Context context) {
		try {
			AccountResource.checkCredentials(context);
			return Payload.ok();
		} catch (Throwable throwable) {
			return AbstractResource.toPayload(throwable);
		}
	}

	@Get("/logout")
	@Get("/logout/")
	public Payload logout(Context context) {
		try {
			AccountResource.checkCredentials(context);
			return Payload.ok();
		} catch (Throwable throwable) {
			return AbstractResource.toPayload(throwable);
		}
	}

	@Get("/user")
	@Get("/user/")
	public Payload getAll(Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);

			SearchResponse response = Start.getElasticClient()
					.prepareSearch(credentials.getAccountId())
					.setTypes(USER_TYPE)
					.setQuery(QueryBuilders.matchAllQuery()).get();

			return extractResults(response);
		} catch (Throwable throwable) {
			return AbstractResource.toPayload(throwable);
		}
	}

	@Post("/user")
	@Post("/user/")
	public Payload signUp(String body, Context context) {
		try {
			JsonObject input = JsonObject.readFrom(body);
			Credentials credentials = AccountResource.checkCredentials(context);

			User user = new User();
			user.username = input.getString("username", null);
			user.email = input.getString("email", null);
			user.password = input.getString("password", null);
			user.checkUserInputValidity();

			byte[] userBytes = getObjectMapper().writeValueAsBytes(user);

			IndexResponse resp2 = Start.getElasticClient()
					.prepareIndex(credentials.getAccountId(), USER_TYPE)
					.setSource(userBytes).get();

			return created(USER_TYPE, resp2.getId());

		} catch (Throwable throwable) {
			return AbstractResource.toPayload(throwable);
		}
	}

	@Get("/user/:id")
	@Get("/user/:id/")
	public Payload get(String id, Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);
			GetResponse response = Start.getElasticClient()
					.prepareGet(credentials.getAccountId(), USER_TYPE, id)
					.get();

			if (!response.isExists())
				return notFound("user for id [%s] not found", id);

			return new Payload(JSON_CONTENT, response.getSourceAsBytes(),
					HttpStatus.OK);
		} catch (Throwable throwable) {
			return AbstractResource.toPayload(throwable);
		}
	}

	@Delete("/user/:id")
	@Delete("/user/:id/")
	public Payload delete(String id, Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);

			DeleteResponse response = Start.getElasticClient()
					.prepareDelete(credentials.getAccountId(), USER_TYPE, id)
					.get();

			return response.isFound() ? done() : notFound(
					"user for id [%s] not found", id);

		} catch (Throwable throwable) {
			return AbstractResource.toPayload(throwable);
		}
	}

}
