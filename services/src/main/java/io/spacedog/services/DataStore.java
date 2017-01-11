/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;
import net.codestory.http.Context;

public class DataStore implements SpaceParams, SpaceFields {

	//
	// help methods
	//

	public boolean isType(String backendId, String type) {
		return Start.get().getElasticClient().existsIndex(backendId, type);
	}

	public ObjectNode getObject(String backendId, String type, String id) {
		GetResponse response = Start.get().getElasticClient().get(backendId, type, id);

		if (!response.isExists())
			throw NotFoundException.object(type, id);

		ObjectNode object = Json.readObject(response.getSourceAsString());

		object.with("meta")//
				.put("id", response.getId())//
				.put("type", response.getType())//
				.put("version", response.getVersion());

		return object;
	}

	IndexResponse createObject(String backendId, String type, ObjectNode object, String createdBy) {
		return createObject(backendId, type, Optional.empty(), object, createdBy);
	}

	IndexResponse createObject(String backendId, String type, String id, ObjectNode object, String createdBy) {
		return createObject(backendId, type, Optional.of(id), object, createdBy);
	}

	IndexResponse createObject(String backendId, String type, Optional<String> id, ObjectNode object,
			String createdBy) {

		object = setMetaBeforeCreate(object, createdBy);
		ElasticClient elasticClient = Start.get().getElasticClient();

		return id.isPresent() //
				? elasticClient.index(backendId, type, id.get(), object.toString())//
				: elasticClient.index(backendId, type, object.toString());
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
	public IndexResponse updateObject(String backendId, String type, String id, long version, ObjectNode object,
			String updatedBy) {

		object.with("meta").remove("id");
		object.with("meta").remove("version");
		object.with("meta").remove("type");

		Json.checkStringNotNullOrEmpty(object, "meta.createdBy");
		Json.checkStringNotNullOrEmpty(object, "meta.createdAt");

		object.with("meta").put("updatedBy", updatedBy);
		object.with("meta").put("updatedAt", DateTime.now().toString());

		IndexRequestBuilder builder = Start.get().getElasticClient().prepareIndex(backendId, type, id)
				.setSource(object.toString());
		if (version > 0)
			builder.setVersion(version);
		return builder.get();
	}

	public IndexResponse updateObject(String backendId, ObjectNode object, String updatedBy) {

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

		return Start.get().getElasticClient().prepareIndex(backendId, type, id).setSource(object.toString())
				.setVersion(version).get();
	}

	public UpdateResponse patchObject(String backendId, String type, String id, ObjectNode object, String updatedBy) {
		return patchObject(backendId, type, id, 0, object, updatedBy);
	}

	public UpdateResponse patchObject(String backendId, String type, String id, long version, ObjectNode object,
			String updatedBy) {

		object.with("meta").removeAll()//
				.put("updatedBy", updatedBy)//
				.put("updatedAt", DateTime.now().toString());

		UpdateRequestBuilder update = Start.get().getElasticClient().prepareUpdate(backendId, type, id)
				.setDoc(object.toString());

		if (version > 0)
			update.setVersion(version);

		return update.get();
	}

	// public DeleteByQueryResponse delete(String index, String query, String...
	// types) {
	//
	// if (Strings.isNullOrEmpty(query))
	// query =
	// Json.objectBuilder().object("query").object("match_all").toString();
	//
	// DeleteByQueryRequest delete = new DeleteByQueryRequest(index)//
	// .timeout(new TimeValue(60000))//
	// .source(query);
	//
	// if (types != null)
	// delete.types(types);
	//
	// try {
	// return
	// Start.get().getElasticClient().execute(DeleteByQueryAction.INSTANCE,
	// delete).get();
	// } catch (ExecutionException | InterruptedException e) {
	// throw Exceptions.wrap(e);
	// }
	// }

	public SearchHits search(String backendId, String type, Object... terms) {

		if (terms.length % 2 == 1)
			throw Exceptions.illegalArgument(//
					"invalid search terms %s: missing term value", Arrays.toString(terms));

		BoolQueryBuilder builder = QueryBuilders.boolQuery();
		for (int i = 0; i < terms.length; i = i + 2)
			builder.filter(QueryBuilders.termQuery(terms[i].toString(), terms[i + 1]));

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(backendId, type).setTypes(type).setQuery(builder).get();

		return response.getHits();
	}

	public FilteredSearchBuilder searchBuilder(String backendId, String type) {
		return new FilteredSearchBuilder(backendId, type);
	}

	public static class FilteredSearchBuilder {

		private SearchRequestBuilder search;
		private BoolQueryBuilder boolBuilder;

		public FilteredSearchBuilder(String backendId, String type) {

			// check if type is well defined
			// throws a NotFoundException if not
			if (!Strings.isNullOrEmpty(type))
				Start.get().getElasticClient().getSchema(backendId, type);

			this.search = Start.get().getElasticClient()//
					.prepareSearch(backendId, type)//
					.setTypes(type);

			this.boolBuilder = QueryBuilders.boolQuery();
		}

		public FilteredSearchBuilder applyContext(Context context) {
			search.setFrom(context.request().query().getInteger(PARAM_FROM, 0))
					.setSize(context.request().query().getInteger(PARAM_SIZE, 10))
					.setFetchSource(context.request().query().getBoolean("fetch-contents", true));

			String queryText = context.get("q");
			if (!Strings.isNullOrEmpty(queryText))
				boolBuilder.must(QueryBuilders.simpleQueryStringQuery(queryText));
			else
				boolBuilder.must(QueryBuilders.matchAllQuery());

			return this;
		}

		public FilteredSearchBuilder applyFilters(JsonNode filters) {
			filters.fields()
					.forEachRemaining(field -> boolBuilder.filter(//
							QueryBuilders.termQuery(field.getKey(), //
									Json.toValue(field.getValue()))));
			return this;
		}

		public SearchResponse get() throws InterruptedException, ExecutionException {
			return search.setQuery(boolBuilder).get();
		}
	}

	public void refreshType(String backendId, String type) {
		refreshType(true, backendId, type);
	}

	public void refreshType(boolean refresh, String backendId, String type) {
		if (refresh)
			Start.get().getElasticClient().refreshType(backendId, type);
	}

	public void refreshBackend(String backendId) {
		refreshBackend(true, backendId);
	}

	public void refreshBackend(boolean refresh, String backendId) {
		if (refresh) {
			Start.get().getElasticClient().refreshBackend(backendId);
		}
	}

	//
	// singleton
	//

	private static DataStore singleton = new DataStore();

	static DataStore get() {
		return singleton;
	}

	private DataStore() {
	}
}
