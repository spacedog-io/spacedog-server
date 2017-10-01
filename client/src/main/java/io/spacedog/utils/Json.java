/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
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

public class Json {

	public static final String JSON_CONTENT = "application/json";
	public static final String JSON_CONTENT_UTF8 = JSON_CONTENT + ";charset=UTF-8";

	//
	// Get, set, with, remove
	//

	/**
	 * TODO returns null if does not find the property in this object. Should return
	 * an optional.
	 */
	public static JsonNode get(JsonNode json, String fieldPath) {

		JsonNode current = json;

		for (String s : Utils.splitByDot(fieldPath)) {
			if (current == null)
				return null;

			if (current.isObject())
				current = current.get(s);

			else if (current.isArray())
				current = current.get(Integer.parseInt(s));

			else
				return null;
		}

		return isNull(current) ? null : current;
	}

	public static Object get(JsonNode json, String fieldPath, Object defaultValue) {
		JsonNode node = get(json, fieldPath);
		if (isNull(node))
			return defaultValue;
		return toValue(node);
	}

	public static JsonNode set(JsonNode json, String fieldPath, Object value) {
		JsonNode node = toNode(value);
		int lastDotIndex = fieldPath.lastIndexOf('.');
		String lastPathName = fieldPath.substring(lastDotIndex + 1);
		JsonNode parent = json;

		if (lastDotIndex > -1) {
			String parentPath = fieldPath.substring(0, lastDotIndex);
			parent = get(json, parentPath);
		}

		if (json.isObject())
			((ObjectNode) parent).set(lastPathName, node);
		else if (json.isArray())
			((ArrayNode) parent).set(Integer.parseInt(lastPathName), node);
		else
			throw Exceptions.invalidFieldPath(json, fieldPath);

		return json;
	}

	public static JsonNode with(JsonNode json, String fieldPath, Object value) {
		JsonNode current = json;
		Object[] segments = toStringAndIntegers(fieldPath);

		for (int i = 0; i < segments.length - 1; i++) {
			if (current.isArray()) {
				ArrayNode array = (ArrayNode) current;
				int index = (Integer) segments[i];
				if (array.has(index)) {
					current = array.get(index);
				} else {
					current = newContainerNode(segments[i + 1]);
					array.add(current);
				}
			} else if (current.isObject()) {
				ObjectNode object = (ObjectNode) current;
				String fieldName = (String) segments[i];
				current = object.get(fieldName);
				if (Json.isNull(current)) {
					current = newContainerNode(segments[i + 1]);
					object.set(fieldName, current);
				}
			} else
				throw Exceptions.invalidFieldPath(json, fieldPath);

		}

		JsonNode valueNode = toNode(value);
		Object lastSegment = segments[segments.length - 1];
		if (current.isArray()) {
			ArrayNode array = (ArrayNode) current;
			Integer index = (Integer) lastSegment;
			if (array.has(index)) {
				array.set(index, valueNode);
			} else
				array.add(valueNode);
		} else if (current.isObject()) {
			ObjectNode object = (ObjectNode) current;
			object.set((String) lastSegment, valueNode);
		} else
			throw Exceptions.invalidFieldPath(json, fieldPath);

		return json;
	}

	private static Object[] toStringAndIntegers(String fieldPath) {
		String[] strings = Utils.splitByDot(fieldPath);
		Object[] segments = new Object[strings.length];
		for (int i = 0; i < strings.length; i++) {
			try {
				segments[i] = Integer.valueOf(strings[i]);
			} catch (NumberFormatException e) {
				segments[i] = strings[i];
			}
		}
		return segments;
	}

	private static JsonNode newContainerNode(Object type) {
		return type instanceof Integer ? array() : object();
	}

	public static void remove(JsonNode json, String fieldPath) {

		int lastDotIndex = fieldPath.lastIndexOf('.');
		String lastPathName = fieldPath.substring(lastDotIndex + 1);
		JsonNode parent = json;

		if (lastDotIndex > -1) {
			String parentPath = fieldPath.substring(0, lastDotIndex);
			parent = get(json, parentPath);
		}

		if (parent.isObject())
			((ObjectNode) parent).remove(lastPathName);
		else if (parent.isArray())
			((ArrayNode) parent).remove(Integer.parseInt(lastPathName));
		else
			throw Exceptions.invalidFieldPath(json, fieldPath);
	}

	//
	// Merge
	//

	public static JsonMerger merger() {
		return new JsonMerger();
	}

	public static class JsonMerger {

		private ObjectNode merged;

		public JsonMerger merge(ObjectNode objectNode) {
			if (merged == null)
				merged = objectNode;
			else {
				// objectNode.fields().forEachRemaining(entry ->
				// merged.set(entry.getKey(), entry.getValue()));
				Iterator<Entry<String, JsonNode>> fields = objectNode.fields();
				while (fields.hasNext()) {
					Entry<String, JsonNode> field = fields.next();
					merged.set(field.getKey(), field.getValue());
				}
			}

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
		return jsonMapper;
	}

