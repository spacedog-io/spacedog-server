/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.stream.Stream;

import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import io.spacedog.services.Credentials.Level;
import io.spacedog.services.CredentialsResource.SignUp;
import io.spacedog.utils.BackendKey;
import io.spacedog.utils.Internals;
import io.spacedog.utils.SpaceParams;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class BackendResource extends Resource {

	private static final String TYPE = "backend";

	//
	// Routes
	//

	@Post("/1/backend/:id")
	@Post("/1/backend/:id/")
	public Payload post(String backendId, String body, Context context) {

		BackendKey.checkIfIdIsValid(backendId);

		if (existsBackend(backendId))
			return Payloads.invalidParameters("backendId", backendId,
					String.format("backend id [%s] not available", backendId));

		SignUp backendSignUp = new SignUp(backendId, Level.SUPER_ADMIN, body);
		CredentialsResource.get().create(backendSignUp);

		// after backend is created, new admin credentials are valid
		// and can be set in space context if none are set
		SpaceContext.setCredentials(//
				new Credentials(backendId, backendSignUp.credentials.name(), //
						backendSignUp.credentials.email().get(), Level.SUPER_ADMIN));

		if (!SpaceContext.isTest())
			Internals.get().notify(//
					Start.get().configuration().superdogNotificationTopic(), //
					String.format("New backend (%s)", spaceRootUrl(backendId).toString()), //
					String.format("backend id = %s\nadmin email = %s", backendId,
							backendSignUp.credentials.email().get()));

		return Payloads.saved(true, backendId, "/1/backend", TYPE, backendId, true);
	}

	@Get("/1/backend")
	@Get("/1/backend/")
	public Payload getAll(Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials(false);
		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);

		if (credentials.isRootBackend()) {
			if (credentials.isSuperDog())
				return CredentialsResource.get().getAllSuperAdmins(refresh);

			throw new AuthorizationException("no subdomain found: access <backendId>.spacedog.io");
		}

		return CredentialsResource.get().getAllSuperAdmins(credentials.backendId(), refresh);

	}

	@Delete("/1/backend")
	@Delete("/1/backend/")
	public Payload delete(Context context) {
		Credentials credentials = SpaceContext.checkSuperAdminCredentials();

		CredentialsResource.get().deleteAll(credentials.backendId());
		Start.get().getElasticClient().deleteAllIndices(credentials.backendId());

		if (!SpaceContext.isTest() && !Start.get().configuration().isOffline()) {
			FileResource.get().deleteAll();
			ShareResource.get().deleteAll();
		}

		return Payloads.success();
	}

	@Get("/v1/admin/login")
	@Get("/v1/admin/login/")
	@Get("/1/admin/login")
	@Get("/1/admin/login/")
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

		elastic.refreshType(SPACEDOG_BACKEND, CredentialsResource.TYPE);

		long totalHits = elastic.prepareSearch(SPACEDOG_BACKEND, CredentialsResource.TYPE)//
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
