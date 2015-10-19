/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

public class Json {

	public static JsonNode get(JsonNode json, String path) {

		JsonNode current = json;

		for (String s : path.split("\\.")) {

			if (current.isObject())
				current = current.get(s);

			else if (current.isArray())
				current = current.get(Integer.parseInt(s));
		}

		return current;
	}

	public static JsonMerger merger() {
		return new JsonMerger();
	}

	public static class JsonMerger {

		private ObjectNode merged;

		public JsonMerger merge(ObjectNode objectNode) {
			if (merged == null)
				merged = objectNode;
			else
				objectNode.fields().forEachRemaining(entry -> merged.set(entry.getKey(), entry.getValue()));

			return this;
		}

		public ObjectNode get() {
			return merged == null ? newObjectNode() : merged;
		}

	}

	public static boolean isJson(String body) {

		if (Strings.isNullOrEmpty(body))
			return false;

		switch (body.charAt(0)) {
		case 'n':
		case 't':
		case 'f':
		case '"':
		case '[':
		case '{':
		case '-':
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
			return true;
		}
		return false;
	}

	public static boolean isNull(JsonNode value) {
		return value == null || value.isNull();
	}

	public static ObjectMapper getMapper() {
		return Json.jsonMapper;
	}

	public static JsonNode readJsonNode(String jsonString) throws JsonProcessingException, IOException {
		return jsonMapper.readTree(jsonString);
	}

	public static ObjectNode readObjectNode(String jsonObject) throws JsonProcessingException, IOException {
		JsonNode object = jsonMapper.readTree(jsonObject);
		if (!object.isObject())
			throw new RuntimeException(String.format("not a json object but [%s]", object.getNodeType()));
		return (ObjectNode) object;
	}

	public static ArrayNode readArrayNode(String jsonArray) throws JsonProcessingException, IOException {
		JsonNode object = jsonMapper.readTree(jsonArray);
		if (!object.isArray())
			throw new RuntimeException(String.format("not a json array but [%s]", object.getNodeType()));
		return (ArrayNode) object;
	}

	public static ObjectNode newObjectNode() {
		return getMapper().getNodeFactory().objectNode();
	}

	public static ArrayNode newArrayNode() {
		return getMapper().getNodeFactory().arrayNode();
	}

	public static JsonBuilder<ObjectNode> startObject() {
		return new JsonBuilder<ObjectNode>().startObject();
	}

	public static JsonBuilder<ArrayNode> startArray() {
		return new JsonBuilder<ArrayNode>().startArray();
	}

	public static ObjectMapper jsonMapper = new ObjectMapper();
}
