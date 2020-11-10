package io.spacedog.services.log;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.spacedog.client.log.LogItem;
import io.spacedog.client.log.LogSearchResults;
import io.spacedog.client.schema.Schema;
import io.spacedog.services.SpaceService;
import io.spacedog.services.elastic.ElasticIndex;
import io.spacedog.services.elastic.ElasticUtils;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Json;

public class LogService extends SpaceService {

	public LogSearchResults get() {
		return get(10);
	}

	public LogSearchResults get(int size) {
		return get(size, false);
	}

	public LogSearchResults get(int size, boolean refresh) {
		return get(null, size, refresh);
	}

	public LogSearchResults get(String q, int size, boolean refresh) {
		return get(q, 0, size, refresh);
	}

	public LogSearchResults get(String q, int from, int size, boolean refresh) {

		elastic().refreshIndex(refresh, index());

		QueryBuilder query = Strings.isNullOrEmpty(q) //
				? QueryBuilders.matchAllQuery() //
				: QueryBuilders.simpleQueryStringQuery(q);

		SearchSourceBuilder source = SearchSourceBuilder.searchSource()//
				.query(query)//
				.sort(RECEIVED_AT_FIELD, SortOrder.DESC)//
				.from(from)//
				.size(size);

		SearchResponse response = elastic().search(source, index());

		return extractLogs(response);
	}

	public LogSearchResults search(SearchSourceBuilder builder) {
		return search(builder, false);
	}

	public LogSearchResults search(SearchSourceBuilder builder, boolean refresh) {
		elastic().refreshIndex(refresh, index());

		SearchResponse response = elastic().search(builder, index());

		return extractLogs(response);
	}

	public ObjectNode delete(DateTime before) {

		RangeQueryBuilder builder = QueryBuilders.rangeQuery(RECEIVED_AT_FIELD)//
				.lt(before.toString());

		BulkByScrollResponse response = elastic().deleteByQuery(builder, index());
		return ElasticUtils.toJson(response);
	}

	//
	// init
	//

	public void initIndex() {
		ElasticIndex index = index();
		if (!elastic().exists(index)) {
			String string = ClassResources.loadAsString(//
					LogService.class, "log-mapping.json");
			Schema schema = Json.toPojo(string, Schema.class);
			elastic().createIndex(index, schema, false);
		}
	}

	//
	// Implementation
	//

	public static final String SERVICE_NAME = "log";

	public ElasticIndex index() {
		return new ElasticIndex(SERVICE_NAME);
	}

	private LogSearchResults extractLogs(SearchResponse response) {

		LogSearchResults results = new LogSearchResults();
		results.total = response.getHits().getTotalHits().value;
		results.results = Lists.newArrayList();

		for (SearchHit hit : response.getHits().getHits())
			results.results.add(Json.toPojo(hit.getSourceAsString(), LogItem.class));

		Aggregations aggregations = response.getAggregations();
		if (aggregations != null)
			results.aggregations = Json.toObjectNode(aggregations.asMap());

		return results;
	}

}
