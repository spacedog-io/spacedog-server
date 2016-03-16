/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.services.DataStore.FilteredSearchBuilder;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.SpaceParams;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

//@Prefix("/1")
public class SearchResource extends Resource {

	//
	// Routes
	//

	@Get("/v1/search")
	@Get("/v1/search/")
	@Get("/1/search")
	@Get("/1/search/")
	public Payload getSearchAllTypes(Context context) {
		return postSearchAllTypes(null, context);
	}

	@Post("/v1/search")
	@Post("/v1/search/")
	@Post("/1/search")
	@Post("/1/search/")
	public Payload postSearchAllTypes(String body, Context context) {
		Credentials credentials = SpaceContext.checkCredentials();
		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);
		DataStore.get().refreshBackend(refresh, credentials.backendId());
		ObjectNode result = searchInternal(credentials, null, body, context);
		return Payloads.json(result);
	}

	@Delete("/v1/search")
	@Delete("/v1/search/")
	@Delete("/1/search")
	@Delete("/1/search/")
	public Payload deleteAllTypes(String query, Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, true);
		DataStore.get().refreshBackend(refresh, credentials.backendId());
		DeleteByQueryResponse response = Start.get().getElasticClient()//
				.deleteByQuery(credentials.backendId(), query);
		return Payloads.json(response);
	}

	@Get("/v1/search/:type")
	@Get("/v1/search/:type/")
	@Get("/1/search/:type")
	@Get("/1/search/:type/")
	public Payload getSearchForType(String type, Context context) {
		return postSearchForType(type, null, context);
	}

	@Post("/v1/search/:type")
	@Post("/v1/search/:type/")
	@Post("/1/search/:type")
	@Post("/1/search/:type/")
	public Payload postSearchForType(String type, String body, Context context) {
		Credentials credentials = SpaceContext.checkCredentials();
		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);
		DataStore.get().refreshType(refresh, credentials.backendId(), type);
		ObjectNode result = searchInternal(credentials, type, body, context);
		return Payloads.json(result);
	}

	@Delete("/v1/search/:type")
	@Delete("/v1/search/:type/")
	@Delete("/1/search/:type")
	@Delete("/1/search/:type/")
	public Payload deleteSearchForType(String type, String query, Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials();
		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, true);
		DataStore.get().refreshType(refresh, credentials.backendId(), type);

		DeleteByQueryResponse response = Start.get().getElasticClient()//
				.deleteByQuery(credentials.backendId(), type, query);

		return Payloads.json(response);
	}

	@Post("/1/filter/:type")
	@Post("/1/filter/:type")
	public Payload postFilterForType(String type, String body, Context context) {
		try {
			Credentials credentials = SpaceContext.checkCredentials();
			boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);
			DataStore.get().refreshType(refresh, credentials.backendId(), type);
			FilteredSearchBuilder builder = DataStore.get().searchBuilder(credentials.backendId(), type)
					.applyContext(context).applyFilters(Json.readObjectNode(body));
			return Payloads.json(extractResults(builder.get(), context, credentials));

		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	//
	// implementation
	//

	ObjectNode searchInternal(Credentials credentials, String type, String jsonQuery, Context context) {

		SearchRequestBuilder search = null;
		ElasticClient elastic = Start.get().getElasticClient();

		if (Strings.isNullOrEmpty(type)) {
			String[] indices = elastic.toIndices(credentials.backendId(), false);
			if (indices.length == 0)
				return Json.objectBuilder().put("took", 0).put("total", 0).array("results").build();
			else
				search = elastic.prepareSearch().setIndices(indices);
		} else
			search = elastic.prepareSearch(credentials.backendId(), type).setTypes(type);

		if (Strings.isNullOrEmpty(jsonQuery)) {

			search.setFrom(context.query().getInteger("from", 0))//
					.setSize(context.query().getInteger("size", 10))//
					.setFetchSource(context.query().getBoolean("fetch-contents", true))//
					.setQuery(QueryBuilders.matchAllQuery())//
					.setVersion(true);

			String queryText = context.get("q");
			if (!Strings.isNullOrEmpty(queryText))
				search.setQuery(QueryBuilders.simpleQueryStringQuery(queryText));

		} else {
			search.setExtraSource(SearchSourceBuilder.searchSource().version(true).buildAsBytes());
			search.setSource(jsonQuery);
		}

		return extractResults(search.get(), context, credentials);
	}

	private ObjectNode extractResults(SearchResponse response, Context context, Credentials credentials) {

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
			Optional<ObjectNode> object = DataStore.get().getObject(//
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
