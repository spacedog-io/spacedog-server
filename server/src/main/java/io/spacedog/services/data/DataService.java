package io.spacedog.services.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.RolePermissions;
import io.spacedog.client.data.CsvRequest;
import io.spacedog.client.data.DataGetAllRequest;
import io.spacedog.client.data.DataImportRequest;
import io.spacedog.client.data.DataImportRequestBuilder;
import io.spacedog.client.data.DataObject;
import io.spacedog.client.data.DataObjectBase;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.services.Server;
import io.spacedog.services.Services;
import io.spacedog.services.SpaceService;
import io.spacedog.services.db.elastic.ElasticExportStreamingOutput;
import io.spacedog.services.db.elastic.ElasticIndex;
import io.spacedog.services.db.elastic.ElasticVersion;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.payload.StreamingOutput;

public class DataService extends SpaceService implements SpaceFields, SpaceParams {

	//
	// Get
	//

	public <K> DataWrap<K> fetch(DataWrap<K> wrap) {
		GetResponse response = doGet(wrap.type(), wrap.id(), true);

		K source = wrap.source() == null //
				? Json.toPojo(response.getSourceAsBytes(), wrap.sourceClass())//
				: Json.updatePojo(response.getSourceAsBytes(), wrap.source());

		return wrap.source(source)//
				.version(ElasticVersion.toString(response.getSeqNo(), response.getPrimaryTerm()));
	}

	public <K> K fetch(String type, String id, K object) {
		GetResponse response = doGet(type, id, true);
		return Json.updatePojo(response.getSourceAsBytes(), object);
	}

	public ObjectNode get(String type, String id) {
		return get(type, id, true);
	}

	public ObjectNode get(String type, String id, boolean throwNotFound) {
		return get(type, id, ObjectNode.class, throwNotFound);
	}

	public <K> K get(String type, String id, Class<K> sourceClass) {
		return get(type, id, sourceClass, true);
	}

	public <K> K get(String type, String id, Class<K> sourceClass, boolean throwNotFound) {
		GetResponse response = doGet(type, id, throwNotFound);

		if (!response.isExists())
			return null;

		return Json.toPojo(response.getSourceAsBytes(), sourceClass);
	}

	public DataWrap<ObjectNode> getWrapped(String type, String id) {
		return getWrapped(type, id, true);
	}

	public DataWrap<ObjectNode> getWrapped(String type, String id, boolean throwNotFound) {
		return getWrapped(type, id, ObjectNode.class, throwNotFound);
	}

	public <K> DataWrap<K> getWrapped(String type, String id, Class<K> sourceClass) {
		return getWrapped(type, id, sourceClass, true);
	}

	public <K> DataWrap<K> getWrapped(String type, String id, Class<K> sourceClass, boolean throwNotFound) {
		GetResponse response = doGet(type, id, throwNotFound);

		if (!response.isExists())
			return null;

		return wrap(response, sourceClass);
	}

	private GetResponse doGet(String type, String id, boolean throwNotFound) {
		GetResponse response = elastic().get(index(type), id);

		if (throwNotFound && !response.isExists())
			throw Exceptions.objectNotFound(type, id);

		return response;
	}

	private <K> DataWrap<K> wrap(GetResponse response, Class<K> sourceClass) {
		return DataWrap.wrap(//
				Json.toPojo(response.getSourceAsBytes(), sourceClass))//
				.id(response.getId())//
				.type(ElasticIndex.valueOf(response.getIndex()).type())//
				.version(ElasticVersion.toString(response.getSeqNo(), response.getPrimaryTerm()));
	}

	//
	// Meta
	//

	private static final String[] META_FIELDS = //
			new String[] { OWNER_FIELD, GROUP_FIELD, CREATED_AT_FIELD, UPDATED_AT_FIELD };

	public Optional<DataWrap<DataObjectBase>> getMeta(String type, String id) {

		GetRequest request = elastic().prepareGet(index(type), id)//
				.fetchSourceContext(new FetchSourceContext(true, META_FIELDS, null));
		GetResponse response = elastic().get(request);

		DataWrap<DataObjectBase> wrap = null;

		if (response.isExists()) {
			DataObjectBase base = Json.toPojo(response.getSourceAsBytes(), DataObjectBase.class);
			wrap = DataWrap.wrap(base).type(type).id(id)//
					.version(ElasticVersion.toString(response.getSeqNo(), response.getPrimaryTerm()));
		}
		return Optional.ofNullable(wrap);
	}

	public DataWrap<DataObjectBase> getMetaOrThrow(String type, String id) {
		return Services.data().getMeta(type, id)//
				.orElseThrow(() -> Exceptions.objectNotFound(type, id));
	}

