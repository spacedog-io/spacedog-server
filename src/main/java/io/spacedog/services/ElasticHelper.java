/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.codestory.http.Context;

public class ElasticHelper {

	public static Optional<ObjectNode> get(String index, String type, String id) {
		GetResponse response = SpaceDogServices.getElasticClient().prepareGet(index, type, id).get();

		if (!response.isExists())
			return Optional.empty();

		try {
			ObjectNode object = Json.readObjectNode(response.getSourceAsString());
			object.with("meta").put("id", response.getId()).put("type", response.getType()).put("version",
					response.getVersion());
			return Optional.of(object);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static SearchHits search(String index, String type, String... terms) {

		if (terms.length % 2 == 1)
			throw new RuntimeException(
					String.format("invalid search terms [%s]: missing term value", terms.toString()));

		AndFilterBuilder builder = new AndFilterBuilder();
		for (int i = 0; i < terms.length; i = i + 2) {
			builder.add(FilterBuilders.termFilter(terms[i], terms[i + 1]));
		}

		SearchResponse response = SpaceDogServices.getElasticClient().prepareSearch(index).setTypes(type)
				.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), builder)).get();

		return response.getHits();
	}

	public static FilteredSearchBuilder searchBuilder(String index, String type)
			throws NotFoundException, JsonProcessingException, IOException {

		return new FilteredSearchBuilder(index, type);
	}

	public static class FilteredSearchBuilder {

		private SearchRequest searchRequest;
		private SearchSourceBuilder sourceBuilder;
		private QueryBuilder queryBuilder;
		private AndFilterBuilder filterBuilder;

		public FilteredSearchBuilder(String index, String type)
				throws NotFoundException, JsonProcessingException, IOException {

			this.sourceBuilder = SearchSourceBuilder.searchSource();

			this.searchRequest = new SearchRequest(index);

			if (!Strings.isNullOrEmpty(type)) {
				// check if type is well defined
				// throws a NotFoundException if not
				SchemaResource.getSchema(index, type);
				this.searchRequest.types(type);
			}

		}

		public FilteredSearchBuilder applyContext(Context context) {
			sourceBuilder.from(context.request().query().getInteger("from", 0))
					.size(context.request().query().getInteger("size", 10))
					.fetchSource(context.request().query().getBoolean("fetch-contents", true));

			String queryText = context.get("q");
			if (!Strings.isNullOrEmpty(queryText)) {
				queryBuilder = QueryBuilders.simpleQueryStringQuery(queryText);
			} else
				queryBuilder = QueryBuilders.matchAllQuery();

			return this;
		}

		public FilteredSearchBuilder applyFilters(JsonNode filters) {
			filterBuilder = new AndFilterBuilder();
			filters.fields().forEachRemaining(field -> filterBuilder
					.add(FilterBuilders.termFilter(field.getKey(), Json.toSimpleValue(field.getValue()))));
			return this;
		}

		public SearchResponse get() throws InterruptedException, ExecutionException {
			searchRequest.source(sourceBuilder.query(QueryBuilders.filteredQuery(queryBuilder, filterBuilder)));
			return SpaceDogServices.getElasticClient().search(searchRequest).get();
		}
	}
}
