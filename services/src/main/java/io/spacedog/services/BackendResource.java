/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.stream.Stream;

import org.elasticsearch.index.query.QueryBuilders;

import io.spacedog.services.Credentials.Level;
import io.spacedog.utils.AuthenticationException;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Internals;
import io.spacedog.utils.Json;
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

	@Get("")
	@Get("/")
	public Payload ping() {
		return JsonPayload.success();
	}

	@Post("")
	@Post("/")
	@Post("/1")
	@Post("/1/")
	public Payload post(String body, Context context) {
		Credentials credentials = SpaceContext.getCredentials();
		if (credentials.isRootBackend())
			throw Exceptions.illegalArgument("[api] not a valid backend id");
		return post(credentials.backendId(), body, context);
	}

	@Post("/1/backend/:id")
	@Post("/1/backend/:id/")
	public Payload post(String backendId, String body, Context context) {

		Backends.checkIfIdIsValid(backendId);

		if (existsBackend(backendId))
			return JsonPayload.invalidParameters("backendId", backendId,
					String.format("backend id [%s] not available", backendId));

		Credentials credentials = Credentials.signUp(backendId, //
				Level.SUPER_ADMIN, Json.readObject(body));
		CredentialsResource.get().index(credentials);

		// after backend is created, new admin credentials are valid
		// and can be set in space context if none are set
		SpaceContext.setCredentials(credentials);

		if (!SpaceContext.isTest())
			Internals.get().notify(//
					Start.get().configuration().superdogAwsNotificationTopic(), //
					String.format("New backend (%s)", spaceRootUrl(backendId).toString()), //
					String.format("backend id = %s\nadmin email = %s", //
							backendId, credentials.email().get()));

		return JsonPayload.saved(true, backendId, "/1/backend", TYPE, backendId, true);
	}

	@Get("/1/backend")
	@Get("/1/backend/")
	public Payload getAll(Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials(false);
		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);

		if (credentials.isRootBackend()) {
			if (credentials.isSuperDog())
				return CredentialsResource.get().getAllSuperAdmins(refresh);

			throw new AuthenticationException("no backend subdomain found");
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

		return JsonPayload.success();
	}

	//
	// Implementation
	//

	public boolean existsBackend(String backendId) {

		ElasticClient elastic = Start.get().getElasticClient();

		elastic.refreshType(SPACEDOG_BACKEND, CredentialsResource.TYPE);

		long totalHits = elastic.prepareSearch(SPACEDOG_BACKEND, CredentialsResource.TYPE)//
				.setQuery(QueryBuilders.termQuery(BACKEND_ID, backendId))//
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
