/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.indices.TypeMissingException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
	public Payload getAll(Context context) throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = SpaceContext.checkCredentials();
		GetMappingsResponse resp = Start.get().getElasticClient().admin().indices()
				.prepareGetMappings(credentials.backendId()).get();

		JsonMerger jsonMerger = Json.merger();

		Optional.ofNullable(resp.getMappings())//
				.map(indexMap -> indexMap.get(credentials.backendId()))
				.orElseThrow(() -> new NotFoundException(credentials.backendId()))//
				.forEach(typeAndMapping -> {
					try {
						jsonMerger.merge((ObjectNode) Json.readObjectNode(typeAndMapping.value.source().string())
								.get(typeAndMapping.key).get("_meta"));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});

		return Payloads.json(jsonMerger.get());
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload get(String type) throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = SpaceContext.checkCredentials();
		return Payloads.json(ElasticHelper.get().getSchema(credentials.backendId(), type));
	}

	@Put("/:type")
	@Put("/:type/")
	@Post("/:type")
	@Post("/:type/")
	public Payload put(String type, String newSchemaAsString, Context context)
			throws InterruptedException, ExecutionException, JsonParseException, JsonMappingException, IOException {

		Credentials credentials = SpaceContext.checkAdminCredentials();
		JsonNode schema = SchemaValidator.validate(type, Json.readObjectNode(newSchemaAsString));
		String elasticMapping = SchemaTranslator.translate(type, schema).toString();
		PutMappingRequest putMappingRequest = new PutMappingRequest(credentials.backendId()).type(type)
				.source(elasticMapping);
		Start.get().getElasticClient().admin().indices().putMapping(putMappingRequest).get();
		return Payloads.saved(true, "/v1", "schema", type);
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload delete(String type) throws JsonParseException, JsonMappingException, IOException {
		try {
			Credentials credentials = SpaceContext.checkAdminCredentials();
			Start.get().getElasticClient().admin().indices().prepareDeleteMapping(credentials.backendId()).setType(type)
					.get();
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