	public static String toPrettyString(JsonNode node) {
		try {
			return mapper().writerWithDefaultPrettyPrinter()//
					.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> readMap(String jsonString) {
		Check.notNullOrEmpty(jsonString, "JSON");
		try {
			return mapper().readValue(jsonString, Map.class);
		} catch (IOException e) {
			throw Exceptions.illegalArgument(e, //
					"error deserializing JSON string [%s]", jsonString);
		}
	}

	public static JsonNode readNode(String jsonString) {
		Check.notNullOrEmpty(jsonString, "JSON");
		try {
			return jsonMapper.readTree(jsonString);
		} catch (IOException e) {
			throw Exceptions.illegalArgument(e, //
					"error deserializing JSON string [%s]", jsonString);
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

	public static <K> K readObject(String json, Class<K> objectClass) {
		Check.notNull(json, "json");
		Check.notNull(objectClass, "object class");

		try {
			return mapper().readValue(json, objectClass);

		} catch (Exception e) {
			throw Exceptions.illegalArgument(e, "failed to map json [%s] to object of class [%s]", //
					json, objectClass.getSimpleName());
		}
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
	public static ObjectNode object(Object... fields) {
		return addAll(object(), fields);
	}

	public static ObjectNode addAll(ObjectNode object, Object... fields) {
		if (fields.length % 2 != 0)
			throw Exceptions.illegalArgument("odd number of elements");

		for (int i = 0; i < fields.length; i = i + 2)
			object.set(fields[i].toString(), toNode(fields[i + 1]));

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
		SimpleModule jodaModule = new JodaModule()//
				.addSerializer(LocalTime.class, new MyLocalTimeSerializer())//
				.addDeserializer(LocalTime.class, new MyLocalTimeDeserializer())//
				.addSerializer(LocalDate.class, new MyLocalDateSerializer())//
				.addDeserializer(LocalDate.class, new MyLocalDateDeserializer());

		jsonMapper = new ObjectMapper()//
				.setDefaultPrettyPrinter(new DefaultPrettyPrinter()//
						.withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE))//
				.registerModule(jodaModule)//
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)//
				.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}

	@Deprecated
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

	/**
	 * TODO Replace this by toPojo(JsonNode, Class<K>)
	 * 
	 * Example: File[] files = Json.toPojo(node, File[].class);
	 */
	@Deprecated
	public static Object[] toArray(ArrayNode arrayNode) {
		Object[] array = new Object[arrayNode.size()];
		for (int i = 0; i < array.length; i++)
			array[i] = toValue(arrayNode.get(i));
		return array;
	}

	public static JsonNode toNode(Object value) {
		if (value instanceof JsonNode)
			return (JsonNode) value;
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
		ArrayNode arrayNode = array();
		for (int i = 0; i < Array.getLength(array); i++)
			arrayNode.add(toNode(Array.get(array, i)));
		return arrayNode;
	}

	public static ArrayNode toCollectionNode(Collection<?> collection) {
		ArrayNode arrayNode = array();
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

	public static boolean isObject(JsonNode node) {
		return node != null && node.isObject();
	}

	public static ObjectNode checkObject(JsonNode node) {
		if (!isObject(node))
			throw Exceptions.illegalArgument(//
					"not a json object but [%s]", node.getNodeType());
		return (ObjectNode) node;
	}

	public static Optional7<JsonNode> checkObject(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.Object, required);
	}

	public static ArrayNode checkArray(JsonNode node) {
		if (!node.isArray())
			throw Exceptions.illegalArgument(//
					"not a json array but [%s]", node.getNodeType());
		return (ArrayNode) node;
	}

	public static Optional7<JsonNode> checkArray(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.Array, required);
	}

	public static Optional7<JsonNode> checkNode(JsonNode input, String propertyPath, boolean required) {
		JsonNode node = get(input, propertyPath);
		if (node == null) {
			if (required)
				throw Exceptions.illegalArgument("property [%s] is missing", propertyPath);
			return Optional7.ofNullable(null);
		}
		return Optional7.of(node);
	}

	public static JsonNode checkNotNull(ObjectNode jsonBody, String propertyPath) {
		JsonNode value = get(jsonBody, propertyPath);
		if (isNull(value))
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

	public static Optional7<String> checkString(JsonNode input, String path) {
		// return checkStringNode(input, path, false).flatMap(node ->
		// Optional.of(node.asText()));
		Optional7<JsonNode> optional = checkStringNode(input, path, false);
		return Optional7.ofNullable(optional.isPresent() ? optional.get().asText() : null);
	}

	public static Optional7<JsonNode> checkStringNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.String, required);
	}

	// public static Optional<Double> checkDouble(JsonNode push, String path) {
	// return checkDoubleNode(push, path, false).flatMap(node ->
	// Optional.of(node.asDouble()));
	// }

	public static Optional7<JsonNode> checkDoubleNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.Double, required);
	}

