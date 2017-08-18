/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.spacedog.model.DataPermission;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Utils;
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
		Credentials credentials = SpaceContext.credentials();
		String[] types = DataAccessControl.types(DataPermission.search, credentials);

		DataStore.get().refreshDataTypes(isRefreshRequested(context), types);
		ObjectNode result = searchInternal(body, credentials, context, types);
		return JsonPayload.json(result);
	}

	@Delete("")
	@Delete("/")
	public Payload deleteAllTypes(String query, Context context) {
		Credentials credentials = SpaceContext.credentials().checkAtLeastAdmin();
		String[] types = DataAccessControl.types(DataPermission.delete_all, credentials);

		if (Utils.isNullOrEmpty(types))
			return JsonPayload.success();

		DataStore.get().refreshDataTypes(isRefreshRequested(context, true), types);
		DeleteByQueryResponse response = elastic().deleteByQuery(//
				query, DataStore.toDataIndex(types));
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

		Credentials credentials = SpaceContext.credentials();
		if (DataAccessControl.check(credentials, type, DataPermission.search)) {

			DataStore.get().refreshDataTypes(isRefreshRequested(context), type);
			ObjectNode result = searchInternal(body, credentials, context, type);
			return JsonPayload.json(result);
		}
		throw Exceptions.forbidden("forbidden to search [%s] objects", type);
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteSearchForType(String type, String query, Context context) {

		Credentials credentials = SpaceContext.credentials().checkAtLeastAdmin();
		if (DataAccessControl.check(credentials, type, DataPermission.delete_all)) {

			DataStore.get().refreshDataTypes(isRefreshRequested(context, true), type);
			DeleteByQueryResponse response = elastic()//
					.deleteByQuery(query, DataStore.toDataIndex(type));
			return JsonPayload.json(response);
		}
		throw Exceptions.forbidden("forbidden to delete [%s] objects", type);
	}

	//
	// implementation
	//

	ObjectNode searchInternal(String jsonQuery, Credentials credentials, Context context, String... types) {

		if (types.length == 0)
			return Json.object("took", 0, "total", 0, "results", Json.array());

		SearchRequestBuilder search = elastic().prepareSearch(//
				DataStore.toDataIndex(types)).setTypes(types);

		if (Strings.isNullOrEmpty(jsonQuery)) {

			int from = context.query().getInteger(FROM_PARAM, 0);
			int size = context.query().getInteger(SIZE_PARAM, 10);

			search.setFrom(from)//
					.setSize(size)//
					.setFetchSource(context.query().getBoolean("fetch-contents", true))//
					.setQuery(QueryBuilders.matchAllQuery())//
					.setVersion(true);

			String queryText = context.get("q");
			if (!Strings.isNullOrEmpty(queryText))
				search.setQuery(QueryBuilders.simpleQueryStringQuery(queryText));

		} else {
			search.setExtraSource(//
					SearchSourceBuilder.searchSource().version(true).buildAsBytes());
			search.setSource(jsonQuery);
		}

		return extractResults(search.get(), context, credentials);
	}

	private ObjectNode extractResults(SearchResponse response, Context context, Credentials credentials) {

		List<JsonNode> objects = Lists.newArrayList();
		for (SearchHit hit : response.getHits().getHits()) {

			// check if source is null is necessary
			// when the data is not requested
			// fetch-source = false for GET requests
			// or _source = false for POST requests
			String source = hit.sourceAsString();
			ObjectNode object = source == null ? Json.object() : Json.readObject(source);

			ObjectNode meta = object.with("meta");
			meta.put("id", hit.id()).put("type", hit.type()).put("version", hit.version());

			if (Float.isFinite(hit.score()))
				meta.put("score", hit.score());

			if (!Utils.isNullOrEmpty(hit.sortValues())) {
				ArrayNode array = Json.array();
				for (Object value : hit.sortValues()) {
					if (value instanceof Text)
						value = value.toString();
					array.add(Json.toNode(value));
				}
				meta.set("sort", array);
			}

			objects.add(object);
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
