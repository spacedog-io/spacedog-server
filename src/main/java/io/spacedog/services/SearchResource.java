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

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.services.ElasticHelper.FilteredSearchBuilder;
import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/v1/search")
public class SearchResource extends AbstractResource {

	//
	// singleton
	//

	private static SearchResource singleton = new SearchResource();

	static SearchResource get() {
		return singleton;
	}

	private SearchResource() {
	}

	//
	// Routes
	//

	@Post("/search")
	@Post("/search/")
	public Payload searchAllTypes(String body, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);
			ObjectNode result = searchInternal(credentials, null, body, context);
			return new Payload(JSON_CONTENT, result.toString(), HttpStatus.OK);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/search/:type")
	@Post("/search/:type/")
	public Payload searchForType(String type, String body, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);
			ObjectNode result = searchInternal(credentials, type, body, context);
			return new Payload(JSON_CONTENT, result.toString(), HttpStatus.OK);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/filter/:type")
	@Post("/filter/:type")
	public Payload filter(String type, String body, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			FilteredSearchBuilder builder = ElasticHelper.get().searchBuilder(credentials.getBackendId(), type)
					.applyContext(context).applyFilters(Json.readObjectNode(body));

			return new Payload(JSON_CONTENT, extractResults(builder.get(), context, credentials).toString(),
					HttpStatus.OK);

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	ObjectNode searchInternal(Credentials credentials, String type, String jsonQuery, Context context)
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
		set.forEach(
				reference -> results.put(reference,
						ElasticHelper.get()
								.getObject(credentials.getBackendId(), getReferenceType(reference),
										getReferenceId(reference))
								// TODO if bad reference, I put an empty object
								// do we really want this ?
								.orElse(Json.newObjectNode())));
		return results;
	}

}
