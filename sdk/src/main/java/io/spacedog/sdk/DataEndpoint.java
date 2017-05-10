package io.spacedog.sdk;

import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.sdk.elasticsearch.SearchSourceBuilder;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json7;
import io.spacedog.utils.JsonBuilder;
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

	public long save(String type, String id, Object object) {
		return save(type, id, object, true);
	}

	public long save(String type, String id, Object object, boolean strict) {
		return dog.put("/1/data/{type}/{id}").routeParam("id", id)//
				.routeParam("type", type).queryParam("strict", String.valueOf(strict))//
				.bodyPojo(object).go(200, 201).get("version").asLong();
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
		dog.delete("/1/data/{type}/{id}")//
				.routeParam("type", type)//
				.routeParam("id", id)//
				.go(200);
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
				request.queryParam(PARAM_REFRESH, Boolean.toString(refresh));
			if (from != null)
				request.queryParam(PARAM_FROM, Integer.toString(from));
			if (size != null)
				request.queryParam(PARAM_SIZE, Integer.toString(size));

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

	public <K> SearchResults<K> search(SearchSourceBuilder builder, Class<K> dataClass) {
		return search(null, builder, dataClass, false);
	}

	public <K> SearchResults<K> search(String type, SearchSourceBuilder builder, Class<K> dataClass) {
		return search(type, builder, dataClass);
	}

	public <K> SearchResults<K> search(String type, SearchSourceBuilder builder, Class<K> dataClass, boolean refresh) {

		String path = "/1/search";
		if (Strings.isNullOrEmpty(type))
			path = path + "/" + type;

		ObjectNode results = dog.post(path)//
				.queryParam(PARAM_REFRESH, Boolean.toString(refresh))//
				.bodyJson(builder.toString()).go(200).asJsonObject();

		return new SearchResults<>(results, dataClass);
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
	// Old search methods
	//

	@Deprecated
	public static class SimpleQuery {
		public int from = 0;
		public int size = 10;
		public boolean refresh = false;
		public String query;
		public String type;
	}

	@Deprecated
	public <K> List<K> search(SimpleQuery query, Class<K> dataClass) {

		ObjectNode result = SpaceRequest.get("/1/search/{type}")//
				.auth(dog)//
				.routeParam("type", query.type)//
				.queryParam(PARAM_REFRESH, Boolean.toString(query.refresh))//
				.queryParam(PARAM_FROM, Integer.toString(query.from))//
				.queryParam(PARAM_SIZE, Integer.toString(query.size))//
				.queryParam(PARAM_Q, query.query)//
				.go(200).asJsonObject();

		return toList(result, dataClass);
	}

	@Deprecated
	public static class ComplexQuery {
		public boolean refresh = false;
		public ObjectNode query;
		public String type;
	}

	@Deprecated
	public <K> SearchResults<K> search(ComplexQuery query, Class<K> dataClass) {

		ObjectNode results = dog.post("/1/search/{type}")//
				.routeParam("type", query.type)//
				.queryParam(PARAM_REFRESH, Boolean.toString(query.refresh))//
				.bodyJson(query.query).go(200).asJsonObject();

		return new SearchResults<>(results, dataClass);
	}

	@Deprecated
	public static class TermQuery {
		public int from = 0;
		public int size = 10;
		public boolean refresh = false;
		public List<Object> terms;
		public String type;
		public String sort;
		public boolean ascendant = true;
	}

	@Deprecated
	public <K> SearchResults<K> search(TermQuery query, Class<K> dataClass) {

		JsonBuilder<ObjectNode> builder = Json7.objectBuilder()//
				.put("size", query.size)//
				.put("from", query.from)//
				.object("query")//
				.object("bool")//
				.array("filter");

		for (int i = 0; i < query.terms.size(); i = i + 2) {
			Object field = query.terms.get(i);
			Object value = query.terms.get(i + 1);

			if (value instanceof String)
				builder.object().object("term");
			else if (value instanceof List)
				builder.object().object("terms");
			else
				throw Exceptions.illegalArgument("term value [%s] is invalid", value);

			builder.node(field.toString(), Json7.toNode(value))//
					.end().end();
		}

		builder.end().end().end();

		if (query.sort != null)
			builder.array("sort")//
					.object()//
					.object(query.sort)//
					.put("order", query.ascendant ? "asc" : "desc")//
					.end()//
					.end()//
					.end();

		ComplexQuery complex = new ComplexQuery();
		complex.refresh = query.refresh;
		complex.type = query.type;
		complex.query = builder.build();

		return search(complex, dataClass);
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
				K object = Json7.mapper().treeToValue(node, dataClass);
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
