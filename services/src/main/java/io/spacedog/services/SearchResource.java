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

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.services.ElasticHelper.FilteredSearchBuilder;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/v1")
public class SearchResource extends AbstractResource {

	public static final String REFRESH = "refresh";

	//
	// Routes
	//

	@Get("/search")
	@Get("/search/")
	public Payload getSearchAllTypes(Context context) throws JsonParseException, JsonMappingException, IOException,
			NotFoundException, InterruptedException, ExecutionException {
		return postSearchAllTypes(null, context);
	}

	@Post("/search")
	@Post("/search/")
	public Payload postSearchAllTypes(String body, Context context) throws JsonParseException, JsonMappingException,
			IOException, NotFoundException, InterruptedException, ExecutionException {
		Credentials credentials = SpaceContext.checkCredentials();
		refreshIfNecessary(credentials.backendId(), context, false);
		ObjectNode result = searchInternal(credentials, null, body, context);
		return PayloadHelper.json(result);
	}

	@Delete("/search")
	@Delete("/search/")
	public Payload deleteAllTypes(String query, Context context)
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		refreshIfNecessary(credentials.backendId(), context, true);
		DeleteByQueryResponse response = ElasticHelper.get().delete(credentials.backendId(), query, new String[0]);
		return PayloadHelper.json(response.status(), response.getIndex(credentials.backendId()).getFailures());
	}

	@Get("/search/:type")
	@Get("/search/:type/")
	public Payload getSearchForType(String type, Context context) throws JsonParseException, JsonMappingException,
			IOException, NotFoundException, InterruptedException, ExecutionException {
		return postSearchForType(type, null, context);
	}

	@Post("/search/:type")
	@Post("/search/:type/")
	public Payload postSearchForType(String type, String body, Context context) throws JsonParseException,
			JsonMappingException, IOException, NotFoundException, InterruptedException, ExecutionException {
		Credentials credentials = SpaceContext.checkCredentials();
		refreshIfNecessary(credentials.backendId(), context, false);
		ObjectNode result = searchInternal(credentials, type, body, context);
		return PayloadHelper.json(result);
	}

	@Delete("/search/:type")
	@Delete("/search/:type/")
	public Payload deleteSearchForType(String type, String query, Context context)
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		refreshIfNecessary(credentials.backendId(), context, true);
		DeleteByQueryResponse response = ElasticHelper.get().delete(credentials.backendId(), query, type);
		return PayloadHelper.json(response.status(), response.getIndex(credentials.backendId()).getFailures());
	}

	@Post("/filter/:type")
	@Post("/filter/:type")
	public Payload postFilterForType(String type, String body, Context context)
			throws JsonParseException, JsonMappingException, IOException, InterruptedException, ExecutionException {
		Credentials credentials = SpaceContext.checkCredentials();
		refreshIfNecessary(credentials.backendId(), context, false);
		FilteredSearchBuilder builder = ElasticHelper.get().searchBuilder(credentials.backendId(), type)
				.applyContext(context).applyFilters(Json.readObjectNode(body));
		return PayloadHelper.json(extractResults(builder.get(), context, credentials));
	}

	ObjectNode searchInternal(Credentials credentials, String type, String jsonQuery, Context context)
			throws InterruptedException, ExecutionException, NotFoundException, JsonProcessingException, IOException {

		String index = credentials.backendId();
		SearchRequest request = new SearchRequest(index);

		if (!Strings.isNullOrEmpty(type)) {
			// check if type is well defined
			// throws a NotFoundException if not
			ElasticHelper.get().getSchema(index, type);
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
		return extractResults(Start.get().getElasticClient().search(request).get(), context, credentials);
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

			// TODO remove this when hashed passwords have moved to dedicated
			// indices
			object.remove(UserResource.HASHED_PASSWORD);

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

		JsonBuilder<ObjectNode> builder = Json.objectBuilder()//
				.put("took", response.getTookInMillis())//
				.put("total", response.getHits().getTotalHits())//
				.array("results");
		objects.forEach(object -> builder.node(object));
		builder.end();

		if (response.getAggregations() != null)
			builder.node("aggregations", Json.getMapper().valueToTree(response.getAggregations().asMap()));

		return builder.build();
	}

	private Map<String, ObjectNode> getReferences(List<String> references, Credentials credentials) {
		Set<String> set = new HashSet<>(references);
		set.remove(null);
		Map<String, ObjectNode> results = new HashMap<>();
		set.forEach(reference -> {
			Optional<ObjectNode> object = ElasticHelper.get().getObject(//
					credentials.backendId(), getReferenceType(reference), getReferenceId(reference));
			if (object.isPresent()) {

				// TODO remove this when hashed passwords have moved to
				// dedicated indices
				object.get().remove(UserResource.HASHED_PASSWORD);

				results.put(reference, object.get());
			} else
				throw NotFoundException.object(getReferenceType(reference), getReferenceId(reference));
		});
		return results;
	}

	//
	// singleton
	//

	private static SearchResource singleton = new SearchResource();

	static SearchResource get() {
		return singleton;
	}

	private SearchResource() {
	}

}
