/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.io.IOException;
import java.util.Locale;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.google.common.base.Strings;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.data.CsvRequest;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/1/data")
public class SearchService extends SpaceService {

	//
	// Routes
	//

	@Post("/_search")
	@Post("/_search/")
	public Payload postSearchAllTypes(String body, Context context) {
		Credentials credentials = SpaceContext.credentials();
		String[] types = DataAccessControl.types(credentials, Permission.search);

		DataStore.get().refreshDataTypes(isRefreshRequested(context), types);
		ObjectNode result = searchInternal(body, credentials, context, types);
		return JsonPayload.ok().withContent(result).build();
	}

	@Delete("/_search")
	@Delete("/_search/")
	public Payload deleteSearchAllTypes(String query, Context context) {
		Credentials credentials = SpaceContext.credentials().checkAtLeastAdmin();
		String[] types = DataAccessControl.types(credentials, Permission.delete);

		if (Utils.isNullOrEmpty(types))
			return JsonPayload.ok().build();

		DataStore.get().refreshDataTypes(isRefreshRequested(context, true), types);
		BulkByScrollResponse response = elastic().deleteByQuery(//
				query, DataStore.toDataIndex(types));
		return ElasticPayload.bulk(response).build();
	}

	@Post("/:type/_search")
	@Post("/:type/_search/")
	public Payload postSearchForType(String type, String body, Context context) {
		Credentials credentials = SpaceContext.credentials();

		if (DataAccessControl.roles(type)//
				.containsOne(credentials, Permission.search)) {

			DataStore.get().refreshDataTypes(isRefreshRequested(context), type);
			ObjectNode result = searchInternal(body, credentials, context, type);
			return JsonPayload.ok().withContent(result).build();
		}
		throw Exceptions.forbidden("forbidden to search [%s] objects", type);
	}

	@Post("/:type/_csv")
	@Post("/:type/_csv/")
	public Payload postSearchForTypeToCsv(String type, CsvRequest request, Context context) {

		Credentials credentials = SpaceContext.credentials();
		if (!DataAccessControl.roles(type).containsOne(credentials, Permission.search))
			throw Exceptions.forbidden("forbidden to search [%s] objects", type);

		ElasticClient elastic = elastic();
		DataStore.get().refreshDataTypes(request.refresh, type);
		Locale requestLocale = getRequestLocale(context);
		SearchSourceBuilder builder = SearchSourceBuilder.searchSource()//
				.query(ElasticUtils.toQueryBuilder(request.query.toString()));

		SearchResponse response = elastic.prepareSearch(//
				DataStore.toDataIndex(type))//
				.setTypes(type)//
				.setSource(builder)//
				.setScroll(TimeValue.timeValueSeconds(60))//
				.setSize(request.pageSize)//
				.get();

		return new Payload("text/plain;charset=utf-8;", //
				new CsvStreamingOutput(request, response, requestLocale));
	}

	@Delete("/:type/_search")
	@Delete("/:type/_search/")
	public Payload deleteSearchType(String type, String query, Context context) {
		Credentials credentials = SpaceContext.credentials().checkAtLeastAdmin();

		if (DataAccessControl.roles(type)//
				.containsOne(credentials, Permission.delete)) {

			DataStore.get().refreshDataTypes(isRefreshRequested(context, true), type);
			BulkByScrollResponse response = elastic()//
					.deleteByQuery(query, DataStore.toDataIndex(type));
			return ElasticPayload.bulk(response).build();
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
			search.setSource(ElasticUtils.toSearchSourceBuilder(jsonQuery).version(true));
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
			JsonNode source = hit.hasSource() //
					? Json.readObject(hit.getSourceAsString())//
					: NullNode.getInstance();

			ObjectNode object = Json.object("id", hit.getId(), //
					"type", hit.getType(), "version", hit.getVersion(), //
					"source", source);

			if (Float.isFinite(hit.getScore()))
				object.put("score", hit.getScore());

			if (!Utils.isNullOrEmpty(hit.getSortValues())) {
				ArrayNode array = Json.array();
				for (Object value : hit.getSortValues()) {
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

	private static SearchService singleton = new SearchService();

	public static SearchService get() {
		return singleton;
	}

	private SearchService() {
	}

}
