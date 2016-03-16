/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.stream.Stream;

import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import io.spacedog.services.Credentials.Level;
import io.spacedog.services.UserResource.UserSignUp;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Internals;
import io.spacedog.utils.SpaceParams;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/1")
public class BackendResource extends Resource {

	private static final String TYPE = "backend";

	//
	// Routes
	//

	@Post("/backend/:id")
	@Post("/backend/:id/")
	public Payload post(String backendId, String body, Context context) {

		if (existsBackend(backendId))
			return Payloads.invalidParameters("backendId", backendId,
					String.format("backend id [%s] not available", backendId));

		UserSignUp signing = UserResource.get().new UserSignUp(backendId, Level.SUPER_ADMIN, body);

		if (signing.existsCredentials())
			throw Exceptions.illegalArgument(//
					"user credentials for backend [%s] with usename [%s] already exists", //
					signing.backendId, signing.username);

		signing.indexCredentials();

		// after backend is created, new admin credentials are valid
		// and can be set in space context if none are set
		SpaceContext.setCredentials(//
				Credentials.fromAdmin(backendId, signing.username, signing.email));

		if (!isTest(context))
			Internals.get().notify(//
					Start.get().configuration().superdogNotificationTopic(), //
					String.format("New backend (%s)", Resource.spaceUrl(backendId, "/1/backend").toString()), //
					String.format("backend id = %s\nadmin email = %s", backendId, signing.email));

		return Payloads.saved(true, backendId, "/1/backend", TYPE, backendId, true);
	}

	@Get("/backend")
	@Get("/backend/")
	public Payload getAll(Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials(false);
		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);

		if (credentials.isRootBackend()) {
			if (credentials.isSuperDogAuthenticated())
				return UserResource.get().getAllSuperAdmins(refresh);

			throw new AuthorizationException("no subdomain found: access <backendId>.spacedog.io");
		}

		return UserResource.get().getAllSuperAdmins(credentials.backendId(), refresh);

	}

	@Delete("/backend")
	@Delete("/backend/")
	public Payload delete(Context context) {
		Credentials credentials = SpaceContext.checkSuperAdminCredentials();

		UserResource.get().deleteAllBackendCredentials(credentials.backendId());
		Start.get().getElasticClient().deleteAllIndices(credentials.backendId());

		if (!isTest(context) && !Start.get().configuration().isOffline()) {
			FileResource.get().deleteAll();
			ShareResource.get().deleteAll();
		}

		return Payloads.success();
	}

	@Get("/admin/login")
	@Get("/admin/login/")
	public Payload getLogin() {
		SpaceContext.checkAdminCredentials();
		return Payloads.success();
	}

	//
	// Implementation
	//

	public boolean existsBackend(String backendId) {

		ElasticClient elastic = Start.get().getElasticClient();

		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery(BACKEND_ID, backendId));

		elastic.refreshType(SPACEDOG_BACKEND, UserResource.CREDENTIALS_TYPE);

		long totalHits = elastic.prepareSearch(SPACEDOG_BACKEND, UserResource.CREDENTIALS_TYPE)//
				.setQuery(new QuerySourceBuilder().setQuery(boolQueryBuilder).toString())//
				.setSize(0)//
				.get()//
				.getHits()//
				.getTotalHits();

		if (totalHits > 0)
			return true;

		return getAllBackendIndices().anyMatch(index -> index[0].equals(backendId));
	}

	public Stream<String[]> getAllBackendIndices() {
		return Start.get().getElasticClient().indices().map(index -> index.split("-", 2));
	}

	//
	// Singleton
	//

	private static BackendResource singleton = new BackendResource();

	static BackendResource get() {
		return singleton;
	}

	private BackendResource() {
	}
}
