package io.spacedog.client.data;

import java.io.InputStream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.http.OkHttp;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.utils.Json;

public class DataClient implements SpaceFields, SpaceParams {

	private SpaceDog dog;

	public DataClient(SpaceDog session) {
		this.dog = session;
	}

	public static String type(Object source) {
		Class<?> sourceClass = source instanceof Class<?> //
				? (Class<?>) source
				: source.getClass();
		return sourceClass.getSimpleName().toLowerCase();
	}

	//
	// GET
	//

	public ObjectNode get(String type, String id) {
		return get(type, id, true);
	}

	public ObjectNode get(String type, String id, boolean throwNotFound) {
		return get(type, id, ObjectNode.class, throwNotFound);
	}

	public <K> K get(String id, Class<K> sourceClass) {
		return get(type(sourceClass), id, sourceClass, true);
	}

	public <K> K get(String type, String id, Class<K> sourceClass) {
		return get(type, id, sourceClass, true);
	}

	public <K> K get(String type, String id, Class<K> sourceClass, boolean throwNotFound) {
		DataWrap<K> wrap = getWrapped(type, id, sourceClass, throwNotFound);
		return wrap == null ? null : wrap.source();
	}

	public DataWrap<ObjectNode> getWrapped(String type, String id) {
		return getWrapped(type, id, true);
	}

	public DataWrap<ObjectNode> getWrapped(String type, String id, boolean throwNotFound) {
		return getWrapped(type, id, ObjectNode.class, throwNotFound);
	}

	public <K> DataWrap<K> getWrapped(String id, Class<K> sourceClass) {
		return getWrapped(type(sourceClass), id, sourceClass, true);
	}

	public <K> DataWrap<K> getWrapped(String type, String id, Class<K> sourceClass) {
		return getWrapped(type, id, sourceClass, true);
	}

	public <K> DataWrap<K> getWrapped(String id, Class<K> sourceClass, boolean throwNotFound) {
		return getWrapped(type(sourceClass), id, sourceClass, throwNotFound);
	}

	public <K> DataWrap<K> getWrapped(String type, String id, Class<K> sourceClass, boolean throwNotFound) {
		DataWrap<K> wrap = DataWrap.wrap(sourceClass).type(type).id(id);
		return fetch(wrap, throwNotFound);
	}

	public <K> DataWrap<K> fetch(DataWrap<K> wrap) {
		return fetch(wrap, true);
	}

	public <K> DataWrap<K> fetch(DataWrap<K> wrap, boolean throwNotFound) {
		return fetch(wrap.type(), wrap.id(), wrap, throwNotFound);
	}

	public <K> K fetch(String type, String id, K object) {
		return fetch(type, id, object, true);
	}

	public <K> K fetch(String type, String id, K object, boolean throwNotFound) {
		SpaceResponse response = doGet(type, id, throwNotFound);
		return response.status() == 404 ? null //
				: response.asPojo(object);
	}

	private SpaceResponse doGet(String type, String id, boolean throwNotFound) {
		int[] expectedStatus = throwNotFound //
				? new int[] { 200 }
				: new int[] { 200, 404 };

		return dog.get("/2/data/{type}/{id}")//
				.routeParam(TYPE_FIELD, type)//
				.routeParam(ID_FIELD, id)//
				.go(expectedStatus);
	}

	//
	// Save
	//

	public <K> DataWrap<K> create(K source) {
		return save(source, null);
	}

	public <K> DataWrap<K> save(K source, String id) {
		return save(source, id, null);
	}

	public <K> DataWrap<K> save(K source, String id, String version) {
		return save(DataWrap.wrap(source).id(id).version(version));
	}

	public <K> DataWrap<K> save(String type, K source) {
		return save(type, source, null);
	}

	public <K> DataWrap<K> save(String type, K source, String id) {
		return save(type, source, id, null);
	}

	public <K> DataWrap<K> save(String type, K source, String id, String version) {
		return save(DataWrap.wrap(source).type(type).id(id).version(version));
	}

	public <K> DataSaveRequestBuilder<K> prepareSave(K source) {
		return new DataSaveRequestBuilder<K>(source) {
			@Override
			public DataWrap<K> go() {
				return save(build());
			}
		};
	}

	public <K> DataWrap<K> save(DataWrap<K> object) {
		return save(object, false);
	}

	public <K> DataWrap<K> save(DataWrap<K> object, boolean forceMeta) {

		if (object.id() == null)
			return dog.post("/2/data/{type}")//
					.routeParam(TYPE_FIELD, object.type())//
					.queryParam(FORCE_META_PARAM, forceMeta ? true : null)//
					.bodyPojo(object.source())//
					.go(201)//
					.asPojo(object);

		SpaceRequest request = dog.put("/2/data/{type}/{id}")//
				.routeParam(TYPE_FIELD, object.type())//
				.routeParam(ID_FIELD, object.id())//
				.queryParam(FORCE_META_PARAM, forceMeta ? true : null)//
				.bodyPojo(object.source());

		if (object.version() != null)
			request.queryParam(VERSION_PARAM, object.version());

		return request.go(200, 201).asPojo(object);
	}

	//
	// Patch
	//

	public String patch(String type, String id, Object source) {
		return patch(type, id, source, null);
	}

	public String patch(String type, String id, Object source, String version) {
		SpaceRequest request = dog.put("/2/data/{type}/{id}")//
				.routeParam(ID_FIELD, id)//
				.routeParam(TYPE_FIELD, type)//
				.queryParam(PATCH_PARAM, true)//
				.queryParam(VERSION_PARAM, version);

		return request.bodyPojo(source).go(200)//
				.get(VERSION_FIELD).asText(null);
	}