	private <K> void createMeta(DataWrap<K> object, Credentials credentials) {
		object.owner(credentials.id());
		object.group(checkGroupCreate(object.group(), object.type(), credentials));
		DateTime now = DateTime.now();
		object.createdAt(now);
		object.updatedAt(now);
	}

	private String checkGroupCreate(String group, String type, Credentials credentials) {
		if (Utils.isNullOrEmpty(group))
			return credentials.group();
		DataAccessControl.roles(type).checkGroupCreate(group, credentials);
		return group;
	}

	private <K> void updateMeta(DataWrap<K> object, //
			DataObject meta, Credentials credentials) {
		object.owner(meta.owner());
		object.group(checkGroupUpdate(meta.group(), object.group(), object.type(), credentials));
		object.createdAt(meta.createdAt());
		object.updatedAt(DateTime.now());
	}

	private String checkGroupUpdate(String oldGroup, String newGroup, String type, Credentials credentials) {

		if (Utils.isNullOrEmpty(newGroup) || oldGroup.equals(newGroup))
			return oldGroup;

		DataAccessControl.roles(type).checkGroupUpdate(newGroup, credentials);
		return newGroup;
	}

	//
	// Save
	//

	public <K> DataWrap<K> save(String type, K source) {
		return save(type, null, source);
	}

	public <K> DataWrap<K> save(String type, String id, K source) {
		return save(type, id, null, source);
	}

	public <K> DataWrap<K> save(String type, String id, String version, K source) {
		return save(DataWrap.wrap(source).type(type).id(id).version(version));
	}

	public <K> DataWrap<K> save(DataWrap<K> wrap) {

		if (Utils.atLeastOneIsNullOrEmpty(wrap.owner(), wrap.group())//
				|| Utils.atLeastOneIsNull(wrap.createdAt(), wrap.updatedAt()))
			throw Exceptions.illegalArgument("meta fields are mandatory");

		IndexResponse response = elastic().index(//
				index(wrap.type()), wrap.id(), wrap.version(), wrap.source(), false);

		return wrap.id(response.getId())//
				.version(ElasticVersion.toString(response.getSeqNo(), response.getPrimaryTerm()));
	}

	//
	// Patch
	//

	public String patch(String type, String id, Object source) {
		return patch(type, id, null, source);
	}

	public String patch(String type, String id, String version, Object patch) {
		return patch(DataWrap.wrap(patch).type(type).id(id).version(version)).version();
	}

	public <K> DataWrap<K> patch(DataWrap<K> wrap) {

		ObjectNode source = Json.toObjectNode(wrap.source());

		if (source.has(OWNER_FIELD) || source.has(GROUP_FIELD) //
				|| source.has(CREATED_AT_FIELD) || source.has(UPDATED_AT_FIELD))
			throw Exceptions.illegalArgument("patching meta fields is forbidden");

		source.put(UPDATED_AT_FIELD, DateTime.now().toString());

		UpdateRequest request = elastic().prepareUpdate(index(wrap.type()), wrap.id())//
				.doc(source.toString(), XContentType.JSON);

		if (wrap.version() != null) {
			ElasticVersion version = ElasticVersion.valueOf(wrap.version());
			request.setIfSeqNo(version.seqNo);
			request.setIfPrimaryTerm(version.primaryTerm);
		}

		elastic().update(request);

		return getWrapped(wrap.type(), wrap.id(), wrap.sourceClass());
	}

	//
	// Delete
	//

	public boolean delete(DataWrap<?> object) {
		return delete(object.type(), object.id());
	}

	public boolean delete(String type, String id) {
		return delete(type, id, true);
	}

	public boolean delete(String type, String id, boolean throwNotFound) {
		return elastic().delete(index(type), id, false, true);
	}

	//
	// Field methods
	//

	public <K> K getField(String type, String id, String field, Class<K> valueClass) {
		return Json.toPojo(Json.get(get(type, id), field), valueClass);
	}

	public String saveField(String type, String id, String field, Object value) {
		ObjectNode source = Json.object();
		Json.with(source, field, value);
		return patch(type, id, source);
	}

	public String deleteField(String type, String id, String field) {
		ObjectNode source = get(type, id);
		Json.remove(source, field);
		return save(type, id, source).version();
	}

	//
	// Get All Request
	//

	public <K> DataResults<K> getAll(DataGetAllRequest request, Class<K> sourceClass) {

		if (Strings.isNullOrEmpty(request.type))
			refreshAllTypes(request.refresh);
		else
			refresh(request.refresh, request.type);

		QueryBuilder query = Strings.isNullOrEmpty(request.q) //
				? QueryBuilders.matchAllQuery()//
				: QueryBuilders.simpleQueryStringQuery(request.q);

		SearchSourceBuilder search = SearchSourceBuilder.searchSource()//
				.from(request.from).size(request.size).query(query);

		return search(sourceClass, search, request.type);
	}

	//
	// Delete Bulk Request
	//

