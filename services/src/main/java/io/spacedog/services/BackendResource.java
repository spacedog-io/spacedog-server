/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.services.Credentials.Level;
import io.spacedog.services.UserResource.UserSignUp;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Internals;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceParams;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/v1")
public class BackendResource extends AbstractResource {

	private static final String TYPE = "backend";
	public static final String API = "api";

	//
	// Routes
	//
	@Post("/backend/:id")
	@Post("/backend/:id/")
	public Payload post(String backendId, String body, Context context) {

		if (Start.get().getElasticClient().existsIndex(backendId, UserResource.USER_TYPE))
			return Payloads.invalidParameters("backendId", backendId,
					String.format("backend id [%s] not available", backendId));

		UserSignUp signing = UserResource.get().new UserSignUp(backendId, Level.SUPER_ADMIN, body);

		if (signing.existsCredentials())
			throw Exceptions.illegalArgument(//
					"user credentials for backend [%s] with usename [%s] already exists", //
					signing.backendId, signing.username);

		int shards = context.query().getInteger(SpaceParams.SHARDS, SHARDS_DEFAULT);
		int replicas = context.query().getInteger(SpaceParams.REPLICAS, REPLICAS_DEFAULT);

		Start.get().getElasticClient().createIndex(//
				backendId, UserResource.USER_TYPE, UserResource.getDefaultUserMapping(), shards, replicas);

		signing.indexCredentials();
		signing.indexUser();

		// Backend is created, new admin credentials are valid and can be set
		// in space context if none are set
		SpaceContext.setCredentials(//
				Credentials.fromAdmin(backendId, signing.username, signing.email));

		if (!isTest(context))
			Internals.get().notify(//
					Start.get().configuration().superdogNotificationTopic(), //
					String.format("New backend (%s)",
							AbstractResource.spaceUrl(Optional.empty(), "/v1", TYPE, backendId).toString()), //
					String.format("backend id = %s\nadmin email = %s", backendId, signing.email));

		return Payloads.saved(true, backendId, "/v1", TYPE, backendId);
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

	// TODO not use yet but who knows
	public Payload getAllBackendsFromIndices() {

		Set<String> indices = Start.get().getElasticClient().indices()//
				.map(index -> index.split("-", 2)[0])//
				.collect(Collectors.toSet());

		JsonBuilder<ObjectNode> builder = Json.objectBuilder()//
				.put("total", indices.size())//
				.array("results");

		for (String indice : indices)
			builder.add(indice);

		return Payloads.json(builder);
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
