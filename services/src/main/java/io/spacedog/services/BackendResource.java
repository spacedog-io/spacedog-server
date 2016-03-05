/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Internals;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceParams;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/v1")
public class BackendResource extends AbstractResource {

	private static final String TYPE = "backend";

	//
	// Routes
	//

	@Get("/backend")
	@Get("/backend/")
	public Payload getAll(Context context) {

		SpaceContext.checkSuperDogCredentials();

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

	@Post("/backend/:id")
	@Post("/backend/:id/")
	public Payload post(String backendId, String body, Context context) {

		if (Start.get().getElasticClient().exists(backendId, UserResource.USER_TYPE))
			return Payloads.invalidParameters("backendId", backendId,
					String.format("backend [%s] is not available", backendId));

		ObjectNode user = Json.readObjectNode(body);
		String username = UserResource.checkValidNewUser(user);
		String email = user.get(UserResource.EMAIL).asText();

		// this user is admin of the new backend
		user.putArray(UserResource.GROUPS).add(UserResource.ADMIN_GROUP);

		int shards = context.query().getInteger(SpaceParams.SHARDS, SHARDS_DEFAULT);
		int replicas = context.query().getInteger(SpaceParams.REPLICAS, REPLICAS_DEFAULT);

		Start.get().getElasticClient().createIndex(//
				backendId, UserResource.USER_TYPE, UserResource.getDefaultUserMapping(), shards, replicas);

		// Backend is created, new admin credentials are valid and can be set
		// in space context if none are set
		SpaceContext.setCredentials(//
				Credentials.fromAdmin(backendId, username, email, null));

		UserResource.get().signUp(user.toString(), context);

		if (!isTest(context))
			Internals.get().notify(//
					Start.get().configuration().superdogNotificationTopic(), //
					String.format("New backend (%s)",
							AbstractResource.spaceUrl(Optional.empty(), "/v1", TYPE, backendId).toString()), //
					String.format("backend id = %s\nadmin email = %s", backendId, email));

		return Payloads.saved(true, backendId, "/v1", TYPE, backendId);
	}

	@Delete("/backend/:id")
	@Delete("/backend/:id/")
	public Payload delete(String backendId, Context context) {
		SpaceContext.checkAdminCredentials();

		ElasticClient elastic = Start.get().getElasticClient();
		if (!elastic.exists(backendId, UserResource.USER_TYPE))
			return Payloads.error(HttpStatus.NOT_FOUND, "backend [%s] not found", backendId);

		Start.get().getElasticClient()//
				.deleteAllIndices(backendId);

		if (!isTest(context) || !Start.get().configuration().isOffline()) {
			FileResource.get().deleteAll();
			ShareResource.get().deleteAll();
		}

		return Payloads.success();
	}

	// TODO move to /admin/login when possible
	@Get("/backend/login")
	@Get("/backend/login/")
	public Payload getAdminLogin() {
		SpaceContext.checkAdminCredentials();
		return Payloads.success();
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
