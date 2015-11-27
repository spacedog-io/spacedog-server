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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
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

		return SpaceDogServices.getElasticClient().prepareIndex(index, type).setSource(object.toString()).get();
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteAll(String type, Context context) {
		try {
			Account spaceAccount = AdminResource.checkAdminCredentialsOnly(context);

			// check if type is well defined
			// throws a NotFoundException if not
			// TODO useful for security?
			SchemaResource.getSchema(spaceAccount.backendId, type);

			DeleteByQueryResponse response = SpaceDogServices.getElasticClient()
					.prepareDeleteByQuery(spaceAccount.backendId).setQuery(QueryBuilders.matchAllQuery()).get();

			return response.status().getStatus() == 200 ? success()
					: error(response.status().getStatus(), String.format(
							"error deleting all documents of type [%s] in backend [%s]", type, spaceAccount.backendId));

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Get("/:type/:id")
	public Payload get(String type, String objectId, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// throws a NotFoundException if not
			// TODO useful for security?
			SchemaResource.getSchema(credentials.getBackendId(), type);

			Optional<ObjectNode> object = ElasticHelper.get(credentials.getBackendId(), type, objectId);

			return object.isPresent() ? new Payload(JSON_CONTENT, object.get().toString(), HttpStatus.OK)
					: error(HttpStatus.NOT_FOUND);

		} catch (Throwable throwable) {
			return error(throwable);
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
	public Payload update(String type, String id, String body, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// object should be validated before saved
			SchemaResource.getSchema(credentials.getBackendId(), type);

			ObjectNode object = Json.readObjectNode(body);
			boolean strict = context.query().getBoolean("strict", false);
			// TODO return better exception-message in case of invalid version
			// format
			long version = context.query().getLong("version", 0l);

			if (strict) {

				IndexResponse response = fullUpdateInternal(type, id, version, object, credentials);
				return saved(false, "/v1/data", response.getType(), response.getId(), response.getVersion());

			} else {

				UpdateResponse response = partialUpdateInternal(type, id, version, object, credentials);
				return saved(false, "/v1/data", response.getType(), response.getId(), response.getVersion());
			}

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	/**
	 * TODO should i get id and type from the meta for more consistency ?
	 */
	public IndexResponse fullUpdateInternal(String type, String id, long version, ObjectNode object,
			Credentials credentials) {

		object.remove("id");
		object.remove("version");
		object.remove("type");

		checkNotNullOrEmpty(object, "meta.createdBy", type);
		checkNotNullOrEmpty(object, "meta.createdAt", type);

		object.with("meta").put("updatedBy", credentials.getName());
		object.with("meta").put("updatedAt", DateTime.now().toString());

		IndexRequestBuilder index = SpaceDogServices.getElasticClient()
				.prepareIndex(credentials.getBackendId(), type, id).setSource(object.toString()).setVersion(version);

		return index.get();
	}

	public UpdateResponse partialUpdateInternal(String type, String id, long version, ObjectNode object,
			Credentials credentials) {

		object.with("meta").removeAll()//
				.put("updatedBy", credentials.getName())//
				.put("updatedAt", DateTime.now().toString());

		UpdateRequestBuilder update = SpaceDogServices.getElasticClient()
				.prepareUpdate(credentials.getBackendId(), type, id).setDoc(object.toString());

		if (version > 0)
			update.setVersion(version);

		return update.get();
	}

	@Delete("/:type/:id")
	public Payload delete(String type, String objectId, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// throws a NotFoundException if not
			// TODO useful for security?
			SchemaResource.getSchema(credentials.getBackendId(), type);

			DeleteResponse response = SpaceDogServices.getElasticClient()
					.prepareDelete(credentials.getBackendId(), type, objectId).get();
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
		return extractResults(SpaceDogServices.getElasticClient().search(request).get(), context, credentials);
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
		builder.end();

		if (response.getAggregations() != null)
			builder.putNode("aggregations", Json.getMapper().valueToTree(response.getAggregations().asMap()));

		return builder.build();
	}

	private Map<String, ObjectNode> getReferences(List<String> references, Credentials credentials) {
		Set<String> set = new HashSet<>(references);
		set.remove(null);
		Map<String, ObjectNode> results = new HashMap<>();
		set.forEach(reference -> results.put(reference,
				ElasticHelper.get(credentials.getBackendId(), getReferenceType(reference), getReferenceId(reference))
						// TODO if bad reference, I put an empty object
						// do we really want this ?
						.orElse(Json.newObjectNode())));
		return results;
	}

}
