/**
 * © David Attias 2015
 */
package io.spacedog.services.data;

import java.util.Map;

import org.elasticsearch.common.Strings;

import io.spacedog.client.schema.Schema;
import io.spacedog.services.JsonPayload;
import io.spacedog.services.Server;
import io.spacedog.services.Services;
import io.spacedog.services.SpaceResty;
import io.spacedog.utils.Json;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/2/schemas")
public class SchemaResty extends SpaceResty {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Map<String, Schema> getAll(Context context) {
		return Services.schemas().getAll();
	}

	@Delete("")
	@Delete("/")
	public void deleteAll() {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.schemas().deleteAll();
	}

	@Get("/:type")
	@Get("/:type/")
	public Schema get(String type) {
		return Services.schemas().get(type);
	}

	@Put("/:type")
	@Put("/:type/")
	public Payload put(String type, String newSchemaAsString, Context context) {

		Server.context().credentials().checkAtLeastAdmin();
		Schema.checkName(type);

		if (Strings.isNullOrEmpty(newSchemaAsString))
			Services.schemas().setDefault(type);
		else {
			Schema schema = Json.toPojo(newSchemaAsString, Schema.class).name(type);
			Services.schemas().set(schema);
		}

		return JsonPayload.saved(false, "/2", "schemas", type).build();
	}

	@Delete("/:type")
	@Delete("/:type/")
	public void delete(String type) {
		Server.context().credentials().checkAtLeastAdmin();
		Services.schemas().delete(type);
	}

}
