/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.Map;

import org.elasticsearch.common.Strings;
import org.elasticsearch.indices.TypeMissingException;

import io.spacedog.client.data.DataSettings;
import io.spacedog.client.schema.Schema;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Json.JsonMerger;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/1/schemas")
public class SchemaService extends SpaceService {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getAll(Context context) {
		JsonMerger merger = Json.merger();
		Map<String, Schema> schemas = DataStore.get().getAllSchemas();
		schemas.values().forEach(schema -> merger.merge(schema.mapping()));
		return JsonPayload.ok().withContent(merger.get()).build();
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload get(String type) {
		Schema.checkName(type);
		return JsonPayload.ok()//
				.withContent(DataStore.get().getSchema(type).mapping())//
				.build();
	}

	@Put("/:type")
	@Put("/:type/")
	public Payload put(String type, String newSchemaAsString, Context context) {

		Server.context().credentials().checkAtLeastAdmin();
		Schema.checkName(type);

		Schema schema = Strings.isNullOrEmpty(newSchemaAsString) ? getDefaultSchema(type) //
				: new Schema(type, Json.readObject(newSchemaAsString));

		if (schema == null)
			throw Exceptions.illegalArgument("no default schema for type [%s]", type);

		int shards = context.query().getInteger(SHARDS_PARAM, SHARDS_DEFAULT_PARAM);
		int replicas = context.query().getInteger(REPLICAS_PARAM, REPLICAS_DEFAULT_PARAM);
		boolean async = context.query().getBoolean(ASYNC_PARAM, ASYNC_DEFAULT_PARAM);

		org.elasticsearch.common.settings.Settings settings = //
				org.elasticsearch.common.settings.Settings.builder()//
						.put("number_of_shards", shards)//
						.put("number_of_replicas", replicas)//
						.build();

		boolean created = DataStore.get().setSchema(schema, settings, async);

		return JsonPayload.saved(created, "/1", "schemas", type).build();
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload delete(String type) {
		try {
			Server.context().credentials().checkAtLeastAdmin();
			elastic().deleteIndex(DataStore.toDataIndex(type));
		} catch (TypeMissingException ignored) {
		}
		return JsonPayload.ok().build();
	}

	//
	// Implementation
	//

	private Schema getDefaultSchema(String type) {
		if (PushService.DATA_TYPE.equals(type))
			return PushService.getDefaultInstallationSchema();
		return null;
	}

	//
	// Singleton
	//

	private static SchemaService singleton = new SchemaService();

	public static SchemaService get() {
		return singleton;
	}

	private SchemaService() {
		SettingsService.get().registerSettings(DataSettings.class);
	}
}
