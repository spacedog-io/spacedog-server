/**
 * © David Attias 2015
 */
package io.spacedog.services.data;

import java.util.Map;

import org.elasticsearch.common.Strings;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.schema.Schema;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import io.spacedog.utils.Json;
import io.spacedog.utils.Json.JsonMerger;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/1/schemas")
public class SchemaResty extends SpaceResty {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public ObjectNode getAll(Context context) {
		Map<String, Schema> all = Services.schemas().getAll();
		JsonMerger merger = Json.merger();
		all.values().forEach(schema -> merger.merge(schema.mapping()));
		return merger.get();
	}

	@Delete("")
	@Delete("/")
	public void deleteAll() {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.schemas().deleteAll();
	}

	@Get("/:type")
	@Get("/:type/")
	public ObjectNode get(String type) {
		return Services.schemas().get(type).mapping();
	}

	@Put("/:type")
	@Put("/:type/")
	public Payload put(String type, String newSchemaAsString, Context context) {

		Server.context().credentials().checkAtLeastAdmin();
		Schema.checkName(type);

		if (Strings.isNullOrEmpty(newSchemaAsString))
			Services.schemas().setDefault(type);
		else {
			Schema schema = new Schema(type, Json.readObject(newSchemaAsString));
			Services.schemas().set(schema);
		}

		return JsonPayload.saved(false, "/1", "schemas", type).build();
	}

	@Delete("/:type")
	@Delete("/:type/")
	public void delete(String type) {
		Server.context().credentials().checkAtLeastAdmin();
		Services.schemas().delete(type);
	}

}
