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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
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
		Check.notNullOrEmpty(jsonString, "jsonString");
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

	// TODO add tests
	public static ObjectNode object(Object... elements) {
		if (elements.length % 2 != 0)
			throw Exceptions.illegalArgument("odd number of elements");

		ObjectNode object = mapper().getNodeFactory().objectNode();

		for (int i = 0; i < elements.length; i = i + 2)
			object.set(elements[i].toString(), toNode(elements[i + 1]));

		return object;
	}

	public static ArrayNode array(Object... elements) {
		ArrayNode array = mapper().getNodeFactory().arrayNode();
		for (int i = 0; i < elements.length; i++)
			array.add(toNode(elements[i]));
		return array;
	}

	public static ObjectNode set(ObjectNode object, String key, Object value) {
		object.set(key, toNode(value));
		return object;
	}

	private static ObjectMapper jsonMapper;

	static {
		jsonMapper = new ObjectMapper()//
				.setDefaultPrettyPrinter(new DefaultPrettyPrinter()//
						.withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE))//
				.registerModule(new JodaModule())//
				.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
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
		if (value == null)
			return NullNode.instance;
		if (value instanceof JsonNode)
			return (JsonNode) value;
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
		else if (value instanceof String)
			return TextNode.valueOf((String) value);
		else if (value.getClass().isArray())
			return toArrayNode(value);
		else if (value instanceof Collection<?>)
			return toCollectionNode((Collection<?>) value);

		throw Exceptions.illegalArgument("invalid value type [%s]", //
				value.getClass().getSimpleName());
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

	public static JsonNode toJson(Throwable t, boolean withTraces) {
		ObjectNode json = object("type", t.getClass().getName());

		if (!Strings.isNullOrEmpty(t.getMessage()))//
			json.put("message", t.getMessage());

		if (withTraces) {
			ArrayNode array = json.putArray("trace");
			for (StackTraceElement element : t.getStackTrace()) {
				array.add(element.toString());
			}
		}

		if (t.getCause() != null)
			json.set("cause", toJson(t.getCause(), withTraces));

		return json;
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
		return checkJsonNodeOfType(input, propertyPath, Type.Object, required);
	}

	public static ArrayNode checkArray(JsonNode node) {
		if (!node.isArray())
			throw Exceptions.illegalArgument(//
					"not a json array but [%s]", node.getNodeType());
		return (ArrayNode) node;
	}

	public static Optional<JsonNode> checkArray(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, Type.Array, required);
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

	public static void checkPresent(ObjectNode jsonBody, String propertyPath) {
		if (Json.isNull(get(jsonBody, propertyPath)))
			throw Exceptions.illegalArgument(//
					"JSON object does not contain this property [%s]", propertyPath);
	}

	public static void checkNotPresent(JsonNode input, String propertyPath, String type) {
		JsonNode node = get(input, propertyPath);
		if (node != null)
			throw Exceptions.illegalArgument(//
					"property [%s] is forbidden in type [%s]", propertyPath, type);
	}

	public static String checkString(JsonNode node) {
		if (!node.isTextual())
			throw Exceptions.runtime("json node [%s] should be a string", node);
		return node.asText();
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
		return checkJsonNodeOfType(input, propertyPath, Type.String, required);
	}

	public static Optional<Double> checkDouble(JsonNode push, String path) {
		return checkDoubleNode(push, path, false).flatMap(node -> Optional.of(node.asDouble()));
	}

	public static Optional<JsonNode> checkDoubleNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, Type.Double, required);
	}

	public static boolean checkBoolean(JsonNode input, String path, boolean defaultValue) {
		return checkBoolean(input, path).orElse(defaultValue);
	}

	public static Optional<Boolean> checkBoolean(JsonNode input, String path) {
		return checkBooleanNode(input, path, false).flatMap(node -> Optional.of(node.asBoolean()));
	}

	public static Optional<JsonNode> checkBooleanNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, Type.Boolean, required);
	}

	public static Optional<Integer> checkInteger(JsonNode input, String path) {
		return checkIntegerNode(input, path, false).flatMap(node -> Optional.of(node.asInt()));
	}

	public static Optional<JsonNode> checkIntegerNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, Type.Integer, required);
	}

	public static Optional<Long> checkLong(JsonNode input, String path) {
		return checkLongNode(input, path, false).flatMap(node -> Optional.of(node.asLong()));
	}

	public static Optional<JsonNode> checkLongNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, Type.Long, required);
	}

	private static Optional<JsonNode> checkJsonNodeOfType(JsonNode input, String propertyPath, Type expected,
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
}
