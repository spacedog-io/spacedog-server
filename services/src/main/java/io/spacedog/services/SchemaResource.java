/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.indices.TypeMissingException;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Json;
import io.spacedog.utils.Json.JsonMerger;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/v1/schema")
public class SchemaResource extends AbstractResource {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getAll(Context context) {
		Credentials credentials = SpaceContext.checkCredentials();
		ElasticClient elastic = Start.get().getElasticClient();
		GetMappingsResponse resp = elastic.getMappings(credentials.backendId());
		JsonMerger jsonMerger = Json.merger();

		for (ObjectCursor<ImmutableOpenMap<String, MappingMetaData>> indexMappings : resp.getMappings().values()) {
			for (ObjectCursor<MappingMetaData> mapping : indexMappings.value.values()) {
				try {
					ObjectNode source = Json.readObjectNode(mapping.value.source().string());
					jsonMerger.merge((ObjectNode) source.get(mapping.value.type()).get("_meta"));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		return Payloads.json(jsonMerger.get());
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload get(String type) {
		Credentials credentials = SpaceContext.checkCredentials();
		return Payloads.json(//
				Start.get().getElasticClient().getSchema(credentials.backendId(), type));
	}

	@Put("/:type")
	@Put("/:type/")
	// TODO deprecate POST since we pass the type as parameter ?
	@Post("/:type")
	@Post("/:type/")
	public Payload put(String type, String newSchemaAsString, Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials();

		JsonNode schema = SchemaValidator.validate(type, Json.readObjectNode(newSchemaAsString));
		String mapping = SchemaTranslator.translate(type, schema).toString();

		String backendId = credentials.backendId();
		ElasticClient elastic = Start.get().getElasticClient();
		boolean indexExists = elastic.exists(backendId, type);

		if (!indexExists)
			elastic.createIndex(backendId, type, mapping);
		else
			elastic.putMapping(backendId, type, mapping);

		return Payloads.saved(!indexExists, "/v1", "schema", type);
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
		return Payloads.success();
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
