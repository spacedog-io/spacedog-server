/**
 * © David Attias 2015
 */
package io.spacedog.services.data;

import java.util.Map;

import org.elasticsearch.common.Strings;

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
	public Payload getAll(Context context) {
		Map<String, Schema> all = Services.schemas().getAll();
		JsonMerger merger = Json.merger();
		all.values().forEach(schema -> merger.merge(schema.mapping()));
		return JsonPayload.ok().withContent(merger.get()).build();
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload get(String type) {
		Schema schema = Services.schemas().get(type);
		return JsonPayload.ok().withContent(schema.mapping()).build();
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
	public Payload delete(String type) {
		Server.context().credentials().checkAtLeastAdmin();
		Services.schemas().delete(type);
		return JsonPayload.ok().build();
	}

}
