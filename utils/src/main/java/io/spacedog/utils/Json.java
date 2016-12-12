/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fasterxml.jackson.datatype.joda.JodaModule;
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

			else
				return null;
		}

		return current;
	}

	public static Object get(JsonNode json, String propertyPath, Object defaultValue) {
		JsonNode node = get(json, propertyPath);
		if (Json.isNull(node))
			return defaultValue;
		return Json.toValue(node);
	}

	public static JsonNode set(JsonNode object, String propertyPath, Object value) {
		JsonNode node = toNode(value);

		int lastDotIndex = propertyPath.lastIndexOf('.');
		String lastPathName = propertyPath.substring(lastDotIndex + 1);
		if (lastDotIndex > -1) {
			String parentPath = propertyPath.substring(0, lastDotIndex);
			object = Json.get(object, parentPath);
		}

		if (object.isObject())
			((ObjectNode) object).set(lastPathName, node);
		else if (object.isArray())
			((ArrayNode) object).set(Integer.parseInt(lastPathName), node);
		else
			throw Exceptions.illegalArgument(//
					"json node does not contain path [%s]", propertyPath);

		return object;
	}

	public static void remove(JsonNode object, String propertyPath) {

		int lastDotIndex = propertyPath.lastIndexOf('.');
		String lastPathName = propertyPath.substring(lastDotIndex + 1);
		if (lastDotIndex > -1) {
			String parentPath = propertyPath.substring(0, lastDotIndex);
			object = Json.get(object, parentPath);
		}

		if (object.isObject())
			((ObjectNode) object).remove(lastPathName);
		else if (object.isArray())
			((ArrayNode) object).remove(Integer.parseInt(lastPathName));
		else
			throw Exceptions.illegalArgument(//
					"json node does not contain path [%s]", propertyPath);
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
			return merged == null ? object() : merged;
		}

	}

	public static boolean isJson(String string) {

		if (Strings.isNullOrEmpty(string))
			return false;

		switch (string.charAt(0)) {
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

	public static boolean isObject(String string) {
		if (Strings.isNullOrEmpty(string))
			return false;
		string = string.trim();
		return string.charAt(0) == '{'//
				&& string.charAt(string.length() - 1) == '}';
	}

	public static boolean isNull(JsonNode value) {
		return value == null || value.isNull();
	}

	public static ObjectMapper mapper() {
		return Json.jsonMapper;
	}

	public static String toPrettyString(JsonNode node) {
		try {
			return mapper().writerWithDefaultPrettyPrinter()//
					.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e);
		}
	}

	public static JsonNode readNode(String jsonString) {
		Check.notNullOrEmpty(jsonString, "JSON");
		try {
			return jsonMapper.readTree(jsonString);
		} catch (IOException e) {
			throw Exceptions.illegalArgument(e);
		}
	}

	public static JsonNode readNode(URL url) {
		Check.notNull(url, "url");
		try {
			return jsonMapper.readTree(url);
		} catch (IOException e) {
			throw Exceptions.illegalArgument(e);
		}
	}

	public static ObjectNode readObject(String jsonObject) {
		JsonNode object = readNode(jsonObject);
		if (!object.isObject())
			throw Exceptions.illegalArgument("not a json object but [%s]", object.getNodeType());
		return (ObjectNode) object;
	}

	public static ArrayNode readArray(String jsonArray) {
		JsonNode object = readNode(jsonArray);
		if (!object.isArray())
			throw Exceptions.illegalArgument("not a json array but [%s]", object.getNodeType());
		return (ArrayNode) object;
	}

	public static JsonBuilder<ObjectNode> objectBuilder() {
		return new JsonBuilder<ObjectNode>().object();
	}

	public static JsonBuilder<ArrayNode> arrayBuilder() {
		return new JsonBuilder<ArrayNode>().array();
	}

	public static ObjectNode object() {
		return mapper().getNodeFactory().objectNode();
	}

	// TODO add tests
	public static ObjectNode object(Object... elements) {
		if (elements.length % 2 != 0)
			throw Exceptions.illegalArgument("odd number of elements");

		ObjectNode object = object();

		for (int i = 0; i < elements.length; i = i + 2)
			object.set(elements[i].toString(), toNode(elements[i + 1]));

		return object;
	}

	public static ArrayNode array() {
		return mapper().getNodeFactory().arrayNode();
	}

	public static ArrayNode array(Object... elements) {
		ArrayNode array = array();
		for (int i = 0; i < elements.length; i++)
			array.add(toNode(elements[i]));
		return array;
	}

	private static ObjectMapper jsonMapper;

	static {
		jsonMapper = new ObjectMapper()//
				.setDefaultPrettyPrinter(new DefaultPrettyPrinter()//
						.withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE))//
				.registerModule(new JodaModule())//
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)//
				.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}

	public static Object toValue(JsonNode value) {

		if (value.isBoolean())
			return value.booleanValue();

		if (value.isTextual())
			return value.textValue();

		if (value.isNumber())
			return value.numberValue();

		if (value.isArray())
			return toArray((ArrayNode) value);

		if (value.isNull())
			return null;

		throw Exceptions.illegalArgument("only supports simple types");
	}

	public static Object[] toArray(ArrayNode arrayNode) {
		Object[] array = new Object[arrayNode.size()];
		for (int i = 0; i < array.length; i++)
			array[i] = toValue(arrayNode.get(i));
		return array;
	}

	public static JsonNode toNode(Object value) {
		return mapper().valueToTree(value);
	}

	public static ValueNode toValueNode(Object value) {

		if (value == null)
			return NullNode.instance;
		if (value instanceof ValueNode)
			return (ValueNode) value;
		if (value instanceof Boolean)
			return BooleanNode.valueOf((boolean) value);
		else if (value instanceof Integer)
			return IntNode.valueOf((int) value);
		else if (value instanceof Long)
			return LongNode.valueOf((long) value);
		else if (value instanceof Double)
			return DoubleNode.valueOf((double) value);
		else if (value instanceof Float)
			return FloatNode.valueOf((float) value);

		return TextNode.valueOf(value.toString());
	}

	public static ArrayNode toArrayNode(Object array) {
		ArrayNode arrayNode = Json.array();
		for (int i = 0; i < Array.getLength(array); i++)
			arrayNode.add(toNode(Array.get(array, i)));
		return arrayNode;
	}

	public static ArrayNode toCollectionNode(Collection<?> collection) {
		ArrayNode arrayNode = Json.array();
		for (Object element : collection)
			arrayNode.add(toNode(element));
		return arrayNode;
	}

	public enum JsonType {
		String, Boolean, Integer, Long, Float, Double, Object, Array
	};

	public static boolean isOfType(JsonType expected, JsonNode node) {
		switch (expected) {
		case String:
			return node.isTextual();
		case Boolean:
			return node.isBoolean();
		case Integer:
			return node.isInt();
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

	public static <T> boolean isOfType(Class<T> expected, JsonNode node) {

		switch (expected.getSimpleName()) {
		case "String":
			return node.isTextual();
		case "Boolean":
			return node.isBoolean();
		case "Long":
			return node.isLong();
		case "Float":
			return node.isFloat();
		case "Double":
			return node.isDouble();
		case "Object":
			return node.isObject();
		case "List":
			return node.isArray();
		default:
			return false;
		}
	}

	public static List<String> toStrings(JsonNode node) {
		if (node.isArray())
			return Lists.newArrayList(node.elements())//
					.stream().map(element -> element.asText())//
					.collect(Collectors.toList());

		if (node.isObject())
			return Lists.newArrayList(node.elements())//
					.stream().map(element -> element.asText())//
					.collect(Collectors.toList());

		if (node.isValueNode())
			return Collections.singletonList(node.asText());

		throw Exceptions.illegalArgument(//
				"can not convert this json node [%s] to a list of strings", node);
	}

	public static JsonNode fullReplaceTextualFields(JsonNode node, String fieldName, String value) {
		List<JsonNode> parents = node.findParents(fieldName);
		for (JsonNode parent : parents) {
			if (parent.get(fieldName).isTextual())
				((ObjectNode) parent).set(fieldName, TextNode.valueOf(value));
		}
		return node;
	}

	public static JsonNode fullRemove(JsonNode node, String fieldName) {
		List<JsonNode> parents = node.findParents(fieldName);
		for (JsonNode parent : parents)
			((ObjectNode) parent).remove(fieldName);
		return node;
	}
	//
	// check methods
	//

	public static ObjectNode checkObject(JsonNode node) {
		if (!node.isObject())
			throw Exceptions.illegalArgument(//
					"not a json object but [%s]", node.getNodeType());
		return (ObjectNode) node;
	}

	public static Optional<JsonNode> checkObject(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.Object, required);
	}

	public static ArrayNode checkArray(JsonNode node) {
		if (!node.isArray())
			throw Exceptions.illegalArgument(//
					"not a json array but [%s]", node.getNodeType());
		return (ArrayNode) node;
	}

	public static Optional<JsonNode> checkArray(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.Array, required);
	}

	public static Optional<JsonNode> checkNode(JsonNode input, String propertyPath, boolean required) {
		JsonNode node = get(input, propertyPath);
		if (node == null) {
			if (required)
				throw Exceptions.illegalArgument("property [%s] is missing", propertyPath);
			return Optional.ofNullable(null);
		}
		return Optional.of(node);
	}

	public static JsonNode checkNotNull(ObjectNode jsonBody, String propertyPath) {
		JsonNode value = get(jsonBody, propertyPath);
		if (Json.isNull(value))
			throw Exceptions.illegalArgument("field [%s] is null", propertyPath);
		return value;
	}

	public static void checkNull(JsonNode input, String propertyPath) {
		if (get(input, propertyPath) != null)
			throw Exceptions.illegalArgument("field [%s] is forbidden", propertyPath);
	}

	public static String checkStringNotNullOrEmpty(JsonNode input, String propertyPath) {
		String string = checkStringNode(input, propertyPath, true).get().asText();
		if (Strings.isNullOrEmpty(string))
			throw Exceptions.illegalArgument("property [%s] is missing", propertyPath);

		return string;
	}

	public static Optional<String> checkString(JsonNode input, String path) {
		return checkStringNode(input, path, false).flatMap(node -> Optional.of(node.asText()));
	}

	public static Optional<JsonNode> checkStringNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.String, required);
	}

	public static Optional<Double> checkDouble(JsonNode push, String path) {
		return checkDoubleNode(push, path, false).flatMap(node -> Optional.of(node.asDouble()));
	}

	public static Optional<JsonNode> checkDoubleNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.Double, required);
	}

	public static boolean checkBoolean(JsonNode input, String path, boolean defaultValue) {
		return checkBoolean(input, path).orElse(defaultValue);
	}

	public static Optional<Boolean> checkBoolean(JsonNode input, String path) {
		return checkBooleanNode(input, path, false).flatMap(node -> Optional.of(node.asBoolean()));
	}

	public static Optional<JsonNode> checkBooleanNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.Boolean, required);
	}

	public static Optional<Integer> checkInteger(JsonNode input, String path) {
		return checkIntegerNode(input, path, false).flatMap(node -> Optional.of(node.asInt()));
	}

	public static Optional<JsonNode> checkIntegerNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.Integer, required);
	}

	public static Optional<Long> checkLong(JsonNode input, String path) {
		return checkLongNode(input, path, false).flatMap(node -> Optional.of(node.asLong()));
	}

	public static Optional<JsonNode> checkLongNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.Long, required);
	}

	private static Optional<JsonNode> checkJsonNodeOfType(JsonNode input, String propertyPath, JsonType expected,
			boolean required) {
		JsonNode node = get(input, propertyPath);
		if (node == null) {
			if (required)
				throw Exceptions.illegalArgument("property [%s] is missing", propertyPath);
			return Optional.empty();
		}
		if (isOfType(expected, node))
			return Optional.of(node);
		else
			throw Exceptions.illegalArgument(//
					"property [%s] must be of type [%s] instead of [%s]", //
					propertyPath, expected, node.getNodeType());
	}

	//
	// JsonNode check helpers
	//

	public static String checkString(JsonNode node) {
		if (node == null)
			return null;
		if (!node.isTextual())
			throw Exceptions.illegalArgument("json node [%s] not a string", node);
		return node.asText();
	}

	public static String checkBoolean(JsonNode node) {
		if (node == null)
			return null;
		if (!node.isBoolean())
			throw Exceptions.illegalArgument("json node [%s] not a boolean", node);
		return node.asText();
	}

	public static Double checkDouble(JsonNode node) {
		if (node == null)
			return null;
		if (!node.isDouble())
			throw Exceptions.illegalArgument("json node [%s] not a double", node);
		return node.asDouble();
	}

	public static JsonNode checkNotNull(JsonNode node) {
		if (node == null || node.isNull())
			throw Exceptions.illegalArgument("json node is null");
		return node;
	}

	private static <T> T checkType(JsonNode node, Class<T> type) {
		if (node == null)
			return null;
		if (!isOfType(type, node))
			throw Exceptions.illegalArgument("json node [%s] not a [%s]", node, type.getSimpleName());
		try {
			return mapper().treeToValue(node, type);
		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e);
		}
	}

}
