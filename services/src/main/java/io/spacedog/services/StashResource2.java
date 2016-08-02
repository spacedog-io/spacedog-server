package io.spacedog.services;

import org.elasticsearch.action.index.IndexResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Credentials;
import io.spacedog.utils.Json;
import io.spacedog.utils.Schema;
import io.spacedog.utils.SpaceParams;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/1/stash")
public class StashResource2 {

	//
	// User constants and schema
	//

	public static final String TYPE = "stash";

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getAll(Context context) {
		SpaceContext.checkAdminCredentials();
		return DataResource.get().getByType(TYPE, context);
	}

	@Post("")
	@Post("/")
	public Payload createIndex(Context context) {
		String schema = Schema.builder(TYPE).stash("data").toString();
		return SchemaResource.get().put(TYPE, schema, context);
	}

	@Get("/:id")
	@Get("/:id/")
	public Payload getById(String id, Context context) {
		return DataResource.get().getById(TYPE, id, context);
	}

	@Post("/:id")
	@Post("/:id/")
	public Payload post(String id, String body, Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		ObjectNode object = Json.object("data", Json.readObject(body));

		IndexResponse response = DataStore.get().createObject(//
				credentials.backendId(), TYPE, id, object, credentials.name());

		return JsonPayload.saved(true, credentials.backendId(), "/1", response.getType(), response.getId(),
				response.getVersion());
	}

	@Put("/:id")
	@Put("/:id/")
	public Payload put(String id, String body, Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		ObjectNode object = Json.object("data", Json.readObject(body));
		long version = context.query().getLong(SpaceParams.VERSION, 0l);

		IndexResponse response = DataStore.get().updateObject(credentials.backendId(), //
				TYPE, id, version, object, credentials.name());

		return JsonPayload.saved(false, credentials.backendId(), "/1", response.getType(), //
				response.getId(), response.getVersion());
	}

	@Delete("/:id")
	@Delete("/:id/")
	public Payload delete(String id, Context context) {
		return DataResource.get().deleteById(TYPE, id, context);
	}

	@Get("/:id/data")
	@Get("/:id/data/")
	public Payload getDataById(String id, Context context) {
		Credentials credentials = SpaceContext.checkCredentials();
		ObjectNode object = DataStore.get().getObject(credentials.backendId(), TYPE, id);
		return JsonPayload.json(object.get("data"));
	}

	//
	// singleton
	//

	private static StashResource2 singleton = new StashResource2();

	static StashResource2 get() {
		return singleton;
	}

	private StashResource2() {
	}

}
