/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.joda.time.DateTime;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import io.spacedog.core.Json8;
import io.spacedog.model.Schema;
import io.spacedog.sdk.DataObject;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;
import io.spacedog.utils.Utils;
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
		return elastic().exists(toDataIndex(type));
	}

	public Schema getSchema(String type) {
		return getSchemas(toDataIndex(type)).get(type);
	}

	public Map<String, Schema> getSchemas(Index... indices) {
		Map<String, Schema> schemas = Maps.newHashMap();
		if (Utils.isNullOrEmpty(indices))
			return schemas;

		GetMappingsResponse response = elastic().getMappings(indices);

		for (ObjectCursor<ImmutableOpenMap<String, MappingMetaData>> indexMappings //
		: response.mappings().values()) {

			MappingMetaData mapping = indexMappings.value.iterator().next().value;
			JsonNode node = Json8.readObject(mapping.source().toString())//
					.get(mapping.type()).get("_meta");
			schemas.put(mapping.type(), new Schema(mapping.type(), Json8.checkObject(node)));
		}

		return schemas;
	}

	public Map<String, Schema> getAllSchemas() {
		return getSchemas(allDataIndices());
	}

	public <T extends DataObject<T>> T getObject(//
			String type, String id, Class<T> dataObjectClass) {

		GetResponse response = elastic().get(toDataIndex(type), id, true);
		return Json8.toPojo(response.getSourceAsBytes(), dataObjectClass)//
				.id(response.getId())//
				.type(response.getType())//
				.version(response.getVersion());
	}

	public ObjectNode getObject(String type, String id) {
		GetResponse response = elastic().get(toDataIndex(type), id);

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
		ElasticClient elasticClient = elastic();

		return id.isPresent() //
				? elasticClient.index(toDataIndex(type), id.get(), object.toString())//
				: elasticClient.index(toDataIndex(type), object.toString());
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

	// public void createObject(String type, Metable object, Credentials requester)
	// {
	//
	// Meta meta = object.meta();
	// Check.isTrue(meta == null || meta.id == null, "[meta.id] is not null");
	//
	// meta = new Meta();
	// meta.createdBy = requester.name();
	// meta.updatedBy = requester.name();
	//
	// DateTime now = DateTime.now();
	// meta.createdAt = now;
	// meta.updatedAt = now;
	//
	// object.meta(meta);
	// ObjectNode node = (ObjectNode) Json8.toNode(object);
	//
	// removeNotIndexedMeta(node);
	//
	// IndexResponse response = elastic().index(toDataIndex(type), node.toString());
	//
	// meta.type = type;
	// meta.id = response.getId();
	// meta.version = response.getVersion();
	// }

	/**
	 * TODO do we need these two update methods or just one?
	 */
	public IndexResponse updateObject(String type, String id, long version, ObjectNode object, String updatedBy) {
		ObjectNode meta = object.with("meta");

		Json8.checkStringNotNullOrEmpty(meta, "createdBy");
		Json8.checkStringNotNullOrEmpty(meta, "createdAt");

		meta.remove(Arrays.asList("id", "version", "type", "sort", "score"));

		meta.put("updatedBy", updatedBy);
		meta.put("updatedAt", DateTime.now().toString());

		return elastic().prepareIndex(toDataIndex(type), id)//
				.setSource(object.toString())//
				.setVersion(version > 0 ? version : Versions.MATCH_ANY)//
				.get();
	}

	public IndexResponse updateObject(ObjectNode object, String updatedBy) {

		String id = Json8.checkStringNotNullOrEmpty(object, "meta.id");
		String type = Json8.checkStringNotNullOrEmpty(object, "meta.type");
		long version = Json8.checkLongNode(object, "meta.version", true).get().asLong();

		return updateObject(type, id, version, object, updatedBy);
	}

	public void updateObject(Metable object, Credentials requester) {

		Meta meta = object.meta();

		Check.notNull(meta, "meta");
		Check.notNullOrEmpty(meta.id, "meta.id");
		Check.notNullOrEmpty(meta.type, "meta.type");

		ObjectNode node = (ObjectNode) Json8.toNode(object);

		meta.version = updateObject(meta.type, meta.id, meta.version, node, requester.name())//
				.getVersion();
	}

	public UpdateResponse patchObject(String type, String id, ObjectNode object, String updatedBy) {
		return patchObject(type, id, 0, object, updatedBy);
	}

	public UpdateResponse patchObject(String type, String id, long version, ObjectNode object, String updatedBy) {

		object.with("meta").removeAll()//
				.put("updatedBy", updatedBy)//
				.put("updatedAt", DateTime.now().toString());

		UpdateRequestBuilder update = elastic().prepareUpdate(toDataIndex(type), id).setDoc(object.toString());

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
	// elastic().execute(DeleteByQueryAction.INSTANCE,
	// delete).get();
	// } catch (ExecutionException | InterruptedException e) {
	// throw Exceptions.wrap(e);
	// }
	// }

	public static Index[] allDataIndices() {
		String[] indices = elastic().filteredBackendIndices(Optional.of("data"));
		return Arrays.stream(indices)//
				.map(index -> Index.valueOf(index))//
				.toArray(Index[]::new);
	}

	public static Index toDataIndex(String type) {
		return Index.toIndex(type).service("data");
	}

	public static Index[] toDataIndex(String... types) {
		return Arrays.stream(types)//
				.map(type -> toDataIndex(type))//
				.toArray(Index[]::new);
	}

	public SearchHits search(String type, Object... terms) {

		if (terms.length % 2 == 1)
			throw Exceptions.illegalArgument(//
					"invalid search terms %s: missing term value", Arrays.toString(terms));

		BoolQueryBuilder builder = QueryBuilders.boolQuery();
		for (int i = 0; i < terms.length; i = i + 2)
			builder.filter(QueryBuilders.termQuery(terms[i].toString(), terms[i + 1]));

		SearchResponse response = elastic().prepareSearch(toDataIndex(type))//
				.setTypes(type).setQuery(builder).get();

		return response.getHits();
	}

	public FilteredSearchBuilder searchBuilder(String type) {
		return new FilteredSearchBuilder(type);
	}

	public class FilteredSearchBuilder {

		private SearchRequestBuilder search;
		private BoolQueryBuilder boolBuilder;

		public FilteredSearchBuilder(String type) {

			// check if type is well defined
			// throws a NotFoundException if not
			if (!Strings.isNullOrEmpty(type))
				getSchemas(toDataIndex(type));

			this.search = elastic().prepareSearch(toDataIndex(type)).setTypes(type);
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

	public void refreshDataTypes(String... types) {
		refreshDataTypes(true, types);
	}

	public void refreshDataTypes(boolean refresh, String... types) {
		if (refresh && !Utils.isNullOrEmpty(types))
			elastic().refreshType(toDataIndex(types));
	}

	public void refreshAllDataTypes() {
		refreshAllDataTypes(true);
	}

	public void refreshAllDataTypes(boolean refresh) {
		if (refresh) {
			elastic().refreshBackend();
		}
	}

	//
	// Implementation
	//

	private static ElasticClient elastic() {
		return Start.get().getElasticClient();
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
