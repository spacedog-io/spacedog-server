package io.spacedog.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceResponse;
import io.spacedog.model.DataObject;
import io.spacedog.model.JsonDataObject;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;

public class DataEndpoint implements SpaceFields, SpaceParams {

	SpaceDog dog;

	DataEndpoint(SpaceDog session) {
		this.dog = session;
	}

	//
	// Data object simple CRUD methods
	//

	public DataObject<ObjectNode> get(String type, String id) {
		return fetch(new JsonDataObject().type(type).id(id));
	}

	public <K> DataObject<K> fetch(DataObject<K> object) {
		return dog.get("/1/data/{type}/{id}")//
				.routeParam(TYPE_FIELD, object.type())//
				.routeParam(ID_FIELD, object.id())//
				.go(200)//
				.toPojo(object);
	}

	public DataObject<ObjectNode> save(String type, ObjectNode source) {
		return save(new JsonDataObject().type(type).source(source));
	}

	public DataObject<ObjectNode> save(String type, String id, ObjectNode source) {
		return save(new JsonDataObject().type(type).id(id).source(source));
	}

	public <K> DataObject<K> save(DataObject<K> object) {

		if (object.id() == null)
			return dog.post("/1/data/{type}")//
					.routeParam(TYPE_FIELD, object.type())//
					.bodyPojo(object.source())//
					.go(201)//
					.toPojo(object);

		SpaceRequest request = dog.put("/1/data/{type}/{id}")//
				.routeParam(TYPE_FIELD, object.type())//
				.routeParam(ID_FIELD, object.id())//
				.bodyPojo(object.source());

		if (object.version() > 0)
			request.queryParam(VERSION_PARAM, String.valueOf(object.version()));

		return request.go(200, 201).toPojo(object);
	}

	public long patch(String type, String id, Object source) {
		return dog.put("/1/data/{type}/{id}")//
				.routeParam(ID_FIELD, id)//
				.routeParam(TYPE_FIELD, type)//
				.queryParam(STRICT_PARAM, "false")//
				.bodyPojo(source).go(200)//
				.get(VERSION_FIELD).asLong();
	}

	public DataEndpoint delete(DataObject<?> object) {
		return delete(object.type(), object.id());
	}

	public DataEndpoint delete(String type, String id) {
		return delete(type, id, true);
	}

	public DataEndpoint delete(String type, String id, boolean throwNotFound) {
		SpaceRequest request = dog.delete("/1/data/{type}/{id}")//
				.routeParam(TYPE_FIELD, type)//
				.routeParam(ID_FIELD, id);

		if (throwNotFound)
			request.go(200);
		else
			request.go(200, 404);

		return this;
	}

	//
	// Field simple CRUD methods
	//

	public <K> K get(String type, String id, String field, Class<K> dataClass) {
		return dog.get("/1/data/{t}/{i}/{f}").routeParam("i", id)//
				.routeParam("t", type).routeParam("f", field)//
				.go(200).toPojo(dataClass);
	}

	public long save(String type, String id, String field, Object object) {
		return dog.put("/1/data/{t}/{i}/{f}").routeParam("i", id)//
				.routeParam("t", type).routeParam("f", field)//
				.bodyPojo(object).go(200).get(VERSION_FIELD).asLong();
	}

	public Optional7<Long> delete(String type, String id, String field) {
		SpaceResponse response = dog.delete("/1/data/{t}/{i}/{f}")//
				.routeParam("i", id).routeParam("t", type)//
				.routeParam("f", field).go(200, 404);

		if (response.status() == 404)
			return Optional7.empty();

		return Optional7.of(response.get(VERSION_FIELD).asLong());
	}

	//
	// Get/Delete All methods
	//

	public GetAllRequest getAll() {
		return new GetAllRequest();
	}

	public class GetAllRequest {
		private String type;
		private Integer from;
		private Integer size;
		private Boolean refresh;

		public GetAllRequest type(String type) {
			this.type = type;
			return this;
		}

		public GetAllRequest from(int from) {
			this.from = from;
			return this;
		}

		public GetAllRequest size(int size) {
			this.size = size;
			return this;
		}

		public GetAllRequest refresh() {
			this.refresh = true;
			return this;
		}

		public JsonDataObject.Results get() {
			return get(JsonDataObject.Results.class);
		}

		public <K> K get(Class<K> resultsClass) {

			// if (type == null)
			SpaceRequest request = type == null //
					? dog.get("/1/data") //
					: dog.get("/1/data/{type}").routeParam("type", type);

			if (refresh != null)
				request.queryParam(REFRESH_PARAM, Boolean.toString(refresh));
			if (from != null)
				request.queryParam(FROM_PARAM, Integer.toString(from));
			if (size != null)
				request.queryParam(SIZE_PARAM, Integer.toString(size));

			return request.go(200).toPojo(resultsClass);
		}

	}

	public DataEndpoint deleteAll(String type) {
		dog.delete("/1/data/{type}").routeParam("type", type).go(200);
		return this;
	}

	//
	// Search
	//

	public <K> K search(String type, String q, //
			Class<K> resultsClass, boolean refresh) {

		String path = "/1/search";
		if (!Strings.isNullOrEmpty(type))
			path = path + "/" + type;

		return dog.get(path).refresh(refresh)//
				.queryParam("q", q).go(200).toPojo(resultsClass);
	}

	public <K> K search(ESSearchSourceBuilder builder, Class<K> resultsClass) {
		return search(null, builder, resultsClass, false);
	}

	public <K> K search(ESSearchSourceBuilder builder, Class<K> resultsClass, boolean refresh) {
		return search(null, builder, resultsClass, refresh);
	}

	public <K> K search(String type, ESSearchSourceBuilder builder, //
			Class<K> resultsClass) {
		return search(type, builder, resultsClass, false);
	}

	public <K> K search(String type, ESSearchSourceBuilder builder, //
			Class<K> resultsClass, boolean refresh) {

		String path = "/1/search";
		if (!Strings.isNullOrEmpty(type))
			path = path + "/" + type;

		return dog.post(path)//
				.queryParam(REFRESH_PARAM, Boolean.toString(refresh))//
				.bodyJson(builder.toString()).go(200).toPojo(resultsClass);
	}
}