	public long deleteAll(String type) {
		return deleteAll(QueryBuilders.matchAllQuery(), type);
	}

	public long deleteAll(QueryBuilder query, String... types) {
		return elastic().deleteByQuery(query, index(types)).getDeleted();
	}

	//
	// Search Request
	//

	public <K> DataResults<K> search(Class<K> sourceClass, String type, Object... terms) {
		return extract(elastic().search(index(type), terms), sourceClass);
	}

	public <K> DataResults<K> search(Class<K> sourceClass, SearchSourceBuilder source, String... types) {
		ElasticIndex[] indices = Utils.isNullOrEmpty(types) ? indices() : index(types);
		return extract(elastic().search(source, indices), sourceClass);
	}

	private <K> DataResults<K> extract(SearchResponse response, Class<K> sourceClass) {

		SearchHits hits = response.getHits();
		DataResults<K> results = DataResults.of(sourceClass);
		results.total = hits.getTotalHits().value;
		results.objects = Lists.newArrayListWithCapacity(hits.getHits().length);
		for (SearchHit hit : hits)
			results.objects.add(wrap(hit, sourceClass));
		Aggregations aggregations = response.getAggregations();
		results.aggregations = aggregations == null ? null : aggregations.asMap();
		return results;
	}

	private <K> DataWrap<K> wrap(SearchHit hit, Class<K> sourceClass) {

		return DataWrap.wrap(Json.toPojo(hit.getSourceAsString(), sourceClass))//
				.id(hit.getId())//
				.type(ElasticIndex.valueOf(hit.getIndex()).type())//
				.version(ElasticVersion.toString(hit.getSeqNo(), hit.getPrimaryTerm()))//
				.score(extractScore(hit.getScore()))//
				.sort(extractSortValues(hit.getSortValues()));
	}

	private Object[] extractSortValues(Object[] sortValues) {
		return Utils.isNullOrEmpty(sortValues) ? null : sortValues;
	}

	private float extractScore(float score) {
		return Float.isFinite(score) ? score : 0;
	}

	//
	// CSV
	//

	public StreamingOutput csv(String type, CsvRequest csvRequest, Locale locale) {

		Services.data().refresh(csvRequest.refresh, type);

		SearchSourceBuilder builder = SearchSourceBuilder.searchSource()//
				.query(QueryBuilders.wrapperQuery(csvRequest.query))//
				.size(csvRequest.pageSize);

		SearchRequest searchRequest = elastic().prepareSearch(index(type))//
				.scroll(TimeValue.timeValueSeconds(60))//
				.source(builder);

		SearchResponse response = elastic().search(searchRequest);
		return new CsvStreamingOutput(csvRequest, response, locale);
	}

	//
	// Import Export
	//

	public StreamingOutput exportNow(String type, QueryBuilder query) {

		SearchSourceBuilder source = SearchSourceBuilder.searchSource()//
				.size(ElasticExportStreamingOutput.SIZE)//
				.query(query);

		SearchRequest request = elastic().prepareSearch(index(type))//
				.scroll(ElasticExportStreamingOutput.TIMEOUT)//
				.source(source);

		SearchResponse response = elastic().search(request);
		return new ElasticExportStreamingOutput(response);
	}

	public DataImportRequestBuilder prepareImport(String type) {
		return new DataImportRequestBuilder(type) {
			@Override
			public void go(InputStream export) throws IOException {
				importNow(this.build(), export);
			}
		};
	}

	public void importNow(DataImportRequest request, InputStream data) throws IOException {

		BufferedReader reader = new BufferedReader(//
				new InputStreamReader(data));

		ElasticIndex index = index(request.type);
		String json = reader.readLine();

		while (json != null) {
			ObjectNode object = Json.readObject(json);
			String source = object.get("source").toString();

			if (request.preserveIds) {
				String id = object.get("id").asText();
				elastic().index(index, id, source);
			} else
				elastic().index(index, source);

			json = reader.readLine();
		}
	}

	//
	// Index help methods
	//

	public static final String SERVICE_NAME = "data";

	public boolean isType(String type) {
		return elastic().exists(index(type));
	}

	public Set<String> types() {
		return elastic().filteredBackendIndexStream(Optional.of(SERVICE_NAME))//
				.map(index -> ElasticIndex.valueOf(index).type())//
				.collect(Collectors.toSet());
	}

	public ElasticIndex[] indices() {
		return elastic().filteredBackendIndexStream(Optional.of(SERVICE_NAME))//
				.map(index -> ElasticIndex.valueOf(index))//
				.toArray(ElasticIndex[]::new);
	}

	public ElasticIndex index(String type) {
		return new ElasticIndex(SERVICE_NAME).type(type);
	}

	public ElasticIndex[] index(String... types) {
		return Arrays.stream(types)//
				.map(type -> index(type))//
				.toArray(ElasticIndex[]::new);
	}

