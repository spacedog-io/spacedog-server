/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.joda.time.DateTime;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import io.spacedog.http.SpaceFields;
import io.spacedog.http.SpaceParams;
import io.spacedog.model.DataObject;
import io.spacedog.model.Metadata;
import io.spacedog.model.MetadataBase;
import io.spacedog.model.MetadataDataObject;
import io.spacedog.model.Schema;
import io.spacedog.utils.Exceptions;
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
			JsonNode node = Json.readObject(mapping.source().toString())//
					.get(mapping.type()).get("_meta");
			schemas.put(mapping.type(), new Schema(mapping.type(), Json.checkObject(node)));
		}

		return schemas;
	}

	public Map<String, Schema> getAllSchemas() {
		return getSchemas(allDataIndices());
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

	public <K> DataObject<K> getObject(DataObject<K> object) {
		return getObject(object, true);
	}

	public <K> DataObject<K> getObject(//
			DataObject<K> object, boolean throwNotFound) {

		GetResponse response = getObject(object.type(), object.id(), throwNotFound);

		if (!response.isExists())
			return null;

		K source = Json.toPojo(response.getSourceAsBytes(), object.sourceClass());
		return object.version(response.getVersion()).source(source);
	}

	private static final String[] METADATA_FIELDS = //
			new String[] { OWNER_FIELD, GROUP_FIELD, CREATED_AT_FIELD, UPDATED_AT_FIELD };

	public Optional<DataObject<Metadata>> getMetadata(String type, String id) {

		GetResponse response = elastic().prepareGet(toDataIndex(type), id)//
				.setFetchSource(METADATA_FIELDS, null)//
				.get();

		DataObject<Metadata> metadata = null;

		if (response.isExists())
			metadata = new MetadataDataObject()//
					.type(type).id(id).version(response.getVersion())//
					.source(Json.toPojo(response.getSourceAsBytes(), MetadataBase.class));

		return Optional.ofNullable(metadata);
	}

	<K> DataObject<K> createObject(DataObject<K> object) {

		IndexResponse response = object.id() == null //
				? elastic().index(toDataIndex(object.type()), object.source())//
				: elastic().index(toDataIndex(object.type()), object.id(), object.source());

		return object.id(response.getId())//
				.version(response.getVersion())//
				.justCreated(response.isCreated());
	}

	public <K> DataObject<K> updateObject(DataObject<K> object) {

		IndexResponse response = elastic().prepareIndex(//
				toDataIndex(object.type()), object.id())//
				.setSource(Json.toString(object.source()))//
				.setVersion(version(object))//
				.get();

		return object.version(response.getVersion())//
				.justCreated(response.isCreated());
	}

	public <K> DataObject<K> patchObject(DataObject<K> object) {

		ObjectNode source = Json.toObjectNode(object.source());
		source.put(UPDATED_AT_FIELD, DateTime.now().toString());

		UpdateResponse response = elastic().prepareUpdate(toDataIndex(object.type()), object.id())//
				.setVersion(version(object)).setDoc(source.toString()).get();

		return object.version(response.getVersion())//
				.justCreated(response.isCreated());
	}

	public static long version(DataObject<?> object) {
		return object.version() == 0 ? Versions.MATCH_ANY : object.version();
	}

	//
	// Index help methods
	//

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

	//
	// Search help methods
	//

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
