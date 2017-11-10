/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.io.IOException;
import java.net.URL;
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
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
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
		return toObject(node);
	}

	public static JsonNode set(JsonNode json, String fieldPath, Object value) {
		JsonNode node = toJsonNode(value);
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

		JsonNode valueNode = toJsonNode(value);
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
	// Field path implementation
	//

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
	public static Map<String, Object> readMap(String json) {
		Check.notNullOrEmpty(json, "json");
		try {
			return mapper().readValue(json, Map.class);
		} catch (IOException e) {
			throw Exceptions.illegalArgument(e, //
					"error deserializing JSON string [%s]", json);
		}
	}

	public static JsonNode readNode(String json) {
		Check.notNullOrEmpty(json, "json");
		try {
			return jsonMapper.readTree(json);
		} catch (IOException e) {
			throw Exceptions.illegalArgument(e, //
					"error deserializing JSON string [%s]", json);
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

	public static ObjectNode readObject(String json) {
		return checkObject(readNode(json));
	}

	public static ArrayNode readArray(String json) {
		return checkArray(readNode(json));
	}

	//
	// Json Builder
	//

	private static final Builder builder = new Builder();

	public static Builder builder() {
		return builder;
	}

	public static class Builder {

		public JsonBuilder<ObjectNode> object() {
			return new JsonBuilder<ObjectNode>().object();
		}

		public JsonBuilder<ArrayNode> array() {
			return new JsonBuilder<ArrayNode>().array();
		}

	}

	//
	// Factory methods
	//

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
			object.set(fields[i].toString(), toJsonNode(fields[i + 1]));

		return object;
	}

	public static ArrayNode array() {
		return mapper().getNodeFactory().arrayNode();
	}

	public static ArrayNode array(Object... values) {
		return addAll(array(), values);
	}

	public static ArrayNode addAll(ArrayNode array, Object... values) {
		for (int i = 0; i < values.length; i++)
			array.add(toJsonNode(values[i]));
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

	//
	// Check and assert methods
	//

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <K extends JsonNode> JsonAssert<K> assertNode(K node) {
		return new JsonAssert(node);
	}

	public static boolean isObject(JsonNode node) {
		return node != null && node.isObject();
	}

	public static ObjectNode checkObject(JsonNode node) {
		if (!isObject(node))
			throw Exceptions.illegalArgument("[%s] not an object", node);
		return (ObjectNode) node;
	}

	public static boolean isArray(JsonNode node) {
		return node != null && node.isArray();
	}

	public static ArrayNode checkArray(JsonNode node) {
		if (!isArray(node))
			throw Exceptions.illegalArgument("[%s] not an array ", node);
		return (ArrayNode) node;
	}

	//
	// Other check methods
	//

	public static Optional7<JsonNode> checkObject(JsonNode input, String propertyPath, boolean required) {
		return checkType(input, propertyPath, JsonNodeType.OBJECT, required);
	}

	public static String checkStringNotNullOrEmpty(JsonNode input, String propertyPath) {
		String string = checkStringNode(input, propertyPath, true).get().asText();
		if (Strings.isNullOrEmpty(string))
			throw Exceptions.illegalArgument("property [%s] is missing", propertyPath);

		return string;
	}

	public static Optional7<String> checkString(JsonNode input, String path) {
		Optional7<JsonNode> optional = checkStringNode(input, path, false);
		return Optional7.ofNullable(optional.isPresent() ? optional.get().asText() : null);
	}

	public static Optional7<JsonNode> checkStringNode(JsonNode input, String propertyPath, boolean required) {
		return checkType(input, propertyPath, JsonNodeType.STRING, required);
	}

	private static Optional7<JsonNode> checkType(JsonNode input, String fieldPath, JsonNodeType expected,
			boolean required) {
		JsonNode node = get(input, fieldPath);
		if (node == null) {
			if (required)
				throw Exceptions.illegalArgument("field [%s] is missing", fieldPath);
			return Optional7.empty();
		}
		if (node.getNodeType().equals(expected))
			return Optional7.of(node);
		else
			throw Exceptions.illegalArgument(//
					"field [%s] must be of type [%s] instead of [%s]", //
					fieldPath, expected, node.getNodeType());
	}

	//
	// JsonNode check helpers
	//

	public static String checkString(JsonNode node) {
		if (node == null)
			return null;
		if (!node.isTextual())
			throw Exceptions.illegalArgument("[%s] not a string", node);
		return node.asText();
	}

	public static Boolean checkBoolean(JsonNode node) {
		if (node == null)
			return null;
		if (!node.isBoolean())
			throw Exceptions.illegalArgument("[%s] not a boolean", node);
		return node.asBoolean();
	}

	public static Double checkDouble(JsonNode node) {
		if (node == null)
			return null;
		if (!node.isDouble())
			throw Exceptions.illegalArgument("[%s] not a double", node);
		return node.asDouble();
	}

	public static JsonNode checkNotNull(JsonNode node) {
		if (node == null || node.isNull())
			throw Exceptions.illegalArgument("json node is null");
		return node;
	}

	//
	// Conversion methods
	//

	public static ObjectNode toObjectNode(Object object) {
		return checkObject(toJsonNode(object));
	}

	public static ArrayNode toArrayNode(Object object) {
		return checkArray(toJsonNode(object));
	}

	// TODO add shortcuts to speed up conversion (int, long, ...)
	public static JsonNode toJsonNode(Object object) {
		if (object instanceof JsonNode)
			return (JsonNode) object;
		return mapper().valueToTree(object);
	}

	public static Object toObject(JsonNode node) {
		return toPojo(node, Object.class);
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
		Check.notNull(pojoClass, "pojoClass");
		if (Strings.isNullOrEmpty(json))
			json = pojoClass.isArray() ? "[]" : "{}";

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

	public static <K> K toPojo(JsonNode jsonNode, String fieldPath, Class<K> pojoClass) {
		Check.notNull(jsonNode, "jsonNode");
		Check.notNull(fieldPath, "fieldPath");

		jsonNode = get(jsonNode, fieldPath);
		if (isNull(jsonNode))
			throw Exceptions.illegalArgument("field [%s] is null in json [%s]", //
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

	//
	// Other methods
	//

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

}
