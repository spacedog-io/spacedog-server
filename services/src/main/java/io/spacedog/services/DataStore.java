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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.core.Json8;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;
import net.codestory.http.Context;

public class DataStore implements SpaceParams, SpaceFields {

	//
	// help classes and interfaces
	//

	public interface Metable {
		Meta meta();

		void meta(Meta meta);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Meta {
		public String id;
		public String type;
		public long version;
		public String createdBy;
		public DateTime createdAt;
		public String updatedBy;
		public DateTime updatedAt;
	}

	//
	// help methods
	//

	public boolean isType(String type) {
		return Start.get().getElasticClient().existsIndex(type);
	}

	public ObjectNode getObject(String type, String id) {
		GetResponse response = Start.get().getElasticClient().get(type, id);

		if (!response.isExists())
			throw NotFoundException.object(type, id);

		ObjectNode object = Json8.readObject(response.getSourceAsString());

		object.with("meta")//
				.put("id", response.getId())//
				.put("type", response.getType())//
				.put("version", response.getVersion());

		return object;
	}

	IndexResponse createObject(String type, ObjectNode object, String createdBy) {
		return createObject(type, Optional.empty(), object, createdBy);
	}

	IndexResponse createObject(String type, String id, ObjectNode object, String createdBy) {
		return createObject(type, Optional.of(id), object, createdBy);
	}

	IndexResponse createObject(String type, Optional<String> id, ObjectNode object, String createdBy) {

		object = setMetaBeforeCreate(object, createdBy);
		ElasticClient elasticClient = Start.get().getElasticClient();

		return id.isPresent() //
				? elasticClient.index(type, id.get(), object.toString())//
				: elasticClient.index(type, object.toString());
	}

	private ObjectNode setMetaBeforeCreate(ObjectNode object, String createdBy) {
		String now = DateTime.now().toString();

		// replace meta to avoid developers to
		// set any meta fields directly
		object.set("meta", Json8.objectBuilder()//
				.put("createdBy", createdBy)//
				.put("updatedBy", createdBy)//
				.put("createdAt", now)//
				.put("updatedAt", now)//
				.build());

		return object;
	}

	public void createObject(String type, Metable object, Credentials requester) {

		Meta meta = object.meta();
		Check.isTrue(meta == null || meta.id == null, "[meta.id] is not null");

		meta = new Meta();
		meta.createdBy = requester.name();
		meta.updatedBy = requester.name();

		DateTime now = DateTime.now();
		meta.createdAt = now;
		meta.updatedAt = now;

		object.meta(meta);
		ObjectNode node = (ObjectNode) Json8.toNode(object);

		node.with("meta").remove("id");
		node.with("meta").remove("version");
		node.with("meta").remove("type");

		IndexResponse response = Start.get().getElasticClient()//
				.index(type, node.toString());

		meta.type = type;
		meta.id = response.getId();
		meta.version = response.getVersion();
	}

	/**
	 * TODO do we need these two update methods or just one?
	 */
	public IndexResponse updateObject(String type, String id, long version, ObjectNode object, String updatedBy) {

		object.with("meta").remove("id");
		object.with("meta").remove("version");
		object.with("meta").remove("type");

		Json8.checkStringNotNullOrEmpty(object, "meta.createdBy");
		Json8.checkStringNotNullOrEmpty(object, "meta.createdAt");

		object.with("meta").put("updatedBy", updatedBy);
		object.with("meta").put("updatedAt", DateTime.now().toString());

		IndexRequestBuilder builder = Start.get().getElasticClient()//
				.prepareIndex(type, id).setSource(object.toString());

		if (version > 0)
			builder.setVersion(version);
		return builder.get();
	}

