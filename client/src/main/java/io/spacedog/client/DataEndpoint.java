package io.spacedog.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceResponse;
import io.spacedog.model.BasicDataObject;
import io.spacedog.model.DataObject;
import io.spacedog.model.JsonDataObject;
import io.spacedog.model.JsonDataObject.Results;
import io.spacedog.utils.Json;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;
import io.spacedog.utils.Utils;

public class DataEndpoint implements SpaceFields, SpaceParams {

	SpaceDog dog;

	DataEndpoint(SpaceDog session) {
		this.dog = session;
	}

	//
	// Get
	//

	public JsonDataObject get(String type, String id) {
		return get(type, id, JsonDataObject.class);
	}

	public <K> K get(String type, String id, Class<K> pojoClass) {
		return fetch(type, id, Utils.instantiate(pojoClass));
	}

	public <K> DataObject<K> fetch(DataObject<K> object) {
		return fetch(object.type(), object.id(), object);
	}

	public <K> K fetch(String type, String id, K object) {
		return dog.get("/1/data/{type}/{id}")//
				.routeParam(TYPE_FIELD, type)//
				.routeParam(ID_FIELD, id)//
				.go(200)//
				.toPojo(object);
	}

	//
	// Save
	//

	public JsonDataObject save(String type, ObjectNode source) {
		return save(type, null, source);
	}

	public JsonDataObject save(String type, String id, ObjectNode source) {
		return save(type, id, source, 0);
	}

	public JsonDataObject save(String type, String id, ObjectNode source, long version) {
		return (JsonDataObject) save(new JsonDataObject()//
				.type(type).id(id).source(source).version(version));
	}

	public BasicDataObject save(String type, Object source) {
		return save(type, null, source);
	}

	public BasicDataObject save(String type, String id, Object source) {
		return save(type, id, source, 0);
	}

	public BasicDataObject save(String type, String id, Object source, long version) {
		return (BasicDataObject) save(new BasicDataObject()//
				.type(type).id(id).source(source).version(version));
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
			request.queryParam(VERSION_PARAM, object.version());

		return request.go(200, 201).toPojo(object);
	}

	//
	// Patch
	//

	public long patch(String type, String id, Object source) {
		return patch(type, id, source, 0);
	}

	public long patch(String type, String id, Object source, long version) {
		SpaceRequest request = dog.put("/1/data/{type}/{id}")//
				.routeParam(ID_FIELD, id)//
				.routeParam(TYPE_FIELD, type)//
				.queryParam(STRICT_PARAM, false);

		if (version > 0)
			request.queryParam(VERSION_PARAM, version);

		return request.bodyPojo(source).go(200)//
				.get(VERSION_FIELD).asLong();
	}

	//
	// Delete
	//

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
	// Field methods
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
	// Get All Request
	//

	public GetAllRequest getAllRequest() {
		return new GetAllRequest();
	}

	public class GetAllRequest {
		private String type;
		private Integer from;
		private Integer size;
		private boolean refresh;
		private String q;

		public GetAllRequest type(String type) {
			this.type = type;
			return this;
		}

		public GetAllRequest q(String q) {
			this.q = q;
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

		public JsonDataObject.Results go() {
			return go(JsonDataObject.Results.class);
		}

		public <K> K go(Class<K> resultsClass) {

			String path = "/1/data";
			if (!Strings.isNullOrEmpty(type))
				path = path + "/" + type;

			return dog.get(path)//
					.refresh(refresh)//
					.queryParam(Q_PARAM, q)//
					.queryParam(FROM_PARAM, from)//
					.queryParam(SIZE_PARAM, size)//
					.go(200)//
					.toPojo(resultsClass);
		}

	}

	//
	// Delete All Request
	//

	public long deleteAll(String type) {
		return deleteAllRequest().type(type).go();
	}

	public DeleteAllRequest deleteAllRequest() {
		return new DeleteAllRequest();
	}

	public class DeleteAllRequest {
		private boolean refresh;
		private String type;
		private String source;

		public DeleteAllRequest refresh() {
			this.refresh = true;
			return this;
		}

		public DeleteAllRequest type(String type) {
			this.type = type;
			return this;
		}

		public DeleteAllRequest source(String source) {
			this.source = source;
			return this;
		}

		public DeleteAllRequest source(ESSearchSourceBuilder source) {
			return source(source.toString());
		}

		public long go() {

			String path = "/1/search";
			if (!Strings.isNullOrEmpty(type))
				path = path + "/" + type;

			if (Strings.isNullOrEmpty(source))
				source = Json.EMPTY_OBJECT;

			return dog.delete(path).bodyJson(source)//
					.refresh(refresh).go(200).get("totalDeleted").asLong();

		}

	}

	//
	// Search Request
	//

	public SearchRequest searchRequest() {
		return new SearchRequest();
	}

	public class SearchRequest {
		public boolean refresh;
		public String type;
		public String source;

		public SearchRequest refresh() {
			this.refresh = true;
			return this;
		}

		public SearchRequest type(String type) {
			this.type = type;
			return this;
		}

		public SearchRequest source(String source) {
			this.source = source;
			return this;
		}

		public SearchRequest source(ESSearchSourceBuilder source) {
			return source(source.toString());
		}

		public Results go() {
			return go(Results.class);
		}

		public <K> K go(Class<K> resultsClass) {

			String path = "/1/search";
			if (!Strings.isNullOrEmpty(type))
				path = path + "/" + type;

			if (Strings.isNullOrEmpty(source))
				source = Json.EMPTY_OBJECT;

			return dog.post(path).bodyJson(source)//
					.refresh(refresh).go(200).toPojo(resultsClass);
		}

	}

}
