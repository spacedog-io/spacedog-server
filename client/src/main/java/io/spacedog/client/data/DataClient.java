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

	//
	// GET
	//

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
		DataWrap<K> wrap = getWrapped(type, id, sourceClass, throwNotFound);
		return wrap == null ? null : wrap.source();
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

		return dog.get("/1/data/{type}/{id}")//
				.routeParam(TYPE_FIELD, type)//
				.routeParam(ID_FIELD, id)//
				.go(expectedStatus);
	}

	//
	// Save
	//

	public <K> DataWrap<K> save(String type, K source) {
		return save(type, null, source);
	}

	public <K> DataWrap<K> save(String type, String id, K source) {
		return save(type, id, -3, source);
	}

	public <K> DataWrap<K> save(String type, String id, long version, K source) {
		return save(DataWrap.wrap(source).type(type).id(id).version(version));
	}

	public <K> DataWrap<K> save(DataWrap<K> object) {

		if (object.id() == null)
			return dog.post("/1/data/{type}")//
					.routeParam(TYPE_FIELD, object.type())//
					.bodyPojo(object.source())//
					.go(201)//
					.asPojo(object);

		SpaceRequest request = dog.put("/1/data/{type}/{id}")//
				.routeParam(TYPE_FIELD, object.type())//
				.routeParam(ID_FIELD, object.id())//
				.bodyPojo(object.source());

		if (object.version() > 0)
			request.queryParam(VERSION_PARAM, object.version());

		return request.go(200, 201).asPojo(object);
	}

	//
	// Patch
	//

	public long patch(String type, String id, Object source) {
		return patch(type, id, source, DataWrap.MATCH_ANY_VERSIONS);
	}

	public long patch(String type, String id, Object source, long version) {
		SpaceRequest request = dog.put("/1/data/{type}/{id}")//
				.routeParam(ID_FIELD, id)//
				.routeParam(TYPE_FIELD, type)//
				.queryParam(PATCH_PARAM, true);

		if (version >= 0)
			request.queryParam(VERSION_PARAM, version);

		return request.bodyPojo(source).go(200)//
				.get(VERSION_FIELD).asLong();
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
		SpaceRequest request = dog.delete("/1/data/{type}/{id}")//
				.routeParam(TYPE_FIELD, type)//
				.routeParam(ID_FIELD, id);

		if (throwNotFound)
			request.go(200);
		else
			request.go(200, 404);
	}

	//
	// Field methods
	//

	public <K> K get(String type, String id, String field, Class<K> dataClass) {
		return dog.get("/1/data/{t}/{i}/{f}").routeParam("i", id)//
				.routeParam("t", type).routeParam("f", field)//
				.go(200).asPojo(dataClass);
	}

	public long save(String type, String id, String field, Object object) {
		return dog.put("/1/data/{t}/{i}/{f}").routeParam("i", id)//
				.routeParam("t", type).routeParam("f", field)//
				.bodyPojo(object).go(200).get(VERSION_FIELD).asLong();
	}

	public Long delete(String type, String id, String field) {
		return dog.delete("/1/data/{t}/{i}/{f}")//
				.routeParam("i", id).routeParam("t", type)//
				.routeParam("f", field).go(200).get(VERSION_FIELD).asLong();
	}

	//
	// Get All Request
	//

	public DataGetAllRequest prepareGetAll() {
		return new DataGetAllRequest() {
			@Override
			public <K> DataResults<K> go(Class<K> resultsClass) {
				return getAll(this, resultsClass);
			}
		};
	}

	public <K> DataResults<K> getAll(DataGetAllRequest request, Class<K> sourceClass) {

		String path = "/1/data";
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
		return dog.delete("/1/data/" + type)//
				.refresh().go(200).get("deleted").asLong();
	}

	public DataBulkDeleteRequest prepareBulkDelete() {
		return new DataBulkDeleteRequest() {
			@Override
			public long go() {
				return bulkDelete(this);
			}
		};
	}

	public long bulkDelete(DataBulkDeleteRequest request) {

		String path = "/1/data";

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

	public DataSearchRequest prepareSearch() {
		return new DataSearchRequest() {
			@Override
			public <K> DataResults<K> go(Class<K> sourceClass) {
				return search(this, sourceClass);
			}
		};
	}

	public <K> DataResults<K> search(DataSearchRequest request, Class<K> sourceClass) {

		String path = "/1/data";

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
		return dog.post("/1/data/{type}/_csv")//
				.routeParam("type", type)//
				.bodyPojo(request).go(200);
	}

	//
	// Import Export
	//

	public DataExportRequest prepareExport(String type) {
		return new DataExportRequest(type) {
			@Override
			public SpaceResponse go() {
				return exportNow(this);
			}

		};
	}

	public SpaceResponse exportNow(DataExportRequest request) {
		return dog.post("/1/data/{type}/_export")//
				.routeParam("type", request.type)//
				.queryParam(REFRESH_PARAM, request.refresh)//
				.body(request.query)//
				.go(200);
	}

	public DataImportRequest prepareImport(String type) {
		return new DataImportRequest(type) {
			@Override
			public void go(InputStream export) {
				importNow(this, export);
			}
		};
	}

	public void importNow(DataImportRequest request, InputStream export) {
		dog.post("/1/data/{type}/_import")//
				.withContentType(OkHttp.TEXT_PLAIN)//
				.routeParam("type", request.type)//
				.queryParam(PRESERVE_IDS_PARAM, request.preserveIds)//
				.body(export)//
				.go(200);
	}
}
