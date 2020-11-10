package io.spacedog.services.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.settings.SettingsAclSettings;
import io.spacedog.server.Server;
import io.spacedog.services.JsonPayload;
import io.spacedog.services.Services;
import io.spacedog.services.SpaceResty;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/2/settings")
public class SettingsResty extends SpaceResty {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public ObjectNode getAll(Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();

		int from = context.query().getInteger(FROM_PARAM, 0);
		int size = context.query().getInteger(SIZE_PARAM, 10);

		return Services.settings().getAll(from, size, isRefreshRequested(context));
	}

	@Delete("")
	@Delete("/")
	public void deleteIndex() {
		Server.context().credentials().checkAtLeastSuperAdmin();
		elastic().deleteIndices(Services.settings().index());
	}

	@Get("/:id")
	@Get("/:id/")
	public ObjectNode get(String id) {
		checkAuthorized(id, Permission.read);
		return Services.settings().getOrThrow(id);
	}

	@Put("/:id")
	@Put("/:id/")
	public Payload put(String id, ObjectNode settings) {
		checkAuthorized(id, Permission.update);
		long version = Services.settings().save(id, settings);
		return JsonPayload.ok().withFields("id", id, //
				"type", "settings", "version", version)//
				.build();
	}

	@Delete("/:id")
	@Delete("/:id/")
	public void delete(String id) {
		checkAuthorized(id, Permission.update);
		Services.settings().delete(id);
	}

	@Get("/:id/:field")
	@Get("/:id/:field/")
	public JsonNode get(String id, String field) {
		checkAuthorized(id, Permission.read);
		return Services.settings().get(id, field)//
				.orElse(NullNode.getInstance());
	}

	@Put("/:id/:field")
	@Put("/:id/:field/")
	public Payload put(String id, String field, JsonNode value) {
		checkAuthorized(id, Permission.update);
		long version = Services.settings().save(id, field, value);
		return JsonPayload.ok().withFields("id", id, //
				"type", "settings", "version", version)//
				.build();
	}

	@Delete("/:id/:field")
	@Delete("/:id/:field/")
	public Payload delete(String id, String field) {
		checkAuthorized(id, Permission.update);
		long version = Services.settings().delete(id, field);
		return JsonPayload.ok().withFields("id", id, //
				"type", "settings", "version", version)//
				.build();
	}

	//
	// implementation
	//

	private Credentials checkAuthorized(String settingsId, Permission permission) {
		Credentials credentials = Server.context().credentials();

		if (settingsId.toLowerCase().startsWith("internal"))
			credentials.checkSuperDog();
		else {
			Services.settings()//
					.getOrThrow(SettingsAclSettings.class)//
					.get(settingsId)//
					.checkPermission(credentials, permission);
		}

		return credentials;
	}
}
