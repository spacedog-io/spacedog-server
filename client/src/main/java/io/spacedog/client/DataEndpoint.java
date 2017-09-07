package io.spacedog.client;

import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.http.SpaceRequest;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;

public class DataEndpoint implements SpaceFields, SpaceParams {

	SpaceDog dog;

	DataEndpoint(SpaceDog session) {
		this.dog = session;
	}

	//
	// new data object methods
	//

	public DataObject<?> object(String type) {
		return new DataObject<>(dog, type);
	}

	public DataObject<?> object(String type, String id) {
		return new DataObject<>(dog, type, id);
	}

	public <K extends DataObject<K>> K object(Class<K> dataClass) {
		try {
			K object = dataClass.newInstance();
			object.dog = dog;
			return object;
		} catch (InstantiationException | IllegalAccessException e) {
			throw Exceptions.runtime(e);
		}
	}

	public <K extends DataObject<K>> K object(Class<K> dataClass, String id) {
		K object = object(dataClass);
		object.id(id);
		return object;
	}

	//
	// Simple CRUD methods
	//

	public ObjectNode get(String type, String id) {
		return get(type, id, ObjectNode.class);
	}

	public <K extends DataObject<K>> K get(Class<K> dataClass, String id) {
		K object = object(dataClass, id);
		object.fetch();
		return object;
	}

	public <K> K get(String type, String id, Class<K> dataClass) {
		return dog.get("/1/data/{type}/{id}")//
				.routeParam("type", type)//
				.routeParam("id", id)//
				.go(200).toPojo(dataClass);
	}

	@SuppressWarnings("unchecked")
	public <K extends Datable<K>> K reload(K object) {
		return dog.get("/1/data/{type}/{id}")//
				.routeParam("type", object.type())//
				.routeParam("id", object.id())//
				.go(200).toPojo((Class<K>) object.getClass());
	}

	public String create(String type, Object object) {
		return dog.post("/1/data/{type}").routeParam("type", type)//
				.bodyPojo(object).go(201).getString("id");
	}

	public void create(String type, String id, Object object) {
		dog.put("/1/data/{type}/{id}").routeParam("type", type)//
				.routeParam("id", id).queryParam(STRICT_PARAM, "true")//
				.bodyPojo(object).go(201);
	}

	public long save(String type, String id, Object object) {
		return save(type, id, object, true);
	}

	public long save(String type, String id, Object object, boolean strict) {
		return dog.put("/1/data/{type}/{id}").routeParam("id", id)//
				.routeParam("type", type).queryParam("strict", String.valueOf(strict))//
				.bodyPojo(object).go(200, 201).get("version").asLong();
	}

	public long save(String type, String id, String field, Object object) {
		return dog.put("/1/data/{t}/{i}/{f}").routeParam("i", id)//
				.routeParam("t", type).routeParam("f", field)//
				.bodyPojo(object).go(200).get("version").asLong();
	}

	public <K extends Datable<K>> K save(K object) {
		SpaceRequest request = object.id() == null //
				? dog.post("/1/data/{type}")//
				: dog.put("/1/data/{type}/{id}").routeParam("id", object.id());

		ObjectNode result = request.routeParam("type", object.type())//
				.bodyPojo(object).go(200, 201).asJsonObject();

		object.id(result.get("id").asText());
		object.version(result.get("version").asLong());
		return object;
	}

	public void delete(String type, String id) {
		delete(type, id, true);
	}

	public void delete(String type, String id, boolean throwNotFound) {
		SpaceRequest request = dog.delete("/1/data/{type}/{id}")//
				.routeParam("type", type)//
				.routeParam("id", id);

		if (throwNotFound)
			request.go(200);
		else
			request.go(200, 404);
	}

	public void delete(Datable<?> object) {
		delete(object.type(), object.id());
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

		@SuppressWarnings("rawtypes")
		public List<DataObject> get() {
			return get(DataObject.class);
		}

		// TODO how to return objects in the right pojo form?
		// add registerPojoType(String type, Class<K extends DataObject> clazz)
		// methods??
		// TODO rename this method with get
		public <K> List<K> get(Class<K> dataClass) {

			SpaceRequest request = type == null //
					? dog.get("/1/data") //
					: dog.get("/1/data/{type}").routeParam("type", type);

			if (refresh != null)
				request.queryParam(REFRESH_PARAM, Boolean.toString(refresh));
			if (from != null)
				request.queryParam(FROM_PARAM, Integer.toString(from));
			if (size != null)
				request.queryParam(SIZE_PARAM, Integer.toString(size));

			ObjectNode result = request.go(200).asJsonObject();
			return toList(result, dataClass);
		}

	}

	public DataEndpoint deleteAll(String type) {
		dog.delete("/1/data/{type}").routeParam("type", type).go(200);
		return this;
	}

	//
	// Search
	//

	public <K> SearchResults<K> search(ESSearchSourceBuilder builder, Class<K> dataClass) {
		return search(null, builder, dataClass, false);
	}

	public <K> SearchResults<K> search(ESSearchSourceBuilder builder, Class<K> dataClass, boolean refresh) {
		return search(null, builder, dataClass, refresh);
	}

	public <K> SearchResults<K> search(String type, ESSearchSourceBuilder builder, Class<K> dataClass) {
		return search(type, builder, dataClass, false);
	}

	public <K> SearchResults<K> search(String type, ESSearchSourceBuilder builder, Class<K> dataClass,
			boolean refresh) {

		String path = "/1/search";
		if (!Strings.isNullOrEmpty(type))
			path = path + "/" + type;

		ObjectNode response = dog.post(path)//
				.queryParam(REFRESH_PARAM, Boolean.toString(refresh))//
				.bodyJson(builder.toString()).go(200).asJsonObject();

		return new SearchResults<>(response, dataClass);
	}

	public class SearchResults<K> {

		private long total;
		private List<K> objects;

		public SearchResults(ObjectNode results, Class<K> dataClass) {
			this.total = results.get("total").asLong();
			this.objects = toList((ArrayNode) results.get("results"), dataClass);
		}

		public long total() {
			return total;
		}

		public List<K> objects() {
			return objects;
		}
	}

	//
	// Implementation
	//

	private <K> List<K> toList(ObjectNode resultNode, Class<K> dataClass) {
		return toList((ArrayNode) resultNode.get("results"), dataClass);
	}

	private <K> List<K> toList(ArrayNode arrayNode, Class<K> dataClass) {
		List<K> results = Lists.newArrayList();
		Iterator<JsonNode> elements = arrayNode.elements();
		while (elements.hasNext()) {
			try {
				JsonNode node = elements.next();
				K object = Json.mapper().treeToValue(node, dataClass);
				results.add(object);
				if (object instanceof DataObject<?>)
					enhance((DataObject<?>) object, (ObjectNode) node);
			} catch (JsonProcessingException e) {
				throw Exceptions.runtime(e);
			}
		}
		return results;
	}

	private void enhance(DataObject<?> dataObject, ObjectNode node) {
		dataObject.session(dog);
		dataObject.node(node);
	}

}
