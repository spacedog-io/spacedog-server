/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Map;

import org.elasticsearch.common.Strings;
import org.elasticsearch.indices.TypeMissingException;

import io.spacedog.core.Json8;
import io.spacedog.core.Json8.JsonMerger;
import io.spacedog.model.Schema;
import io.spacedog.model.SchemaSettings;
import io.spacedog.utils.Exceptions;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/1/schema")
public class SchemaResource extends Resource {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getAll(Context context) {
		JsonMerger merger = Json8.merger();
		Map<String, Schema> schemas = DataStore.get().getAllSchemas();
		schemas.values().forEach(schema -> merger.merge(schema.node()));
		return JsonPayload.json(merger.get());
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload get(String type) {
		Schema.checkName(type);
		return JsonPayload.json(//
				DataStore.get().getSchema(type).node());
	}

	@Put("/:type")
	@Put("/:type/")
	// deprecated
	@Post("/:type")
	@Post("/:type/")
	public Payload put(String type, String newSchemaAsString, Context context) {

		SpaceContext.credentials().checkAtLeastAdmin();
		Schema.checkName(type);

		Schema schema = Strings.isNullOrEmpty(newSchemaAsString) ? getDefaultSchema(type) //
				: new Schema(type, Json8.readObject(newSchemaAsString));

		if (schema == null)
			throw Exceptions.illegalArgument("no default schema for type [%s]", type);

		String mapping = schema.validate().translate().toString();

		Index index = DataStore.toDataIndex(type);
		boolean indexExists = elastic().exists(index);

		if (indexExists)
			elastic().putMapping(index, mapping);
		else {
			int shards = context.query().getInteger(SHARDS_PARAM, SHARDS_DEFAULT_PARAM);
			int replicas = context.query().getInteger(REPLICAS_PARAM, REPLICAS_DEFAULT_PARAM);
			boolean async = context.query().getBoolean(ASYNC_PARAM, ASYNC_DEFAULT_PARAM);
			elastic().createIndex(index, mapping, async, shards, replicas);
		}

		DataAccessControl.save(type, schema.acl());

		return JsonPayload.saved(!indexExists, "/1", "schema", type);
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload delete(String type) {
		try {
			SpaceContext.credentials().checkAtLeastAdmin();
			elastic().deleteIndex(DataStore.toDataIndex(type));
			DataAccessControl.delete(type);
		} catch (TypeMissingException ignored) {
		}
		return JsonPayload.success();
	}

	//
	// Implementation
	//

	private Schema getDefaultSchema(String type) {
		if (PushResource.TYPE.equals(type))
			return PushResource.getDefaultInstallationSchema();
		return null;
	}

	//
	// Singleton
	//

	private static SchemaResource singleton = new SchemaResource();

	static SchemaResource get() {
		return singleton;
	}

	private SchemaResource() {
		SettingsResource.get().registerSettings(SchemaSettings.class);
	}
}
