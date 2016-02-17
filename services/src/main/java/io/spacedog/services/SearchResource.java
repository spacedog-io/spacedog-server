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
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

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
	public Payload getSearchAllTypes(Context context) {
		return postSearchAllTypes(null, context);
	}

	@Post("/search")
	@Post("/search/")
	public Payload postSearchAllTypes(String body, Context context) {
		Credentials credentials = SpaceContext.checkCredentials();
		boolean refresh = context.query().getBoolean(SearchResource.REFRESH, false);
		ElasticHelper.get().refresh(refresh, credentials.backendId());
		ObjectNode result = searchInternal(credentials, null, body, context);
		return Payloads.json(result);
	}

	@Delete("/search")
	@Delete("/search/")
	public Payload deleteAllTypes(String query, Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		boolean refresh = context.query().getBoolean(SearchResource.REFRESH, true);
		ElasticHelper.get().refresh(refresh, credentials.backendId());
		DeleteByQueryResponse response = ElasticHelper.get().delete(credentials.backendId(), query);
		return Payloads.json(response);
	}

	@Get("/search/:type")
	@Get("/search/:type/")
	public Payload getSearchForType(String type, Context context) {
		return postSearchForType(type, null, context);
	}

	@Post("/search/:type")
	@Post("/search/:type/")
	public Payload postSearchForType(String type, String body, Context context) {
		Credentials credentials = SpaceContext.checkCredentials();
		boolean refresh = context.query().getBoolean(SearchResource.REFRESH, false);
		ElasticHelper.get().refresh(refresh, credentials.backendId());
		ObjectNode result = searchInternal(credentials, type, body, context);
		return Payloads.json(result);
	}

	@Delete("/search/:type")
	@Delete("/search/:type/")
	public Payload deleteSearchForType(String type, String query, Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials();
		boolean refresh = context.query().getBoolean(SearchResource.REFRESH, true);
		ElasticHelper.get().refresh(refresh, credentials.backendId());

		DeleteByQueryResponse response = ElasticHelper.get().delete(credentials.backendId(), query, type);
		return Payloads.json(response);
	}

	@Post("/filter/:type")
	@Post("/filter/:type")
	public Payload postFilterForType(String type, String body, Context context) {
		try {
			Credentials credentials = SpaceContext.checkCredentials();
			boolean refresh = context.query().getBoolean(SearchResource.REFRESH, false);
			ElasticHelper.get().refresh(refresh, credentials.backendId());
			FilteredSearchBuilder builder = ElasticHelper.get().searchBuilder(credentials.backendId(), type)
					.applyContext(context).applyFilters(Json.readObjectNode(body));
			return Payloads.json(extractResults(builder.get(), context, credentials));

		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	ObjectNode searchInternal(Credentials credentials, String type, String jsonQuery, Context context) {

		try {
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

				builder.from(context.query().getInteger("from", 0))//
						.size(context.query().getInteger("size", 10))//
						.fetchSource(context.query().getBoolean("fetch-contents", true))//
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

		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
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
