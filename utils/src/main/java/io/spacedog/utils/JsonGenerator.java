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

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Maps;

public class JsonGenerator {

	private Map<String, List<Object>> map = Maps.newHashMap();
	private Random random = new Random();

	public JsonGenerator() {
	}

	public void reg(String path, Object value) {
		List<Object> values = map.get(path);
		if (values == null) {
			values = Lists.newArrayList();
			map.put(path, values);
		}
		values.add(value);
	}

	public void reg(String path, List<Object> moreValues) {
		List<Object> values = map.get(path);
		if (values == null) {
			values = Lists.newArrayList();
			map.put(path, values);
		}
		values.addAll(moreValues);
	}

	public void reg(String path, Object... moreValues) {
		reg(path, Arrays.asList(moreValues));
	}

	public ObjectNode gen(ObjectNode schema) {
		LinkedList<String> stack = new LinkedList<String>();
		return generateObject(stack, (ObjectNode) schema.elements().next());
	}

	private ObjectNode generateObject(LinkedList<String> stack, ObjectNode schema) {
		JsonBuilder<ObjectNode> builder = Json.objectBuilder();
		generateFields(stack, builder, schema);
		return builder.build();
	}

	private void generateFields(LinkedList<String> path, JsonBuilder<ObjectNode> builder, ObjectNode schema) {
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
						.node(generateValue(path, fieldSchema))//
						.node(generateValue(path, fieldSchema))//
						.end();
			else
				builder.node(fieldKey, generateValue(path, fieldSchema));

			path.removeLast();

		}
	}

	private JsonNode generateValue(LinkedList<String> path, ObjectNode schema) {

		String stringPath = String.join(".", path.toArray(new String[path.size()]));
		List<Object> list = map.get(stringPath);
		if (list != null)
			return Json.toNode(list.get(random.nextInt(list.size())));

		JsonNode values = Json.get(schema, "_values");
		if (values != null) {
			if (values.isArray())
				return values.get(random.nextInt(values.size()));
			else
				return values;
		}

		JsonNode examples = Json.get(schema, "_examples");
		if (examples != null) {
			if (examples.isArray())
				return examples.get(random.nextInt(examples.size()));
			else
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
			return Json.object("lat", -55.6765, "lon", -54.6765);
		else if ("object".equals(type))
			return generateObject(path, schema);

		return NullNode.getInstance();
	}
}
