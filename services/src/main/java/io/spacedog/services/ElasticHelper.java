/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryAction;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequest;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.utils.Json;
import net.codestory.http.Context;

public class ElasticHelper {

	//
	// help methods
	//

	public Optional<ObjectNode> getObject(String index, String type, String id) {
		GetResponse response = Start.get().getElasticClient().prepareGet(index, type, id).get();

		if (!response.isExists())
			return Optional.empty();

		ObjectNode object = Json.readObjectNode(response.getSourceAsString());
		object.with("meta").put("id", response.getId()).put("type", response.getType()).put("version",
				response.getVersion());
		return Optional.of(object);
	}

	IndexResponse createObject(String index, String type, ObjectNode object, String createdBy) {
		return createObject(index, type, Optional.empty(), object, createdBy);
	}

	IndexResponse createObject(String index, String type, String id, ObjectNode object, String createdBy) {
		return createObject(index, type, Optional.of(id), object, createdBy);
	}

	IndexResponse createObject(String index, String type, Optional<String> id, ObjectNode object, String createdBy) {

		object = setMetaBeforeCreate(object, createdBy);
		Client elasticClient = Start.get().getElasticClient();

		return (id.isPresent() //
				? elasticClient.prepareIndex(index, type, id.get())//
				: elasticClient.prepareIndex(index, type))//
						.setSource(object.toString()).get();
	}

	private ObjectNode setMetaBeforeCreate(ObjectNode object, String createdBy) {
		String now = DateTime.now().toString();

		// replace meta to avoid developers to
		// set any meta fields directly
		object.set("meta",
				Json.objectBuilder()//
						.put("createdBy", createdBy)//
						.put("updatedBy", createdBy)//
						.put("createdAt", now)//
						.put("updatedAt", now)//
						.build());

		return object;
	}

	/**
	 * TODO do we need these two update methods or just one?
	 */
	public IndexResponse updateObject(String index, String type, String id, long version, ObjectNode object,
			String updatedBy) {

		object.with("meta").remove("id");
		object.with("meta").remove("version");
		object.with("meta").remove("type");

		Json.checkStringNotNullOrEmpty(object, "meta.createdBy");
		Json.checkStringNotNullOrEmpty(object, "meta.createdAt");

		object.with("meta").put("updatedBy", updatedBy);
		object.with("meta").put("updatedAt", DateTime.now().toString());

		IndexRequestBuilder builder = Start.get().getElasticClient().prepareIndex(index, type, id)
				.setSource(object.toString());
		if (version > 0)
			builder.setVersion(version);
		return builder.get();
	}

	public IndexResponse updateObject(String index, ObjectNode object, String updatedBy) {

		String id = Json.checkStringNotNullOrEmpty(object, "meta.id");
		String type = Json.checkStringNotNullOrEmpty(object, "meta.type");
		long version = Json.checkLongNode(object, "meta.version", true).get().asLong();

		Json.checkStringNotNullOrEmpty(object, "meta.createdBy");
		Json.checkStringNotNullOrEmpty(object, "meta.createdAt");

		object.with("meta").remove("id");
		object.with("meta").remove("version");
		object.with("meta").remove("type");

		object.with("meta").put("updatedBy", updatedBy);
		object.with("meta").put("updatedAt", DateTime.now().toString());

		return Start.get().getElasticClient().prepareIndex(index, type, id).setSource(object.toString())
				.setVersion(version).get();
	}

	public UpdateResponse patchObject(String index, String type, String id, ObjectNode object, String updatedBy) {
		return patchObject(index, type, id, 0, object, updatedBy);
	}

	public UpdateResponse patchObject(String index, String type, String id, long version, ObjectNode object,
			String updatedBy) {

		object.with("meta").removeAll()//
				.put("updatedBy", updatedBy)//
				.put("updatedAt", DateTime.now().toString());

		UpdateRequestBuilder update = Start.get().getElasticClient().prepareUpdate(index, type, id)
				.setDoc(object.toString());

		if (version > 0)
			update.setVersion(version);

		return update.get();
	}

