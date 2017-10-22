/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.io.IOException;

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
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.google.common.base.Strings;

import io.spacedog.model.Permission;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
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
		String[] types = DataAccessControl.types(Permission.search, credentials);

		DataStore.get().refreshDataTypes(isRefreshRequested(context), types);
		ObjectNode result = searchInternal(body, credentials, context, types);
		return JsonPayload.ok().withObject(result).build();
	}

	@Delete("")
	@Delete("/")
	public Payload deleteAllTypes(String query, Context context) {
		Credentials credentials = SpaceContext.credentials().checkAtLeastAdmin();
		String[] types = DataAccessControl.types(Permission.delete_all, credentials);

		if (Utils.isNullOrEmpty(types))
			return JsonPayload.ok().build();

		DataStore.get().refreshDataTypes(isRefreshRequested(context, true), types);
		DeleteByQueryResponse response = elastic().deleteByQuery(//
				query, DataStore.toDataIndex(types));
		return ElasticPayload.deleted(response).build();
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
		if (DataAccessControl.check(credentials, type, Permission.search)) {

			DataStore.get().refreshDataTypes(isRefreshRequested(context), type);
			ObjectNode result = searchInternal(body, credentials, context, type);
			return JsonPayload.ok().withObject(result).build();
		}
		throw Exceptions.forbidden("forbidden to search [%s] objects", type);
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteSearchForType(String type, String query, Context context) {

		Credentials credentials = SpaceContext.credentials().checkAtLeastAdmin();
		if (DataAccessControl.check(credentials, type, Permission.delete_all)) {

			DataStore.get().refreshDataTypes(isRefreshRequested(context, true), type);
			DeleteByQueryResponse response = elastic()//
					.deleteByQuery(query, DataStore.toDataIndex(type));
			return ElasticPayload.deleted(response).build();
		}
		throw Exceptions.forbidden("forbidden to delete [%s] objects", type);
	}

	//
	// implementation
	//

	ObjectNode searchInternal(String jsonQuery, Credentials credentials, Context context, String... types) {

		if (types.length == 0)
			return Json.object("total", 0, "results", Json.array());

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

		ArrayNode results = Json.array();
		for (SearchHit hit : response.getHits().getHits()) {

			// check if source is null is necessary
			// when the data is not requested
			// fetch-source = false for GET requests
			// or _source = false for POST requests
			JsonNode source = hit.isSourceEmpty() //
					? NullNode.getInstance()
					: Json.readObject(hit.sourceAsString());

			ObjectNode object = Json.object("id", hit.id(), //
					"type", hit.type(), "version", hit.version(), //
					"source", source);

			if (Float.isFinite(hit.score()))
				object.put("score", hit.score());

			if (!Utils.isNullOrEmpty(hit.sortValues())) {
				ArrayNode array = Json.array();
				for (Object value : hit.sortValues()) {
					if (value instanceof Text)
						value = value.toString();
					array.add(Json.toJsonNode(value));
				}
				object.set("sort", array);
			}

			results.add(object);
		}

		ObjectNode payload = Json.object(//
				"total", response.getHits().getTotalHits(), //
				"results", results);

		if (response.getAggregations() != null) {
			try {
				InternalAggregations aggs = (InternalAggregations) response.getAggregations();
				XContentBuilder jsonXBuilder = JsonXContent.contentBuilder();
				jsonXBuilder.startObject();
				aggs.toXContentInternal(jsonXBuilder, ToXContent.EMPTY_PARAMS);
				jsonXBuilder.endObject();
				payload.putRawValue("aggregations", new RawValue(jsonXBuilder.string()));
			} catch (IOException e) {
				throw Exceptions.runtime("failed to convert aggregations into json", e);
			}
		}

		return payload;
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
