/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.services.Credentials.Level;
import io.spacedog.services.CredentialsResource.SignUp;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SchemaBuilder2;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class UserResource extends Resource {

	//
	// User constants and schema
	//

	public static final String TYPE = "user";

	public static SchemaBuilder2 getDefaultUserSchema() {
		return SchemaBuilder2.builder(TYPE, USERNAME)//
				.stringProperty(USERNAME, true)//
				.stringProperty(EMAIL, true);
	}

	//
	// Routes
	//

	@Get("/v1/user")
	@Get("/v1/user/")
	@Get("/1/user")
	@Get("/1/user/")
	@Get("/1/data/user")
	@Get("/1/data/user/")
	public Payload getAll(Context context) {
		SpaceContext.checkUserCredentials();
		return DataResource.get().getByType(TYPE, context);
	}

	@Delete("/v1/user")
	@Delete("/v1/user/")
	@Delete("/1/user")
	@Delete("/1/user/")
	@Delete("/1/data/user")
	@Delete("/1/data/user/")
	public Payload deleteAll(Context context) {
		Credentials credentials = SpaceContext.checkSuperAdminCredentials();
		ElasticClient elastic = Start.get().getElasticClient();

		elastic.refreshType(SPACEDOG_BACKEND, CredentialsResource.TYPE);

		// delete all backend users and only users
		QuerySourceBuilder query = new QuerySourceBuilder().setQuery(//
				QueryBuilders.boolQuery()//
						.must(QueryBuilders.termQuery(BACKEND_ID, credentials.backendId()))//
						.must(QueryBuilders.termQuery(CREDENTIALS_LEVEL, Level.USER.toString())));

		SearchResponse search = elastic.prepareSearch(SPACEDOG_BACKEND, CredentialsResource.TYPE)//
				.setQuery(query.toString())//
				.setSize(1000)//
				.setFetchSource(false)//
				.setScroll(TimeValue.timeValueMinutes(1))//
				.get();

		SearchHit[] hits = search.getHits().getHits();
		int totalDeleted = hits.length;

		while (hits.length > 0) {
			BulkRequestBuilder bulk = elastic.prepareBulk();
			for (SearchHit hit : hits) {
				bulk.add(new DeleteRequest(//
						elastic.toAlias(SPACEDOG_BACKEND, CredentialsResource.TYPE), CredentialsResource.TYPE,
						hit.getId()));
				bulk.add(new DeleteRequest(//
						elastic.toAlias(credentials.backendId(), TYPE), TYPE,
						CredentialsResource.fromCredentialsId(hit.getId())[1]));
			}
			bulk.get();

			if (hits.length < 1000)
				break;

			search = elastic.prepareSearchScroll(search.getScrollId()).get();
			hits = search.getHits().hits();
			totalDeleted += hits.length;
		}

		return Payloads.json(Payloads.minimalBuilder(200).put("totalDeleted", totalDeleted));
	}

	@Post("/v1/user")
	@Post("/v1/user/")
	@Post("/1/user")
	@Post("/1/user/")
	@Post("/1/data/user")
	@Post("/1/data/user/")
	public Payload signUp(String body, Context context) {

		Credentials credentials = SpaceContext.checkCredentials();
		SignUp userSignUp = new SignUp(credentials.backendId(), Level.USER, body);

		if (credentials.isAtLeastUser())
			userSignUp.createdBy = Optional.of(credentials.name());

		if (exists(userSignUp.credentials.backendId(), userSignUp.credentials.name()))
			throw Exceptions.illegalArgument(//
					"user [%s] for backend [%s] already exists", //
					userSignUp.credentials.name(), userSignUp.credentials.backendId());

		CredentialsResource.get().create(userSignUp);

		DataStore.get().createObject(userSignUp.credentials.backendId(), //
				TYPE, //
				userSignUp.credentials.name(), //
				userSignUp.data, //
				userSignUp.createdBy.isPresent() ? userSignUp.createdBy.get() : userSignUp.credentials.name());

		JsonBuilder<ObjectNode> savedBuilder = Payloads.savedBuilder(true, credentials.backendId(), "/1", TYPE,
				userSignUp.credentials.name());

		if (userSignUp.passwordResetCode.isPresent())
			savedBuilder.put(PASSWORD_RESET_CODE, userSignUp.passwordResetCode.get());

		return Payloads.json(savedBuilder, HttpStatus.CREATED)//
				.withHeader(Payloads.HEADER_OBJECT_ID, userSignUp.credentials.name());
	}

	@Get("/v1/user/:username")
	@Get("/v1/user/:username/")
	@Get("/1/user/:username")
	@Get("/1/user/:username/")
	@Get("/1/data/user/:username")
	@Get("/1/data/user/:username/")
	public Payload get(String username, Context context) {
		SpaceContext.checkUserCredentials();
		return DataResource.get().getById(TYPE, username, context);
	}

	@Put("/v1/user/:username")
	@Put("/v1/user/:username/")
	@Put("/1/user/:username")
	@Put("/1/user/:username/")
	@Put("/1/data/user/:username")
	@Put("/1/data/user/:username/")
	public Payload put(String username, String jsonBody, Context context) {
		// TODO check that this can not be used to create new users
		// without credentials
		SpaceContext.checkUserCredentials(username);
		return DataResource.get().put(TYPE, username, jsonBody, context);
	}

	@Delete("/v1/user/:username")
	@Delete("/v1/user/:username/")
	@Delete("/1/user/:username")
	@Delete("/1/user/:username/")
	@Delete("/1/data/user/:username")
	@Delete("/1/data/user/:username/")
	public Payload delete(String username, Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials(username);
		ElasticClient elastic = Start.get().getElasticClient();

		elastic.delete(credentials.backendId(), TYPE, username);
		DeleteResponse response = CredentialsResource.get().delete(credentials.backendId(), username);

		return response.isFound() ? Payloads.success() //
				: Payloads.error(HttpStatus.NOT_FOUND, "user [%s] not found", username);
	}

	@Delete("/v1/user/:username/password")
	@Delete("/v1/user/:username/password")
	@Delete("/1/user/:username/password")
	@Delete("/1/user/:username/password")
	public Payload deletePassword(String username, Context context) {
		// TODO check this username is attached to a real user ?
		// manage admin users with another resource ?
		Credentials credentials = SpaceContext.checkAdminCredentials();
		String passwordResetCode = CredentialsResource.get().doDeletePassword(credentials.backendId(), username);
		return Payloads.json(//
				Payloads.savedBuilder(false, credentials.backendId(), "/1", TYPE, username)//
						.put(PASSWORD_RESET_CODE, passwordResetCode));
	}

	@Post("/v1/user/:username/password")
	@Post("/v1/user/:username/password")
	@Post("/1/user/:username/password")
	@Post("/1/user/:username/password")
	public Payload postPassword(String username, Context context) {
		// TODO check this username is attached to a real user ?
		// manage admin users with another resource ?
		String backendId = SpaceContext.checkCredentials().backendId();
		IndexResponse response = CredentialsResource.get().doPostPassword(backendId, username, context);
		return Payloads.saved(false, backendId, "/1", TYPE, username, response.getVersion());
	}

	@Put("/v1/user/:username/password")
	@Put("/v1/user/:username/password")
	@Put("/1/user/:username/password")
	@Put("/1/user/:username/password")
	public Payload putPassword(String username, Context context) {
		// TODO check this username is attached to a real user ?
		// manage admin users with another resource ?
		Credentials credentials = SpaceContext.checkUserCredentials(username);
		UpdateResponse response = CredentialsResource.get().doPutPassword(credentials.backendId(), username, context);
		return Payloads.saved(false, credentials.backendId(), "/1/user", TYPE, username, response.getVersion());
	}

	//
	// Implementation
	//

	boolean exists(String backendId, String username) {
		return Start.get().getElasticClient().exists(backendId, TYPE, username);
	}

	GetResponse get(String backendId, String username, boolean throwNotFound) {
		GetResponse response = Start.get().getElasticClient().get(backendId, TYPE, username);

		if (throwNotFound && !response.isExists())
			throw Exceptions.notFound("no user found for username [%s] in backend [%s]", username, backendId);

		return response;
	}

	//
	// singleton
	//

	private static UserResource singleton = new UserResource();

	static UserResource get() {
		return singleton;
	}

	private UserResource() {
	}
}