	public static boolean checkBoolean(JsonNode input, String path, boolean defaultValue) {
		return checkBoolean(input, path).orElse(defaultValue);
	}

	public static Optional7<Boolean> checkBoolean(JsonNode input, String path) {
		// return checkBooleanNode(input, path, false).flatMap(node ->
		// Optional.of(node.asBoolean()));
		Optional7<JsonNode> optional = checkBooleanNode(input, path, false);
		return Optional7.ofNullable(optional.isPresent() ? optional.get().asBoolean() : null);
	}

	public static Optional7<JsonNode> checkBooleanNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.Boolean, required);
	}

	public static Optional7<Integer> checkInteger(JsonNode input, String path) {
		// return checkIntegerNode(input, path, false).flatMap(node ->
		// Optional.of(node.asInt()));
		Optional7<JsonNode> optional = checkIntegerNode(input, path, false);
		return Optional7.ofNullable(optional.isPresent() ? optional.get().asInt() : null);
	}

	public static Optional7<JsonNode> checkIntegerNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.Integer, required);
	}

	// public static Optional<Long> checkLong(JsonNode input, String path) {
	// return checkLongNode(input, path, false).flatMap(node ->
	// Optional.of(node.asLong()));
	// }

	public static Optional7<JsonNode> checkLongNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, JsonType.Long, required);
	}

	private static Optional7<JsonNode> checkJsonNodeOfType(JsonNode input, String propertyPath, JsonType expected,
			boolean required) {
		JsonNode node = get(input, propertyPath);
		if (node == null) {
			if (required)
				throw Exceptions.illegalArgument("property [%s] is missing", propertyPath);
			return Optional7.empty();
		}
		if (isOfType(expected, node))
			return Optional7.of(node);
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

	public static Boolean checkBoolean(JsonNode node) {
		if (node == null)
			return null;
		if (!node.isBoolean())
			throw Exceptions.illegalArgument("json node [%s] not a boolean", node);
		return node.asBoolean();
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

	public static <K> K toPojo(JsonNode jsonNode, String fieldPath, Class<K> pojoClass) {
		Check.notNull(jsonNode, "jsonNode");
		Check.notNull(fieldPath, "fieldPath");

		jsonNode = get(jsonNode, fieldPath);
		if (isNull(jsonNode))
			throw Exceptions.illegalArgument("no field [%s] in json [%s]", //
					fieldPath, jsonNode);

		return toPojo(jsonNode, pojoClass);
	}

	public static <K> K updatePojo(String json, K pojo) {
		Check.notNull(json, "json");
		Check.notNull(pojo, "pojo");

		try {
			return mapper().readerForUpdating(pojo).readValue(json);

		} catch (IOException e) {
			throw Exceptions.runtime(e, "failed to map json [%s] to pojo class [%s]", //
					json, pojo.getClass().getSimpleName());
		}
	}

	public static <K> K toPojo(JsonNode jsonNode, Class<K> pojoClass) {
		Check.notNull(jsonNode, "jsonNode");
		Check.notNull(pojoClass, "pojoClass");

		try {
			return mapper().treeToValue(jsonNode, pojoClass);

		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e, "failed to map json [%s] to pojo class [%s]", //
					jsonNode, pojoClass.getSimpleName());
		}
	}

	public static <K> K toPojo(String json, Class<K> pojoClass) {
		Check.notNullOrEmpty(json, "json");
		Check.notNull(pojoClass, "pojoClass");

		try {
			return mapper().readValue(json, pojoClass);

		} catch (IOException e) {
			throw Exceptions.runtime(e, "error mapping string [%s] to pojo class [%s]", //
					json, pojoClass.getSimpleName());
		}
	}

	public static <K> K toPojo(byte[] jsonBytes, Class<K> pojoClass) {
		Check.notNull(jsonBytes, "jsonBytes");
		Check.notNull(pojoClass, "pojoClass");

		try {
			return mapper().readValue(jsonBytes, pojoClass);

		} catch (IOException e) {
			throw Exceptions.runtime(e, "error mapping bytes to pojo class [%s]", //
					pojoClass.getSimpleName());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <K extends JsonNode> JsonAssert<K> assertNode(K node) {
		return new JsonAssert(node);
	}

	public static ArrayNode withArray(ObjectNode node, String path) {
		JsonNode field = get(node, path);
		if (isNull(field)) {
			ArrayNode array = array();
			set(node, path, array);
			return array;
		}
		return checkArray(field);
	}

	public static ObjectNode withObject(ObjectNode node, String path) {
		JsonNode field = get(node, path);
		if (isNull(field)) {
			ObjectNode object = object();
			set(node, path, object);
			return object;
		}
		return checkObject(field);
	}

	public static String toString(Object object) {
		try {
			return mapper().writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw Exceptions.illegalArgument(e, "error processing object to json string");
		}
	}

}
