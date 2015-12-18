/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class Json {

	/**
	 * TODO returns null if does not find the property in this object. Should
	 * return an optional.
	 */
	public static JsonNode get(JsonNode json, String propertyPath) {

		JsonNode current = json;

		for (String s : Utils.splitByDot(propertyPath)) {
			if (current == null)
				return null;

			if (current.isObject())
				current = current.get(s);

			else if (current.isArray())
				current = current.get(Integer.parseInt(s));
		}

		return current;
	}

	public static void set(JsonNode object, String propertyPath, JsonNode value) {

		int lastDotIndex = propertyPath.lastIndexOf('.');
		String lastPathName = propertyPath.substring(lastDotIndex + 1);
		if (lastDotIndex > -1) {
			String parentPath = propertyPath.substring(0, lastDotIndex);
			object = Json.get(object, parentPath);
		}

		if (object.isObject())
			((ObjectNode) object).set(lastPathName, value);
		else if (object.isArray())
			((ArrayNode) object).set(Integer.parseInt(lastPathName), value);
		else
			throw new IllegalArgumentException(String.format("json node does not contain path [%s]", propertyPath));
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
			throw new IllegalArgumentException(String.format("not a json object but [%s]", object.getNodeType()));
		return (ObjectNode) object;
	}

	public static ArrayNode readArrayNode(String jsonArray) throws JsonProcessingException, IOException {
		JsonNode object = jsonMapper.readTree(jsonArray);
		if (!object.isArray())
			throw new IllegalArgumentException(String.format("not a json array but [%s]", object.getNodeType()));
		return (ArrayNode) object;
	}

	public static ObjectNode newObjectNode() {
		return getMapper().getNodeFactory().objectNode();
	}

	public static ArrayNode newArrayNode() {
		return getMapper().getNodeFactory().arrayNode();
	}

	public static JsonBuilder<ObjectNode> objectBuilder() {
		return new JsonBuilder<ObjectNode>().object();
	}

	public static JsonBuilder<ArrayNode> arrayBuilder() {
		return new JsonBuilder<ArrayNode>().array();
	}

	public static ObjectMapper jsonMapper = new ObjectMapper().setDefaultPrettyPrinter(
			new DefaultPrettyPrinter().withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE));

	public static Object toSimpleValue(JsonNode value) {

		if (value.isBoolean())
			return value.booleanValue();

		if (value.isTextual())
			return value.textValue();

		if (value.isNumber())
			return value.numberValue();

		if (value.isNull())
			return null;

		throw new RuntimeException("only supports simple types");
	}

	public static JsonNode toJson(Throwable t) {
		JsonBuilder<ObjectNode> builder = objectBuilder()//
				.put("type", t.getClass().getName()) //
				.put("message", t.getMessage()) //
				.array("trace");

		for (StackTraceElement element : t.getStackTrace()) {
			builder.add(element.toString());
		}

		builder.end();

		if (t.getCause() != null) {
			builder.node("cause", toJson(t.getCause()));
		}

		return builder.build();
	}

	public enum Type {
		String, Boolean, Integer, Long, Float, Double, Object, Array
	};

	public static boolean isOfType(Type expected, JsonNode node) {
		switch (expected) {
		case String:
			return node.isTextual();
		case Boolean:
			return node.isBoolean();
		case Long:
			return node.isLong();
		case Float:
			return node.isFloat();
		case Double:
			return node.isDouble();
		case Object:
			return node.isObject();
		case Array:
			return node.isArray();
		default:
			return false;
		}
	}

	public static ObjectNode checkObject(JsonNode node) {
		if (!node.isObject())
			throw new IllegalArgumentException(String.format("not a json object but [%s]", node.getNodeType()));
		return (ObjectNode) node;
	}

	public static List<String> toList(JsonNode node) {
		if (node.isArray())
			return Lists.newArrayList(node.elements()).stream().map(element -> element.asText())
					.collect(Collectors.toList());

		if (node.isValueNode())
			return Collections.singletonList(node.asText());

		throw new IllegalArgumentException(
				String.format("can not convert this json node [%s] to a list of strings", node));
	}
}
