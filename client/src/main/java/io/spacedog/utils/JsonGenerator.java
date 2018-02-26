/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.client.schema.Schema;

public class JsonGenerator {

	private Map<String, List<Object>> paths = Maps.newHashMap();
	private Map<String, List<String>> types = Maps.newHashMap();
	private Random random = new Random();

	public JsonGenerator() {
	}

	public void regPath(String path, Object value) {
		List<Object> values = paths.get(path);
		if (values == null) {
			values = Lists.newArrayList();
			paths.put(path, values);
		}
		values.add(value);
	}

	public void regPath(String path, List<Object> moreValues) {
		List<Object> values = paths.get(path);
		if (values == null) {
			values = Lists.newArrayList();
			paths.put(path, values);
		}
		values.addAll(moreValues);
	}

	public void regPath(String path, Object... moreValues) {
		regPath(path, Arrays.asList(moreValues));
	}

	public void regType(String type, List<String> values) {
		types.put(type, values);
	}

	public ObjectNode gen(Schema schema) {
		return gen(schema, 0);
	}

	public ObjectNode gen(Schema schema, int index) {
		LinkedList<String> stack = new LinkedList<>();
		return generateObject(stack, (ObjectNode) schema.node().elements().next(), index);
	}

	private ObjectNode generateObject(LinkedList<String> stack, ObjectNode schema, int index) {
		JsonBuilder<ObjectNode> builder = Json.builder().object();
		generateFields(stack, builder, schema, index);
		return builder.build();
	}

	private void generateFields(LinkedList<String> path, JsonBuilder<ObjectNode> builder, ObjectNode schema,
			int index) {
		Iterator<Entry<String, JsonNode>> fields = schema.fields();
		while (fields.hasNext()) {
			Entry<String, JsonNode> field = fields.next();

			String fieldKey = field.getKey();
			if (fieldKey.startsWith("_"))
				continue;

			ObjectNode fieldSchema = (ObjectNode) field.getValue();
			boolean fieldIsArray = fieldSchema.path("_array").asBoolean(false);

			path.add(fieldKey);

			if (fieldIsArray)
				builder.array(fieldKey)//
						.add(generateValue(path, fieldSchema, index))//
						.add(generateValue(path, fieldSchema, index + 1))//
						.end();
			else
				builder.add(fieldKey, generateValue(path, fieldSchema, index));

			path.removeLast();

		}
	}

	private JsonNode generateValue(LinkedList<String> path, ObjectNode schema, int index) {

		String stringPath = Utils.join(".", path.toArray(new String[path.size()]));
		List<Object> list = paths.get(stringPath);
		if (list != null)
			return Json.toJsonNode(list.get(random.nextInt(list.size())));

		JsonNode values = Json.get(schema, "_values");
		if (values != null) {
			if (values.isArray()) {
				if (index < values.size())
					return values.get(index);
				return values.get(random.nextInt(values.size()));
			} else
				return values;
		}

		JsonNode enumType = Json.get(schema, "_enumType");
		if (enumType != null) {
			if (types.containsKey(enumType.asText())) {
				List<String> typeValues = types.get(enumType.asText());
				String value = typeValues.get(random.nextInt(typeValues.size()));
				return Json.toJsonNode(value);
			}
		}

		JsonNode examples = Json.get(schema, "_examples");
		if (examples != null) {
			if (examples.isArray()) {
				if (index < examples.size())
					return examples.get(index);
				return examples.get(random.nextInt(examples.size()));
			} else
				return examples;
		}

		String type = schema.get("_type").asText();

		if ("text".equals(type))
			return TextNode.valueOf(
					"But I must explain to you how all this mistaken idea of denouncing pleasure and praising pain was born and I will give you a complete account of the system, and expound the actual teachings of the great explorer of the truth, the master-builder of human happiness.");
		else if ("string".equals(type))
			return TextNode.valueOf("RD5654GH78");
		else if ("boolean".equals(type))
			return BooleanNode.valueOf(random.nextBoolean());
		else if ("integer".equals(type))
			return IntNode.valueOf(random.nextInt());
		else if ("long".equals(type))
			return LongNode.valueOf(random.nextLong());
		else if ("float".equals(type))
			return FloatNode.valueOf(random.nextFloat());
		else if ("double".equals(type))
			return DoubleNode.valueOf(random.nextDouble());
		else if ("date".equals(type))
			return TextNode.valueOf("2015-09-09");
		else if ("time".equals(type))
			return TextNode.valueOf("15:30:00");
		else if ("timestamp".equals(type))
			return TextNode.valueOf("2015-01-09T15:37:00.123Z");
		else if ("enum".equals(type))
			return TextNode.valueOf("blue");
		else if ("geopoint".equals(type))
			return Json.object("lat", 48 + random.nextDouble(), "lon", 2 + random.nextDouble());
		else if ("object".equals(type))
			return generateObject(path, schema, index);

		return NullNode.getInstance();
	}
}
