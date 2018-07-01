package io.spacedog.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.settings.SettingsAclSettings;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import io.spacedog.utils.Exceptions;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/1/settings")
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
		elastic().deleteIndex(Services.settings().index());
	}

	@Get("/:id")
	@Get("/:id/")
	public ObjectNode get(String id) {
		checkAuthorizedTo(id, Permission.read);
		return Services.settings().getOrThrow(id);
	}

	@Put("/:id")
	@Put("/:id/")
	public Payload put(String id, ObjectNode settings) {
		checkNotInternalSettings(id);
		checkAuthorizedTo(id, Permission.update);
		long version = Services.settings().save(id, settings);
		return JsonPayload.ok().withFields("id", id, //
				"type", "settings", "version", version)//
				.build();
	}

	@Delete("/:id")
	@Delete("/:id/")
	public void delete(String id) {
		checkNotInternalSettings(id);
		checkAuthorizedTo(id, Permission.update);
		Services.settings().delete(id);
	}

	@Get("/:id/:field")
	@Get("/:id/:field/")
	public JsonNode get(String id, String field) {
		checkAuthorizedTo(id, Permission.read);
		return Services.settings().get(id, field)//
				.orElse(NullNode.getInstance());
	}

	@Put("/:id/:field")
	@Put("/:id/:field/")
	public Payload put(String id, String field, JsonNode value) {
		checkNotInternalSettings(id);
		checkAuthorizedTo(id, Permission.update);
		long version = Services.settings().save(id, field, value);
		return JsonPayload.ok().withFields("id", id, //
				"type", "settings", "version", version)//
				.build();
	}

	@Delete("/:id/:field")
	@Delete("/:id/:field/")
	public Payload delete(String id, String field) {
		checkNotInternalSettings(id);
		checkAuthorizedTo(id, Permission.update);
		long version = Services.settings().delete(id, field);
		return JsonPayload.ok().withFields("id", id, //
				"type", "settings", "version", version)//
				.build();
	}

	//
	// implementation
	//

	private Credentials checkAuthorizedTo(String settingsId, Permission permission) {
		Credentials credentials = Server.context().credentials();
		Services.settings()//
				.getOrThrow(SettingsAclSettings.class)//
				.get(settingsId)//
				.check(credentials, permission);
		return credentials;
	}

	private void checkNotInternalSettings(String settingsId) {
		if (settingsId.toLowerCase().startsWith("internal"))
			throw Exceptions.forbidden("direct update of internal settings is forbidden");
	}

}