	public DeleteByQueryResponse delete(String index, String query, String... types) {

		if (Strings.isNullOrEmpty(query))
			query = Json.objectBuilder().object("query").object("match_all").toString();

		DeleteByQueryRequest delete = new DeleteByQueryRequest(index)//
				.timeout(new TimeValue(60000))//
				.source(query);

		if (types != null)
			delete.types(types);

		try {
			return Start.get().getElasticClient().execute(DeleteByQueryAction.INSTANCE, delete).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	public SearchHits search(String index, String type, String... terms) {

		if (terms.length % 2 == 1)
			throw new RuntimeException(
					String.format("invalid search terms [%s]: missing term value", String.join(", ", terms)));

		BoolQueryBuilder builder = QueryBuilders.boolQuery();
		for (int i = 0; i < terms.length; i = i + 2) {
			builder.filter(QueryBuilders.termQuery(terms[i], terms[i + 1]));
		}

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(index).setTypes(type).setQuery(builder).get();

		return response.getHits();
	}

	public FilteredSearchBuilder searchBuilder(String index, String type) {
		return new FilteredSearchBuilder(index, type);
	}

	public static class FilteredSearchBuilder {

		private SearchRequest searchRequest;
		private SearchSourceBuilder sourceBuilder;
		private BoolQueryBuilder boolBuilder;

		public FilteredSearchBuilder(String index, String type) {

			this.sourceBuilder = SearchSourceBuilder.searchSource();
			this.boolBuilder = QueryBuilders.boolQuery();
			this.searchRequest = new SearchRequest(index);

			if (!Strings.isNullOrEmpty(type)) {
				// check if type is well defined
				// throws a NotFoundException if not
				ElasticHelper.get().getSchema(index, type);
				this.searchRequest.types(type);
			}

		}

		public FilteredSearchBuilder applyContext(Context context) {
			sourceBuilder.from(context.request().query().getInteger("from", 0))
					.size(context.request().query().getInteger("size", 10))
					.fetchSource(context.request().query().getBoolean("fetch-contents", true));

			String queryText = context.get("q");
			if (!Strings.isNullOrEmpty(queryText)) {
				boolBuilder.must(QueryBuilders.simpleQueryStringQuery(queryText));
			} else
				boolBuilder.must(QueryBuilders.matchAllQuery());

			return this;
		}

		public FilteredSearchBuilder applyFilters(JsonNode filters) {
			filters.fields()
					.forEachRemaining(field -> boolBuilder.filter(//
							QueryBuilders.termQuery(field.getKey(), //
									Json.toSimpleValue(field.getValue()))));
			return this;
		}

		public SearchResponse get() throws InterruptedException, ExecutionException {
			searchRequest.source(sourceBuilder.query(boolBuilder));
			return Start.get().getElasticClient().search(searchRequest).get();
		}
	}

	public void refresh(boolean refresh, String... indices) {
		if (refresh) {
			Start.get().getElasticClient().admin().indices().prepareRefresh(indices).get();
		}
	}

	public ObjectNode getSchema(String index, String type) {

		GetMappingsResponse resp = Start.get().getElasticClient().admin().indices()//
				.prepareGetMappings(index)//
				.addTypes(type)//
				.get();

		String source = Optional.ofNullable(resp.getMappings())//
				.map(indexMap -> indexMap.get(index))//
				.map(typeMap -> typeMap.get(type))//
				.orElseThrow(() -> NotFoundException.type(type))//
				.source()//
				.toString();

		return (ObjectNode) Json.readObjectNode(source).get(type).get("_meta");
	}

	//
	// singleton
	//

	private static ElasticHelper singleton = new ElasticHelper();

	static ElasticHelper get() {
		return singleton;
	}

	private ElasticHelper() {
	}

}
