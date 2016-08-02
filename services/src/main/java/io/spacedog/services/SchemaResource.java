/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.indices.TypeMissingException;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Json.JsonMerger;
import io.spacedog.utils.Schema;
import io.spacedog.utils.SpaceParams;
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
		Credentials credentials = SpaceContext.checkCredentials();
		ElasticClient elastic = Start.get().getElasticClient();
		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings;
		mappings = elastic.getMappings(credentials.backendId());
		JsonMerger jsonMerger = Json.merger();

		for (ObjectCursor<ImmutableOpenMap<String, MappingMetaData>> indexMappings : mappings.values()) {
			for (ObjectCursor<MappingMetaData> mapping : indexMappings.value.values()) {
				try {
					ObjectNode source = (ObjectNode) Json.readObject(mapping.value.source().string())//
							.get(mapping.value.type());
					if (source.hasNonNull("_meta"))
						jsonMerger.merge((ObjectNode) source.get("_meta"));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		return JsonPayload.json(jsonMerger.get());
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload get(String type) {
		Credentials credentials = SpaceContext.checkCredentials();
		return JsonPayload.json(Start.get()//
				.getElasticClient().getSchema(credentials.backendId(), type)//
				.node());
	}

	@Put("/:type")
	@Put("/:type/")
	@Post("/:type")
	@Post("/:type/")
	public Payload put(String type, String newSchemaAsString, Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials();

		Schema schema = Strings.isNullOrEmpty(newSchemaAsString) ? getDefaultSchema(type) //
				: new Schema(type, Json.readObject(newSchemaAsString));

		if (schema == null)
			throw Exceptions.illegalArgument("no default schema for type [%s]", type);

		schema.validate();
		String mapping = schema.translate().toString();

		String backendId = credentials.backendId();
		ElasticClient elastic = Start.get().getElasticClient();
		boolean indexExists = elastic.existsIndex(backendId, type);

		if (indexExists)
			elastic.putMapping(backendId, type, mapping);
		else {
			int shards = context.query().getInteger(SpaceParams.SHARDS, SpaceParams.SHARDS_DEFAULT);
			int replicas = context.query().getInteger(SpaceParams.REPLICAS, SpaceParams.REPLICAS_DEFAULT);
			boolean async = context.query().getBoolean(SpaceParams.ASYNC, SpaceParams.ASYNC_DEFAULT);
			elastic.createIndex(backendId, type, mapping, async, shards, replicas);
		}

		DataAccessControl.save(backendId, type, schema.acl());

		return JsonPayload.saved(!indexExists, credentials.backendId(), "/1", "schema", type);
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload delete(String type) {
		try {
			Credentials credentials = SpaceContext.checkAdminCredentials();
			Start.get().getElasticClient().deleteIndex(credentials.backendId(), type);
		} catch (TypeMissingException exception) {
			// ignored
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
	}
}
