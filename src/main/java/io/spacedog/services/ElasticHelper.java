package io.spacedog.services;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.concurrent.ExecutionException;

import net.codestory.http.Context;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class ElasticHelper {

	public static SearchHits search(String index, String type, String... terms) {

		if (terms.length % 2 == 1)
			throw new RuntimeException(String.format(
					"invalid search terms [%s]: missing term value",
					terms.toString()));

		AndFilterBuilder builder = new AndFilterBuilder();
		for (int i = 0; i < terms.length; i = i + 2) {
			builder.add(FilterBuilders.termFilter(terms[i], terms[i + 1]));
		}

		SearchResponse response = Start
				.getElasticClient()
				.prepareSearch(index)
				.setTypes(type)
				.setQuery(
						QueryBuilders.filteredQuery(
								QueryBuilders.matchAllQuery(), builder)).get();

		return response.getHits();
	}

	public static FilteredSearchBuilder searchBuilder(String index, String type) {
		return new FilteredSearchBuilder(index, type);
	}

	public static class FilteredSearchBuilder {

		private SearchRequest searchRequest;
		private SearchSourceBuilder sourceBuilder;
		private QueryBuilder queryBuilder;
		private AndFilterBuilder filterBuilder;

		public FilteredSearchBuilder(String index, String type) {
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
			sourceBuilder
					.from(context.request().query().getInteger("from", 0))
					.size(context.request().query().getInteger("size", 10))
					.fetchSource(
							context.request().query()
									.getBoolean("fetch-contents", true));

			String queryText = context.get("q");
			if (!Strings.isNullOrEmpty(queryText)) {
				queryBuilder = QueryBuilders.simpleQueryStringQuery(queryText);
			} else
				queryBuilder = QueryBuilders.matchAllQuery();

			return this;
		}

		public FilteredSearchBuilder applyFilters(JsonObject filters) {
			filterBuilder = new AndFilterBuilder();
			filters.forEach(member -> filterBuilder.add(FilterBuilders
					.termFilter(member.getName(),
							toSimpleValue(member.getValue()))));
			return this;
		}

		public SearchResponse get() throws InterruptedException,
				ExecutionException {
			searchRequest.source(sourceBuilder.query(QueryBuilders
					.filteredQuery(queryBuilder, filterBuilder)));
			return Start.getElasticClient().search(searchRequest).get();
		}
	}

	public static Object toSimpleValue(JsonValue value) {

		if (value.isBoolean())
			return value.asBoolean();

		if (value.isString())
			return value.asString();

		if (value.isNumber())
			try {
				return NumberFormat.getInstance().parse(value.toString());
			} catch (ParseException e) {
				new RuntimeException(e);
			}

		if (value.isNull())
			return null;

		throw new RuntimeException("only supports simple types");
	}
}
