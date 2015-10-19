/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.LinkedList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonBuilder<N extends JsonNode> {

	private LinkedList<JsonNode> stack = new LinkedList<JsonNode>();

	private ObjectNode checkCurrentIsObjectNode() {
		JsonNode current = stack.getLast();
		if (!current.isObject())
			throw new IllegalStateException(
					String.format("current node not an object but [%s]", current.getNodeType()));
		return (ObjectNode) current;
	}

	private ArrayNode checkCurrentIsArrayNode() {
		JsonNode current = stack.getLast();
		if (!current.isArray())
			throw new IllegalStateException(String.format("current node not an array but [%s]", current.getNodeType()));
		return (ArrayNode) current;
	}

	//
	// object methods
	//

	public JsonBuilder<N> put(String key, String value) {
		checkCurrentIsObjectNode().put(key, value);
		return this;
	}

	public JsonBuilder<N> put(String key, boolean value) {
		checkCurrentIsObjectNode().put(key, value);
		return this;
	}

	public JsonBuilder<N> put(String key, int value) {
		checkCurrentIsObjectNode().put(key, value);
		return this;
	}

	public JsonBuilder<N> put(String key, long value) {
		checkCurrentIsObjectNode().put(key, value);
		return this;
	}

	public JsonBuilder<N> put(String key, double value) {
		checkCurrentIsObjectNode().put(key, value);
		return this;
	}

	public JsonBuilder<N> put(String key, float value) {
		checkCurrentIsObjectNode().put(key, value);
		return this;
	}

	public JsonBuilder<N> putNode(String key, JsonNode value) {
		checkCurrentIsObjectNode().set(key, value);
		return this;
	}

	public JsonBuilder<N> putNode(String key, String jsonText) {
		try {
			checkCurrentIsObjectNode().set(key, Json.getMapper().readTree(jsonText));
			return this;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public JsonBuilder<N> startObject(String key) {
		stack.add(checkCurrentIsObjectNode().putObject(key));
		return this;
	}

	public JsonBuilder<N> startArray(String key) {
		stack.add(checkCurrentIsObjectNode().putArray(key));
		return this;
	}

	//
	// array methods
	//

	public JsonBuilder<N> add(String value) {
		checkCurrentIsArrayNode().add(value);
		return this;
	}

	public JsonBuilder<N> add(boolean value) {
		checkCurrentIsArrayNode().add(value);
		return this;
	}

	public JsonBuilder<N> add(int value) {
		checkCurrentIsArrayNode().add(value);
		return this;
	}

	public JsonBuilder<N> add(long value) {
		checkCurrentIsArrayNode().add(value);
		return this;
	}

	public JsonBuilder<N> add(double value) {
		checkCurrentIsArrayNode().add(value);
		return this;
	}

	public JsonBuilder<N> add(float value) {
		checkCurrentIsArrayNode().add(value);
		return this;
	}

	public JsonBuilder<N> addNode(JsonNode value) {
		checkCurrentIsArrayNode().add(value);
		return this;
	}

	public JsonBuilder<N> addNode(String jsonText) {
		try {
			checkCurrentIsArrayNode().add(Json.getMapper().readTree(jsonText));
			return this;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public JsonBuilder<N> startObject() {
		stack.add( //
				stack.isEmpty() ? //
						Json.getMapper().getNodeFactory().objectNode() //
						: checkCurrentIsArrayNode().addObject());
		return this;
	}

	public JsonBuilder<N> startArray() {
		stack.add( //
				stack.isEmpty() ? //
						Json.getMapper().getNodeFactory().arrayNode() //
						: checkCurrentIsArrayNode().addArray());
		return this;
	}

	public <T extends Object> JsonBuilder<N> addAll(Iterable<T> values) {
		for (Object value : values)
			addGenericToArray(value);
		return this;
	}

	private void addGenericToArray(Object value) {
		if (value instanceof Integer)
			add((Integer) value);
		else if (value instanceof Long)
			add((Long) value);
		else if (value instanceof Float)
			add((Float) value);
		else if (value instanceof Double)
			add((Double) value);
		else if (value instanceof String)
			add((String) value);
		else if (value instanceof Boolean)
			add((Boolean) value);
		else if (value instanceof JsonNode)
			addNode((JsonNode) value);
		else
			throw new IllegalArgumentException(
					String.format("invalif array value type [%s]", value.getClass().getSimpleName()));
	}

	//
	// other methods
	//

	public JsonBuilder<N> end() {
		if (stack.size() > 1)
			stack.removeLast();
		return this;
	}

	@SuppressWarnings("unchecked")
	public N build() {
		return (N) stack.getFirst();
	}

	@Override
	public String toString() {
		return build().toString();
	}
}
