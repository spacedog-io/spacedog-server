package com.magiclabs.restapi;

import java.util.Optional;

import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.indices.TypeMissingException;

import com.eclipsesource.json.JsonObject;
import com.magiclabs.restapi.Json.JsonMerger;

@Prefix("/v1/schema")
public class SchemaResource extends AbstractResource {

	private static SchemaResource singleton = new SchemaResource();

	static SchemaResource get() {
		return singleton;
	}

	private SchemaResource() {
	}

	@Get("")
	@Get("/")
	public Payload getAll(Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);
			String elasticIndex = credentials.getAccountId();
			GetMappingsResponse resp = Start.getElasticClient().admin()
					.indices().prepareGetMappings(elasticIndex).get();

			JsonMerger jsonMerger = Json.merger();

			Optional.ofNullable(resp.getMappings())
					.map(indexMap -> indexMap.get(elasticIndex))
					.orElseThrow(() -> new NotFoundException(elasticIndex))
					.forEach(
							typeAndMapping -> {
								try {
									jsonMerger.add(JsonObject
											.readFrom(
													typeAndMapping.value
															.source().string())
											.get(typeAndMapping.key).asObject()
											.get("_meta").asObject());
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
							});

			return new Payload(JSON_CONTENT, jsonMerger.get().toString(),
					HttpStatus.OK);

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload get(String type, Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);
			return new Payload(JSON_CONTENT, getSchema(
					credentials.getAccountId(), type).toString(), HttpStatus.OK);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	public static JsonObject getSchema(String index, String type)
			throws NotFoundException {
		GetMappingsResponse resp = Start.getElasticClient().admin().indices()
				.prepareGetMappings(index).addTypes(type).get();

		String source = Optional.ofNullable(resp.getMappings())
				.map(indexMap -> indexMap.get(index))
				.map(typeMap -> typeMap.get(type))
				.orElseThrow(() -> new NotFoundException(index, type)).source()
				.toString();

		return JsonObject.readFrom(source).get(type).asObject().get("_meta")
				.asObject();
	}

	@Put("/:type")
	@Put("/:type/")
	@Post("/:type")
	@Post("/:type/")
	public Payload upsertSchema(String type, String newSchemaAsString,
			Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);

			JsonObject schema = SchemaValidator.validate(type,
					JsonObject.readFrom(newSchemaAsString));

			String elasticMapping = SchemaTranslator.translate(type, schema)
					.toString();

			PutMappingRequest putMappingRequest = new PutMappingRequest(
					credentials.getAccountId()).type(type).source(
					elasticMapping);

			Start.getElasticClient().admin().indices()
					.putMapping(putMappingRequest).get();

			return created("/v1", "schema", type);

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteSchema(String type, Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);
			Start.getElasticClient().admin().indices()
					.prepareDeleteMapping(credentials.getAccountId())
					.setType(type).get();

		} catch (TypeMissingException exception) {
			// ignored
		} catch (Throwable throwable) {
			return error(throwable);
		}

		return success();
	}

	@SuppressWarnings("serial")
	public static class NotFoundException extends RuntimeException {

		public NotFoundException(String repo, String type) {
			super(String.format("type [%s] not found in repo [%s]", type, repo));
		}

		public NotFoundException(String repo) {
			super(String.format("repo [%s] not found", repo));
		}

	}
}
