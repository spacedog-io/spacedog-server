/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.services.ElasticHelper.FilteredSearchBuilder;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/v1/data")
public class DataResource extends AbstractResource {

	private static DataResource singleton = new DataResource();

	static DataResource get() {
		return singleton;
	}

	private DataResource() {
	}

	@Get("")
	@Get("/")
	public Payload externalSearch(Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);
			ObjectNode result = internalSearch(credentials, null, null, context);
			return new Payload(JSON_CONTENT, result.toString(), HttpStatus.OK);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload externalGetAll(String type, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);
			ObjectNode result = internalSearch(credentials, type, null, context);
			return new Payload(JSON_CONTENT, result.toString(), HttpStatus.OK);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/:type")
	@Post("/:type/")
	public Payload create(String type, String jsonBody, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// object should be validated before saved
			SchemaResource.getSchema(credentials.getBackendId(), type);

			IndexResponse response = createInternal(credentials.getBackendId(), type, Json.readObjectNode(jsonBody),
					credentials.getName());

			return saved(true, "/v1/data", response.getType(), response.getId(), response.getVersion());

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	IndexResponse createInternal(String index, String type, ObjectNode object, String createdBy) {

		String now = DateTime.now().toString();

		// replace meta to avoid developers to
		// set any meta fields directly
		object.set("meta", Json.startObject().put("createdBy", createdBy).put("updatedBy", createdBy)
				.put("createdAt", now).put("updatedAt", now).build());

		return Start.getElasticClient().prepareIndex(index, type).setSource(object.toString()).get();
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteAll(String type, Context context) {
		/*
		 * TODO (1) this is not exactly right I might want to delete all objects
		 * but keep the schema for future use. (2) Admin credentials?
		 */
		return SchemaResource.get().deleteSchema(type, context);
	}

	@Get("/:type/:id")
	public Payload externalGet(String type, String objectId, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// throws a NotFoundException if not
			// TODO useful for security?
			SchemaResource.getSchema(credentials.getBackendId(), type);

			ObjectNode object = internalGet(type, objectId, credentials);

			return new Payload(JSON_CONTENT, object.toString(), HttpStatus.OK);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	public ObjectNode internalGet(String type, String objectId, Credentials credentials) {

		GetResponse response = Start.getElasticClient().prepareGet(credentials.getBackendId(), type, objectId).get();

		if (!response.isExists())
			throw new NotFoundException(credentials.getBackendId(), type, objectId);

		try {
			ObjectNode object = Json.readObjectNode(response.getSourceAsString());
			object.with("meta").put("id", response.getId()).put("type", response.getType()).put("version",
					response.getVersion());
			return object;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Post("/search")
	@Post("/search/")
	public Payload searchAllTypes(String body, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);
			ObjectNode result = internalSearch(credentials, null, body, context);
			return new Payload(JSON_CONTENT, result.toString(), HttpStatus.OK);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/:type/search")
	@Post("/:type/search/")
	public Payload searchThisType(String type, String body, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);
			ObjectNode result = internalSearch(credentials, type, body, context);
			return new Payload(JSON_CONTENT, result.toString(), HttpStatus.OK);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/:type/filter")
	@Post("/:type/filter/")
	public Payload filter(String type, String jsonBody, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			FilteredSearchBuilder builder = ElasticHelper.searchBuilder(credentials.getBackendId(), type)
					.applyContext(context).applyFilters(Json.readObjectNode(jsonBody));

			return new Payload(JSON_CONTENT, extractResults(builder.get(), context, credentials).toString(),
					HttpStatus.OK);

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Put("/:type/:id")
	public Payload update(String type, String objectId, String jsonBody, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// object should be validated before saved
			SchemaResource.getSchema(credentials.getBackendId(), type);

			ObjectNode object = Json.readObjectNode(jsonBody);
			// removed to forbid developers the update of meta fields
			object.with("meta").removeAll().put("updatedBy", credentials.getName()).put("updatedAt",
					DateTime.now().toString());

			UpdateRequestBuilder update = Start.getElasticClient()
					.prepareUpdate(credentials.getBackendId(), type, objectId).setDoc(object.toString());

			String onVersion = context.get("version");

			if (!Strings.isNullOrEmpty(onVersion))
				try {
					update.setVersion(Long.parseLong(onVersion));
				} catch (NumberFormatException exc) {
					return invalidParameters("version", onVersion, "provided version is not a valid long");
				}

			UpdateResponse response = update.get();

			return saved(response.isCreated(), "/v1/data", response.getType(), response.getId(), response.getVersion());

		} catch (VersionConflictEngineException exc) {
			return error(HttpStatus.CONFLICT, exc);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Delete("/:type/:id")
	public Payload delete(String type, String objectId, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// throws a NotFoundException if not
			// TODO useful for security?
			SchemaResource.getSchema(credentials.getBackendId(), type);

			DeleteResponse response = Start.getElasticClient().prepareDelete(credentials.getBackendId(), type, objectId)
					.get();
			return response.isFound() ? success()
					: error(HttpStatus.NOT_FOUND, "object of type [%s] and id [%s] not found", type, objectId);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	ObjectNode internalSearch(Credentials credentials, String type, String jsonQuery, Context context)
			throws InterruptedException, ExecutionException, NotFoundException, JsonProcessingException, IOException {

		String index = credentials.getBackendId();
		SearchRequest request = new SearchRequest(index);

		if (!Strings.isNullOrEmpty(type)) {
			// check if type is well defined
			// throws a NotFoundException if not
			SchemaResource.getSchema(index, type);
			request.types(type);
		}

		SearchSourceBuilder builder = SearchSourceBuilder.searchSource().version(true);

		if (Strings.isNullOrEmpty(jsonQuery)) {

			builder.from(context.request().query().getInteger("from", 0))
					.size(context.request().query().getInteger("size", 10))
					.fetchSource(context.request().query().getBoolean("fetch-contents", true))
					.query(QueryBuilders.matchAllQuery());

			String queryText = context.get("q");
			if (!Strings.isNullOrEmpty(queryText)) {
				builder.query(QueryBuilders.simpleQueryStringQuery(queryText));
			}

		} else {
			request.source(jsonQuery);
		}

		request.extraSource(builder);
		return extractResults(Start.getElasticClient().search(request).get(), context, credentials);
	}

	private ObjectNode extractResults(SearchResponse response, Context context, Credentials credentials)
			throws JsonProcessingException, IOException {

		String propertyPath = context.request().query().get("fetch-references");
		boolean fetchReferences = !Strings.isNullOrEmpty(propertyPath);
		List<String> references = null;
		if (fetchReferences)
			references = new ArrayList<>();

		List<JsonNode> objects = new ArrayList<>();
		for (SearchHit hit : response.getHits().getHits()) {
			ObjectNode object = Json.readObjectNode(hit.sourceAsString());
			((ObjectNode) object.get("meta")).put("id", hit.id()).put("type", hit.type()).put("version", hit.version());
			objects.add(object);

			if (fetchReferences)
				references.add(Json.get(object, propertyPath).asText());
		}

		if (fetchReferences) {
			Map<String, ObjectNode> referencedObjects = getReferences(references, credentials);

			for (int i = 0; i < objects.size(); i++) {
				String reference = references.get(i);
				if (!Strings.isNullOrEmpty(reference))
					Json.set(objects.get(i), propertyPath, referencedObjects.get(reference));
			}
		}

		JsonBuilder<ObjectNode> builder = Json.startObject()//
				.put("took", response.getTookInMillis())//
				.put("total", response.getHits().getTotalHits())//
				.startArray("results");

		objects.forEach(object -> builder.addNode(object));

		return builder.build();
	}

	private Map<String, ObjectNode> getReferences(List<String> references, Credentials credentials) {
		Set<String> set = new HashSet<>(references);
		set.remove(null);
		Map<String, ObjectNode> results = new HashMap<>();
		set.forEach(reference -> results.put(reference,
				internalGet(getReferenceType(reference), getReferenceId(reference), credentials)));
		return results;
	}

}