	public IndexResponse updateObject(ObjectNode object, String updatedBy) {

		String id = Json8.checkStringNotNullOrEmpty(object, "meta.id");
		String type = Json8.checkStringNotNullOrEmpty(object, "meta.type");
		long version = Json8.checkLongNode(object, "meta.version", true).get().asLong();

		Json8.checkStringNotNullOrEmpty(object, "meta.createdBy");
		Json8.checkStringNotNullOrEmpty(object, "meta.createdAt");

		object.with("meta").remove("id");
		object.with("meta").remove("version");
		object.with("meta").remove("type");

		object.with("meta").put("updatedBy", updatedBy);
		object.with("meta").put("updatedAt", DateTime.now().toString());

		return Start.get().getElasticClient().prepareIndex(type, id)//
				.setSource(object.toString()).setVersion(version).get();
	}

	public void updateObject(Metable object, Credentials requester) {

		Meta meta = object.meta();

		Check.notNull(meta, "meta");
		Check.notNullOrEmpty(meta.id, "meta.id");
		Check.notNullOrEmpty(meta.type, "meta.type");
		Check.notNull(meta.createdBy, "meta.createdBy");
		Check.notNull(meta.createdAt, "meta.createdAt");

		meta.updatedBy = requester.name();
		meta.updatedAt = DateTime.now();

		ObjectNode node = (ObjectNode) Json8.toNode(object);
		node.with("meta").remove("id");
		node.with("meta").remove("version");
		node.with("meta").remove("type");

		meta.version = Start.get().getElasticClient()//
				.prepareIndex(meta.type, meta.id)//
				.setSource(node.toString())//
				.setVersion(meta.version)//
				.get()//
				.getVersion();
	}

	public UpdateResponse patchObject(String type, String id, ObjectNode object, String updatedBy) {
		return patchObject(type, id, 0, object, updatedBy);
	}

	public UpdateResponse patchObject(String type, String id, long version, ObjectNode object, String updatedBy) {

		object.with("meta").removeAll()//
				.put("updatedBy", updatedBy)//
				.put("updatedAt", DateTime.now().toString());

		UpdateRequestBuilder update = Start.get().getElasticClient()//
				.prepareUpdate(type, id).setDoc(object.toString());

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

	public SearchHits search(String type, Object... terms) {

		if (terms.length % 2 == 1)
			throw Exceptions.illegalArgument(//
					"invalid search terms %s: missing term value", Arrays.toString(terms));

		BoolQueryBuilder builder = QueryBuilders.boolQuery();
		for (int i = 0; i < terms.length; i = i + 2)
			builder.filter(QueryBuilders.termQuery(terms[i].toString(), terms[i + 1]));

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(type).setTypes(type).setQuery(builder).get();

		return response.getHits();
	}

	public FilteredSearchBuilder searchBuilder(String type) {
		return new FilteredSearchBuilder(type);
	}

	public static class FilteredSearchBuilder {

		private SearchRequestBuilder search;
		private BoolQueryBuilder boolBuilder;

		public FilteredSearchBuilder(String type) {

			// check if type is well defined
			// throws a NotFoundException if not
			if (!Strings.isNullOrEmpty(type))
				Start.get().getElasticClient().getSchema(type);

			this.search = Start.get().getElasticClient()//
					.prepareSearch(type)//
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
			filters.fields().forEachRemaining(field -> boolBuilder.filter(//
					QueryBuilders.termQuery(field.getKey(), //
							Json8.toValue(field.getValue()))));
			return this;
		}

		public SearchResponse get() throws InterruptedException, ExecutionException {
			return search.setQuery(boolBuilder).get();
		}
	}

	public void refreshType(String type) {
		refreshType(true, type);
	}

	public void refreshType(boolean refresh, String type) {
		if (refresh)
			Start.get().getElasticClient().refreshType(type);
	}

	public void refreshBackend() {
		refreshBackend(true);
	}

	public void refreshBackend(boolean refresh) {
		if (refresh) {
			Start.get().getElasticClient().refreshBackend();
		}
	}

	//
	// singleton
	//

	private static DataStore singleton = new DataStore();

	public static DataStore get() {
		return singleton;
	}

	private DataStore() {
	}
}