	//
	// Delete
	//

	public void delete(DataWrap<?> object) {
		delete(object.type(), object.id());
	}

	public void delete(String type, String id) {
		delete(type, id, true);
	}

	public void delete(String type, String id, boolean throwNotFound) {
		SpaceRequest request = dog.delete("/2/data/{type}/{id}")//
				.routeParam(TYPE_FIELD, type)//
				.routeParam(ID_FIELD, id);

		if (throwNotFound)
			request.go(200).asVoid();
		else
			request.go(200, 404).asVoid();
	}

	public long searchDelete(String type, String query) {
		return dog.delete("/2/data/{type}/_search")//
				.routeParam(TYPE_FIELD, type)//
				.bodyJson(query)//
				.go(200)//
				.get("deleted").asLong();
	}

	//
	// Field methods
	//

	public <K> K getField(String type, String id, String field, Class<K> dataClass) {
		return dog.get("/2/data/{t}/{i}/{f}").routeParam("i", id)//
				.routeParam("t", type).routeParam("f", field)//
				.go(200).asPojo(dataClass);
	}

	public String saveField(String type, String id, String field, Object object) {
		return dog.put("/2/data/{t}/{i}/{f}").routeParam("i", id)//
				.routeParam("t", type).routeParam("f", field)//
				.bodyPojo(object).go(200).get(VERSION_FIELD).asText(null);
	}

	public String deleteField(String type, String id, String field) {
		return dog.delete("/2/data/{t}/{i}/{f}")//
				.routeParam("i", id).routeParam("t", type)//
				.routeParam("f", field).go(200).get(VERSION_FIELD).asText(null);
	}

	//
	// Get All Request
	//

	public DataGetAllRequestBuilder prepareGetAll() {
		return new DataGetAllRequestBuilder() {
			@Override
			public <K> DataResults<K> go(Class<K> resultsClass) {
				return getAll(this.build(), resultsClass);
			}
		};
	}

	public <K> DataResults<K> getAll(DataGetAllRequest request, Class<K> sourceClass) {

		String path = "/2/data";
		if (!Strings.isNullOrEmpty(request.type))
			path = path + "/" + request.type;

		return dog.get(path)//
				.refresh(request.refresh)//
				.queryParam(Q_PARAM, request.q)//
				.queryParam(FROM_PARAM, request.from)//
				.queryParam(SIZE_PARAM, request.size)//
				.go(200)//
				.asPojo(DataResults.of(sourceClass));
	}

	//
	// Delete Bulk Request
	//

	public long deleteAll(String type) {
		return dog.delete("/2/data/" + type)//
				.refresh().go(200).get("deleted").asLong();
	}

	public DataBulkDeleteRequestBuilder prepareBulkDelete() {
		return new DataBulkDeleteRequestBuilder() {
			@Override
			public long go() {
				return bulkDelete(this.build());
			}
		};
	}

	public long bulkDelete(DataBulkDeleteRequest request) {

		String path = "/2/data";

		if (!Strings.isNullOrEmpty(request.type))
			path = path + "/" + request.type;

		SpaceRequest spaceRequest = dog.delete(path + "/_search")//
				.refresh(request.refresh);

		if (!Strings.isNullOrEmpty(request.query))
			spaceRequest.bodyJson(request.query);

		return spaceRequest.go(200).get("deleted").asLong();
	}

	//
	// Search Request
	//

	public DataSearchRequestBuilder prepareSearch() {
		return new DataSearchRequestBuilder() {
			@Override
			public <K> DataResults<K> go(Class<K> sourceClass) {
				return search(this.build(), sourceClass);
			}
		};
	}

	public <K> DataResults<K> search(DataSearchRequest request, Class<K> sourceClass) {

		String path = "/2/data";

		if (!Strings.isNullOrEmpty(request.type))
			path = path + "/" + request.type;

		if (Strings.isNullOrEmpty(request.source))
			request.source = Json.EMPTY_OBJECT;

		return dog.post(path + "/_search").bodyJson(request.source)//
				.refresh(request.refresh).go(200)//
				.asPojo(DataResults.of(sourceClass));
	}

	//
	// CSV
	//

	public SpaceResponse csv(String type, CsvRequest request) {
		return dog.post("/2/data/{type}/_csv")//
				.routeParam("type", type)//
				.bodyPojo(request).go(200);
	}

	//
	// Import Export
	//

	public DataExportRequestBuilder prepareExport(String type) {
		return new DataExportRequestBuilder(type) {
			@Override
			public SpaceResponse go() {
				return exportNow(this.build());
			}

		};
	}

	public SpaceResponse exportNow(DataExportRequest request) {
		return dog.post("/2/data/{type}/_export")//
				.routeParam("type", request.type)//
				.queryParam(REFRESH_PARAM, request.refresh)//
				.bodyJson(request.query)//
				.go(200);
	}

	public DataImportRequestBuilder prepareImport(String type) {
		return new DataImportRequestBuilder(type) {
			@Override
			public void go(InputStream export) {
				importNow(this.build(), export);
			}
		};
	}

	public void importNow(DataImportRequest request, InputStream export) {
		dog.post("/2/data/{type}/_import")//
				.withContentType(OkHttp.TEXT_PLAIN)//
				.routeParam("type", request.type)//
				.queryParam(PRESERVE_IDS_PARAM, request.preserveIds)//
				.bodyStream(export)//
				.go(200)//
				.asVoid();
	}

	//
	// Settings
	//

	public DataSettings settings() {
		return dog.settings().get(DataSettings.class);
	}

	public void settings(DataSettings settings) {
		dog.settings().save(settings);
	}
}
