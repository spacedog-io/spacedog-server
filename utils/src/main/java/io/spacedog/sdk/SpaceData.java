package io.spacedog.sdk;

import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;

public class SpaceData implements SpaceFields, SpaceParams {

	SpaceDog dog;

	SpaceData(SpaceDog session) {
		this.dog = session;
	}

	public DataObject object(String type) {
		return new DataObject(dog, type);
	}

	public DataObject object(String type, String id) {
		return new DataObject(dog, type, id);
	}

	public <K extends DataObject> K object(Class<K> dataClass) {
		try {
			K object = dataClass.newInstance();
			object.dog = dog;
			return object;
		} catch (InstantiationException | IllegalAccessException e) {
			throw Exceptions.runtime(e);
		}
	}

	public <K extends DataObject> K object(Class<K> dataClass, String id) {
		K object = object(dataClass);
		object.id(id);
		return object;
	}

	public <K extends DataObject> K get(Class<K> dataClass, String id) {
		K object = object(dataClass, id);
		object.fetch();
		return object;
	}

	//
	// Load
	//

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

		public List<DataObject> get() {
			return load(DataObject.class);
		}

		public List<DataObject> load() {
			return load(DataObject.class);
		}

		// TODO how to return objects in the right pojo form?
		// add registerPojoType(String type, Class<K extends DataObject> clazz)
		// methods??
		// TODO rename this method with get
		public <K extends DataObject> List<K> load(Class<K> dataClass) {

			SpaceRequest request = type == null //
					? SpaceRequest.get("/1/data") //
					: SpaceRequest.get("/1/data/{type}").routeParam("type", type);

			if (refresh != null)
				request.queryParam(PARAM_REFRESH, Boolean.toString(refresh));
			if (from != null)
				request.queryParam(PARAM_FROM, Integer.toString(from));
			if (size != null)
				request.queryParam(PARAM_SIZE, Integer.toString(size));

			ObjectNode result = request.auth(dog).go(200).objectNode();

			return toList(result, dataClass);
		}

	}

	public GetAllRequest getAllRequest() {
		return new GetAllRequest();
	}

	//
	// Search
	//

	public static class SimpleQuery {
		public int from = 0;
		public int size = 10;
		public boolean refresh = false;
		public String query;
		public String type;
	}

	public <K extends DataObject> List<K> search(SimpleQuery query, Class<K> dataClass) {

		ObjectNode result = SpaceRequest.get("/1/search/{type}")//
				.auth(dog)//
				.routeParam("type", query.type)//
				.queryParam(PARAM_REFRESH, Boolean.toString(query.refresh))//
				.queryParam(PARAM_FROM, Integer.toString(query.from))//
				.queryParam(PARAM_SIZE, Integer.toString(query.size))//
				.queryParam(PARAM_Q, query.query)//
				.go(200).objectNode();

		return toList(result, dataClass);
	}

	public static class ComplexQuery {
		public boolean refresh = false;
		public ObjectNode query;
		public String type;
	}

	public <K extends DataObject> SearchResults<K> search(ComplexQuery query, Class<K> dataClass) {

		ObjectNode results = SpaceRequest.post("/1/search/{type}")//
				.auth(dog)//
				.routeParam("type", query.type)//
				.queryParam(PARAM_REFRESH, Boolean.toString(query.refresh))//
				.body(query.query).go(200).objectNode();

		return new SearchResults<>(results, dataClass);
	}

	public static class TermQuery {
		public int from = 0;
		public int size = 10;
		public boolean refresh = false;
		public List<Object> terms;
		public String type;
		public String sort;
		public boolean ascendant = true;
	}

	public class SearchResults<K extends DataObject> {

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

	public <K extends DataObject> SearchResults<K> search(TermQuery query, Class<K> dataClass) {

		JsonBuilder<ObjectNode> builder = Json.objectBuilder()//
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

			builder.node(field.toString(), Json.toNode(value))//
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

	private <K extends DataObject> List<K> toList(ObjectNode resultNode, Class<K> dataClass) {
		return toList((ArrayNode) resultNode.get("results"), dataClass);
	}

	private <K extends DataObject> List<K> toList(ArrayNode arrayNode, Class<K> dataClass) {
		List<K> results = Lists.newArrayList();
		Iterator<JsonNode> elements = arrayNode.elements();
		while (elements.hasNext()) {
			try {
				JsonNode node = elements.next();
				K object = Json.mapper().treeToValue(node, dataClass);
				object.dog = dog;
				object.node = (ObjectNode) node;
				results.add(object);
			} catch (JsonProcessingException e) {
				throw Exceptions.runtime(e);
			}
		}
		return results;
	}

}