	//
	// Refresh help methods
	//

	public void refresh(String... types) {
		refresh(true, types);
	}

	public void refresh(boolean refresh, String... types) {
		if (refresh && !Utils.isNullOrEmpty(types))
			elastic().refreshIndex(index(types));
	}

	public void refreshAllTypes() {
		refreshAllTypes(true);
	}

	public void refreshAllTypes(boolean refresh) {
		if (refresh)
			elastic().refreshBackend();
	}

	//
	// Get, Update, Patch, Delete if authorized
	//

	public DataWrap<ObjectNode> getIfAuthorized(String type, String id) {
		DataWrap<ObjectNode> object = Services.data().getWrapped(type, id);
		checkReadPermission(object);
		return object;
	}

	public <K> DataWrap<K> saveIfAuthorized(DataWrap<K> object, boolean forceMeta) {

		Credentials credentials = Server.context().credentials();

		if (forceMeta)
			DataAccessControl.roles(object.type())//
					.checkPermission(credentials, Permission.forceMeta);

		if (object.id() != null) {

			Optional<DataWrap<DataObjectBase>> meta = Services.data()//
					.getMeta(object.type(), object.id());

			if (meta.isPresent()) {
				checkUpdatePermissions(meta.get());

				if (!forceMeta)
					updateMeta(object, meta.get().source(), credentials);

				return Services.data().save(object);
			}
		}

		DataAccessControl.roles(object.type()).checkPermission(credentials, //
				Permission.createMine, Permission.createGroup, Permission.create);

		if (!forceMeta)
			createMeta(object, credentials);

		return Services.data().save(object);
	}

	public <K> DataWrap<K> patchIfAuthorized(DataWrap<K> object) {

		DataWrap<DataObjectBase> meta = Services.data()//
				.getMeta(object.type(), object.id())//
				.orElseThrow(() -> Exceptions.objectNotFound(object));

		checkUpdatePermissions(meta);

		return Services.data().patch(object);
	}

	public boolean deleteIfAuthorized(String type, String id) {
		checkDeletePermission(type, id);
		return delete(type, id);
	}

	//
	// Check object permission
	//

	public void checkReadPermission(DataWrap<?> object) {

		Credentials credentials = Server.context().credentials();
		RolePermissions permissions = DataAccessControl.roles(object.type());

		if (permissions.hasOne(credentials, Permission.read, Permission.search))
			return;

		if (permissions.hasOne(credentials, Permission.readMine)) {
			credentials.checkOwnerAccess(object.owner(), object.type(), object.id());
			return;
		}

		if (permissions.hasOne(credentials, Permission.readGroup)) {
			credentials.checkGroupAccessPermission(object.group());
			return;
		}

		throw Exceptions.forbidden(credentials, //
				"forbidden to read [%s] objects", object.type());
	}

	public void checkUpdatePermissions(DataWrap<?> object) {

		Credentials credentials = Server.context().credentials();
		RolePermissions permissions = DataAccessControl.roles(object.type());

		if (permissions.hasOne(credentials, Permission.update))
			return;

		if (permissions.hasOne(credentials, Permission.updateMine)) {
			credentials.checkOwnerAccess(object.owner(), object.type(), object.id());
			return;
		}

		if (permissions.hasOne(credentials, Permission.updateGroup)) {
			credentials.checkGroupAccessPermission(object.group());
			return;
		}

		throw Exceptions.forbidden(credentials, //
				"forbidden to update [%s] objects", object.type());
	}

	public void checkDeletePermission(String type, String id) {

		Credentials credentials = Server.context().credentials();
		RolePermissions permissions = DataAccessControl.roles(type);

		if (permissions.hasOne(credentials, Permission.delete))
			return;

		if (permissions.hasOne(credentials, Permission.deleteMine)) {
			DataWrap<DataObjectBase> meta = getMetaOrThrow(type, id);
			credentials.checkOwnerAccess(meta.owner(), type, id);
			return;
		}

		else if (permissions.hasOne(credentials, Permission.deleteGroup)) {
			DataWrap<DataObjectBase> meta = getMetaOrThrow(type, id);
			credentials.checkGroupAccessPermission(meta.group());
			return;
		}

		throw Exceptions.forbidden(credentials, //
				"forbidden to delete [%s] objects", type);
	}

	//
	// Init
	//

	public void init() {
		try {

			PutIndexTemplateRequest request = new PutIndexTemplateRequest("data")//
					.source(ClassResources.loadAsBytes(this, "data-template.json"), //
							XContentType.JSON);

			elastic().internal().indices().putTemplate(request, RequestOptions.DEFAULT);

		} catch (IOException e) {
			Exceptions.runtime(e);
		}
	}

	public DataSettings settings() {
		return Services.settings().getOrThrow(DataSettings.class);
	}

}
