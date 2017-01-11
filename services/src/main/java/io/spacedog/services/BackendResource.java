/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.stream.Stream;

import org.elasticsearch.index.query.QueryBuilders;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Backends;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Internals;
import io.spacedog.utils.Json;
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

	@Get("/1/backend")
	@Get("/1/backend/")
	public Payload getAll(Context context) {
		Credentials credentials = SpaceContext.checkSuperAdminCredentials();

		int from = context.query().getInteger("from", 0);
		int size = context.query().getInteger("size", 10);

		SearchResults<Credentials> superAdmins = credentials.isSuperDog() //
				&& credentials.isTargetingRootApi() //
						? CredentialsResource.get().getAllSuperAdmins(from, size)//
						: CredentialsResource.get().getBackendSuperAdmins(credentials.target(), from, size);

		return toPayload(superAdmins);
	}

	@Delete("/1/backend")
	@Delete("/1/backend/")
	public Payload delete(Context context) {
		Credentials credentials = SpaceContext.checkSuperAdminCredentials();

		if (credentials.isTargetingRootApi())
			throw Exceptions.illegalArgument("backend [api] shall not be deleted");

		CredentialsResource.get().deleteAll(credentials.target());
		Start.get().getElasticClient().deleteAllIndices(credentials.target());

		if (!SpaceContext.isTest() && !Start.get().configuration().isOffline()) {
			FileResource.get().deleteAll();
			ShareResource.get().deleteAll();
		}

		return JsonPayload.success();
	}

	@Post("/1/backend")
	@Post("/1/backend/")
	public Payload post(String body, Context context) {
		return post(SpaceContext.target(), body, context);
	}

	// TODO these routes are deprecated
	@Post("/1/backend/:id")
	@Post("/1/backend/:id/")
	public Payload post(String backendId, String body, Context context) {

		Backends.checkIfIdIsValid(backendId);

		if (existsBackend(backendId))
			return JsonPayload.invalidParameters("backendId", backendId,
					String.format("backend id [%s] not available", backendId));

		ObjectNode data = Json.readObject(body);
		Credentials credentials = CredentialsResource.get()//
				.create(backendId, Level.SUPER_ADMIN, data);

		// after backend is created, new admin credentials are valid
		// and can be set in space context if none are set
		SpaceContext.setCredentials(credentials);

		if (context.query().getBoolean(NOTIF, true))
			Internals.get().notify(//
					Start.get().configuration().superdogAwsNotificationTopic(), //
					String.format("New backend (%s)", spaceRootUrl(backendId).toString()), //
					String.format("backend id = %s\nadmin email = %s", //
							backendId, credentials.email().get()));

		return JsonPayload.saved(true, backendId, "/1/backend", TYPE, backendId, true);
	}

	//
	// Public interface
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
	// Implementation
	//

	private Payload toPayload(SearchResults<Credentials> superAdmins) {
		ArrayNode results = Json.array();

		for (Credentials superAdmin : superAdmins.results)
			results.add(Json.object(BACKEND_ID, superAdmin.backendId(), //
					USERNAME, superAdmin.name(), EMAIL, superAdmin.email().get()));

		return JsonPayload.json(Json.object("total", superAdmins.total, "results", results));
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
