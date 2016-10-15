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

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceParams;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/1/search")
public class SearchResource extends Resource {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getSearchAllTypes(Context context) {
		return postSearchAllTypes(null, context);
	}

	@Post("")
	@Post("/")
	public Payload postSearchAllTypes(String body, Context context) {
		Credentials credentials = SpaceContext.getCredentials();
		String[] types = DataAccessControl.types(DataPermission.search, credentials);
		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);
		DataStore.get().refreshBackend(refresh, credentials.backendId());
		ObjectNode result = searchInternal(body, credentials, context, types);
		return JsonPayload.json(result);
	}

	@Delete("")
	@Delete("/")
	public Payload deleteAllTypes(String query, Context context) {
		// TODO delete special types like user the right way
		// credentials and user data at the same time
		Credentials credentials = SpaceContext.checkAdminCredentials();
		String[] types = DataAccessControl.types(DataPermission.delete_all, credentials);
		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, true);
		DataStore.get().refreshBackend(refresh, credentials.backendId());
		DeleteByQueryResponse response = Start.get().getElasticClient()//
				.deleteByQuery(query, credentials.backendId(), types);
		return JsonPayload.json(response);
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload getSearchForType(String type, Context context) {
		return postSearchForType(type, null, context);
	}

	@Post("/:type")
	@Post("/:type/")
	public Payload postSearchForType(String type, String body, Context context) {
		Credentials credentials = SpaceContext.getCredentials();
		if (DataAccessControl.check(credentials, type, DataPermission.search)) {
			boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);
			DataStore.get().refreshType(refresh, credentials.backendId(), type);
			ObjectNode result = searchInternal(body, credentials, context, type);
			return JsonPayload.json(result);
		}
		throw Exceptions.forbidden("forbidden to search [%s] objects", type);
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteSearchForType(String type, String query, Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		if (DataAccessControl.check(credentials, type, DataPermission.delete_all)) {

			boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, true);
			DataStore.get().refreshType(refresh, credentials.backendId(), type);

			DeleteByQueryResponse response = Start.get().getElasticClient()//
					.deleteByQuery(query, credentials.backendId(), type);

			return JsonPayload.json(response);
		}
		throw Exceptions.forbidden("forbidden to delete [%s] objects", type);
	}

	// @Post("/1/filter/:type")
	// @Post("/1/filter/:type")
	// public Payload postFilterForType(String type, String body, Context
	// context) {
	// try {
	// Credentials credentials = SpaceContext.checkCredentials();
	// boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);
	// DataStore.get().refreshType(refresh, credentials.backendId(), type);
	// FilteredSearchBuilder builder =
	// DataStore.get().searchBuilder(credentials.backendId(), type)
	// .applyContext(context).applyFilters(Json.readObject(body));
	// return JsonPayload.json(extractResults(builder.get(), context,
	// credentials));
	//
	// } catch (InterruptedException | ExecutionException e) {
	// throw new RuntimeException(e);
	// }
	// }

	//
	// implementation
	//

	ObjectNode searchInternal(String jsonQuery, Credentials credentials, Context context, String... types) {

		SearchRequestBuilder search = null;
		ElasticClient elastic = Start.get().getElasticClient();
		String[] aliases = elastic.toAliases(credentials.backendId(), types);

		if (aliases.length == 0)
			return Json.object("took", 0, "total", 0, "results", Json.array());

		search = elastic.prepareSearch().setIndices(aliases).setTypes(types);

		if (Strings.isNullOrEmpty(jsonQuery)) {

			int from = context.query().getInteger("from", 0);
			int size = context.query().getInteger("size", 10);
			Check.isTrue(from + size <= 1000, "from + size is greater than 1000");

			search.setFrom(from)//
					.setSize(size)//
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
			ObjectNode object = Json.readObject(hit.sourceAsString());

			object.with("meta").put("id", hit.id()).put("type", hit.type()).put("version", hit.version());
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

		if (response.getAggregations() != null) {
			// TODO find a safe and efficient solution to add aggregations to
			// payload.
			// Direct json serialization from response.getAggregations().asMap()
			// results in errors because of getters like:
			// InternalTerms$Bucket.getDocCountError(InternalTerms.java:83) that
			// can throw state exceptions.
			// The following solution is safer but inefficient. It fixes issue
			// #1.
			try {
				InternalAggregations aggs = (InternalAggregations) response.getAggregations();
				XContentBuilder jsonXBuilder = JsonXContent.contentBuilder();
				jsonXBuilder.startObject();
				aggs.toXContentInternal(jsonXBuilder, ToXContent.EMPTY_PARAMS);
				jsonXBuilder.endObject();
				// TODO this is so inefficient
				// SearchResponse -> Json String -> JsonNode -> Payload
				builder.node("aggregations", jsonXBuilder.string());
			} catch (IOException e) {
				throw Exceptions.runtime("failed to convert aggregations into json", e);
			}
		}

		return builder.build();
	}

	private Map<String, ObjectNode> getReferences(List<String> references, Credentials credentials) {
		Set<String> set = new HashSet<>(references);
		set.remove(null);
		Map<String, ObjectNode> results = new HashMap<>();
		set.forEach(reference -> {
			ObjectNode object = DataStore.get().getObject(credentials.backendId(), //
					getReferenceType(reference), getReferenceId(reference));
			results.put(reference, object);
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
