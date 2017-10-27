/**
 * © David Attias 2015
 */
package io.spacedog.server;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.http.SpaceBackend;
import io.spacedog.jobs.Internals;
import io.spacedog.model.BackendSettings;
import io.spacedog.model.CreateBackendRequest;
import io.spacedog.model.CreateBackendRequest.Type;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Optional7;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class BackendService extends SpaceService {

	private static final String TYPE = "backend";

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload ping() {
		ObjectNode payload = (ObjectNode) Json.toJsonNode(Start.get().info());
		return JsonPayload.ok().withObject(payload).build();
	}

	@Get("/1/backends")
	@Get("/1/backends/")
	public Payload getAll(Context context) {
		SpaceContext.credentials().checkAtLeastSuperAdmin();
		int from = context.query().getInteger(FROM_PARAM, 0);
		int size = context.query().getInteger(SIZE_PARAM, 10);

		SearchResults<Credentials> superAdmins = CredentialsService.get()//
				.getAllSuperAdmins(from, size);
		return toPayload(superAdmins);
	}

	@Delete("/1/backends/:backendId")
	@Delete("/1/backends/:backendId/")
	public Payload delete(String backendId, Context context) {
		SpaceContext.credentials().checkAtLeastSuperAdmin();

		if (SpaceContext.backend().isDefault())
			throw Exceptions.illegalArgument("default backend can not be deleted");

		elastic().deleteBackendIndices();

		if (isDeleteFilesAndShares()) {
			FileService.get().deleteAll();
			ShareService.get().deleteAll();
		}

		return JsonPayload.ok().build();
	}

	@Post("/1/backends")
	@Post("/1/backends/")
	public Payload post(String body, Context context) {
		ServerConfiguration configuration = Start.get().configuration();

		SpaceBackend backend = configuration.apiBackend();
		if (!backend.multi())
			throw Exceptions.illegalArgument(//
					"backend [%s] does not allow sub backends", backend);

		CreateBackendRequest request = Json.toPojo(body, CreateBackendRequest.class);
		if (request.type().equals(Type.dedicated))
			throw Exceptions.illegalArgument("dedicated backend not yet supported");
		SpaceBackend.checkIsValid(request.backendId());

		if (configuration.backendCreateRestricted())
			SpaceContext.credentials().checkSuperDog();

		initBackendIndices(request.backendId(), true);

		CredentialsService credentialsService = CredentialsService.get();
		Credentials credentials = credentialsService.createCredentialsRequestToCredentials(//
				request.superadmin(), Credentials.Type.superadmin);
		credentialsService.create(request.backendId(), credentials);

		if (context.query().getBoolean(NOTIF_PARAM, true)) {
			Optional7<String> topic = configuration.awsSuperdogNotificationTopic();
			if (topic.isPresent())
				Internals.get().notify(topic.get(), //
						String.format("New backend (%s)", spaceRootUrl()), //
						String.format("backend id = %s\nadmin email = %s", //
								request.backendId(), credentials.email().get()));
		}

		return JsonPayload.saved(true, "/1", TYPE, request.backendId()).build();
	}

	//
	// Public interface
	//

	// public Stream<String[]> getAllBackendIndices() {
	// return elastic().indices().map(index -> index.split("-",
	// 2));
	// }

	//
	// Implementation
	//

	public void initBackendIndices(String backendId, boolean throwIfAlreadyExists) {
		Index index = CredentialsService.credentialsIndex().backendId(backendId);

		if (!elastic().exists(index)) {
			CredentialsService.get().initIndex(backendId);
			LogService.get().initIndex(backendId);

		} else if (throwIfAlreadyExists)
			throw Exceptions.alreadyExists(TYPE, backendId);
	}

	private Payload toPayload(SearchResults<Credentials> superAdmins) {
		ArrayNode results = Json.array();

		for (Credentials superAdmin : superAdmins.results)
			results.add(Json.object(USERNAME_FIELD, superAdmin.name(), //
					EMAIL_FIELD, superAdmin.email().get()));

		return JsonPayload.ok()//
				.withFields("total", superAdmins.total)//
				.withResults(results)//
				.build();
	}

	private boolean isDeleteFilesAndShares() {
		ServerConfiguration configuration = Start.get().configuration();
		return !SpaceContext.isTest() //
				&& configuration.awsRegion().isPresent() //
				&& !configuration.isOffline();
	}

	//
	// Singleton
	//

	private static BackendService singleton = new BackendService();

	static BackendService get() {
		return singleton;
	}

	private BackendService() {
		SettingsService.get().registerSettings(BackendSettings.class);
	}
}