/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import io.spacedog.client.data.DataObject;
import io.spacedog.client.data.DataObjectBase;
import io.spacedog.client.data.DataObjectWrap;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.schema.Schema;
import io.spacedog.utils.Json;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;

public class DataStore implements SpaceParams, SpaceFields {

	//
	// Schema help methods
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
			schemas.put(mapping.type(), //
					new Schema(mapping.type(), //
							Json.readObject(mapping.source().toString())));
		}

		return schemas;
	}

	public Map<String, Schema> getAllSchemas() {
		return getSchemas(allDataIndices());
	}

	public boolean setSchema(Schema schema, Settings settings, boolean async) {

		ObjectNode mapping = schema.enhance().mapping();

		Index index = DataStore.toDataIndex(schema.name());
		boolean indexExists = elastic().exists(index);

		if (indexExists)
			elastic().putMapping(index, mapping.toString());
		else
			elastic().createIndex(index, mapping.toString(), settings, async);

		return !indexExists;
	}

	//
	// DataObject help methods
	//

	public GetResponse getObject(String type, String id, boolean throwNotFound) {
		GetResponse response = elastic().get(toDataIndex(type), id);

		if (throwNotFound && !response.isExists())
			throw NotFoundException.object(type, id);

		return response;
	}

	public <K> K getObject(String type, String id, Class<K> dataSourceClass, boolean throwNotFound) {
		GetResponse response = getObject(type, id, throwNotFound);
		return response.isExists() //
				? Json.toPojo(response.getSourceAsBytes(), dataSourceClass) //
				: null;
	}

	public <K> DataWrap<K> getObject(DataWrap<K> object) {
		return getObject(object, true);
	}

	public <K> DataWrap<K> getObject(//
			DataWrap<K> object, boolean throwNotFound) {

		GetResponse response = getObject(object.type(), object.id(), throwNotFound);

		if (!response.isExists())
			return null;

		K source = Json.toPojo(response.getSourceAsBytes(), object.sourceClass());
		return object.version(response.getVersion()).source(source);
	}

	private static final String[] METADATA_FIELDS = //
			new String[] { OWNER_FIELD, GROUP_FIELD, CREATED_AT_FIELD, UPDATED_AT_FIELD };

	public Optional<DataWrap<DataObject>> getMetadata(String type, String id) {

		GetResponse response = elastic().prepareGet(toDataIndex(type), id)//
				.setFetchSource(METADATA_FIELDS, null)//
				.get();

		DataWrap<DataObject> metadata = null;

		if (response.isExists())
			metadata = new DataObjectWrap()//
					.type(type).id(id).version(response.getVersion())//
					.source(Json.toPojo(response.getSourceAsBytes(), DataObjectBase.class));

		return Optional.ofNullable(metadata);
	}

	public <K> DataWrap<K> createObject(DataWrap<K> wrap) {

		IndexResponse response = wrap.id() == null //
				? elastic().index(toDataIndex(wrap.type()), wrap.source())//
				: elastic().index(toDataIndex(wrap.type()), wrap.id(), wrap.source());

		return wrap.id(response.getId())//
				.version(response.getVersion())//
				.justCreated(ElasticUtils.isCreated(response));
	}

	public <K> DataWrap<K> updateObject(DataWrap<K> wrap) {

		IndexResponse response = elastic().prepareIndex(//
				toDataIndex(wrap.type()), wrap.id())//
				.setSource(Json.toString(wrap.source()), XContentType.JSON)//
				.setVersion(version(wrap))//
				.get();

		return wrap.version(response.getVersion())//
				.justCreated(ElasticUtils.isCreated(response));
	}

	public <K> DataWrap<K> patchObject(DataWrap<K> wrap) {

		ObjectNode source = Json.toObjectNode(wrap.source());
		source.put(UPDATED_AT_FIELD, DateTime.now().toString());

		UpdateResponse response = elastic()//
				.prepareUpdate(toDataIndex(wrap.type()), wrap.id())//
				.setVersion(version(wrap))//
				.setDoc(source.toString(), XContentType.JSON)//
				.get();

		return wrap.version(response.getVersion())//
				.justCreated(ElasticUtils.isCreated(response));
	}

	public boolean deleteObject(String type, String id, boolean refresh, boolean throwNotFound) {
		return elastic().delete(toDataIndex(type), id, refresh, throwNotFound);
	}

	public static long version(DataWrap<?> wrap) {
		return wrap.version() == 0 ? Versions.MATCH_ANY : wrap.version();
	}

	//
	// Index help methods
	//

	public static Set<String> allDataTypes() {
		return elastic().filteredBackendIndexStream(Optional.of("data"))//
				.map(index -> Index.valueOf(index).type())//
				.collect(Collectors.toSet());
	}

	public static Index[] allDataIndices() {
		return elastic().filteredBackendIndexStream(Optional.of("data"))//
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

	//
	// Search help methods
	//

	public SearchHits search(String type, Object... terms) {
		return elastic().search(toDataIndex(type), terms);
	}

	public SearchHits search(SearchSourceBuilder source, String... types) {
		return elastic().search(source, toDataIndex(types));
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
			search.setFrom(context.request().query().getInteger(FROM_PARAM, 0))
					.setSize(context.request().query().getInteger(SIZE_PARAM, 10))
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
							Json.toObject(field.getValue()))));
			return this;
		}

		public SearchResponse get() throws InterruptedException, ExecutionException {
			return search.setQuery(boolBuilder).get();
		}
	}

	//
	// Refresh help methods
	//

	public void refreshDataTypes(String... types) {
		refreshDataTypes(true, types);
	}

	public void refreshDataTypes(boolean refresh, String... types) {
		if (refresh && !Utils.isNullOrEmpty(types))
			elastic().refreshIndex(toDataIndex(types));
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
		return Server.get().elasticClient();
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
