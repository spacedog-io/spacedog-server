/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.LinkedList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonBuilder<N extends JsonNode> {

	private LinkedList<JsonNode> stack = new LinkedList<JsonNode>();

	public JsonBuilder<N> add(Object... values) {
		JsonNode current = stack.getLast();
		if (current.isArray())
			Json.addAll((ArrayNode) current, values);
		if (current.isObject())
			Json.addAll((ObjectNode) current, values);
		return this;
	}

	//
	// object methods
	//

	public JsonBuilder<N> object(String key) {
		stack.add(checkCurrentIsObject().putObject(key));
		return this;
	}

	public JsonBuilder<N> array(String key) {
		stack.add(checkCurrentIsObject().putArray(key));
		return this;
	}

	//
	// array methods
	//

	public JsonBuilder<N> object() {
		stack.add(stack.isEmpty() ? Json.object() //
				: checkCurrentIsArray().addObject());
		return this;
	}

	public JsonBuilder<N> array() {
		stack.add(stack.isEmpty() ? Json.array() //
				: checkCurrentIsArray().addArray());
		return this;
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

	//
	// implem methods
	//

	private ObjectNode checkCurrentIsObject() {
		JsonNode current = stack.getLast();
		if (!current.isObject())
			throw Exceptions.illegalState("[%s] not an object", current);
		return (ObjectNode) current;
	}

	private ArrayNode checkCurrentIsArray() {
		JsonNode current = stack.getLast();
		if (!current.isArray())
			throw Exceptions.illegalState("[%s] not an array", current);
		return (ArrayNode) current;
	}
}
