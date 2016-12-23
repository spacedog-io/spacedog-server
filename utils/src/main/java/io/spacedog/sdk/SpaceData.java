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

public class SpaceData {

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
				.bearerAuth(dog.backendId, dog.accessToken)//
				.routeParam("type", query.type)//
				.queryParam("refresh", Boolean.toString(query.refresh))//
				.queryParam("from", Integer.toString(query.from))//
				.queryParam("size", Integer.toString(query.size))//
				.queryParam("q", query.query)//
				.go(200).objectNode();

		return toList((ArrayNode) result.get("results"), dataClass);
	}

	public static class ComplexQuery {
		public boolean refresh = false;
		public ObjectNode query;
		public String type;
	}

	public <K extends DataObject> SearchResults<K> search(ComplexQuery query, Class<K> dataClass) {

		ObjectNode results = SpaceRequest.post("/1/search/{type}")//
				.bearerAuth(dog.backendId, dog.accessToken)//
				.routeParam("type", query.type)//
				.queryParam("refresh", Boolean.toString(query.refresh))//
				.body(query.query).go(200).objectNode();

		return new SearchResults<K>(results, dataClass);
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

	private <K extends DataObject> List<K> toList(ArrayNode arrayNode, Class<K> dataClass) {
		List<K> results = Lists.newArrayList();
		Iterator<JsonNode> elements = arrayNode.elements();
		while (elements.hasNext()) {
			try {
				K object = Json.mapper().treeToValue(elements.next(), dataClass);
				object.dog = dog;
				results.add(object);
			} catch (JsonProcessingException e) {
				throw Exceptions.runtime(e);
			}
		}
		return results;
	}
}
