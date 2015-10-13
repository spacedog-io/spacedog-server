/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.HashSet;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.common.base.Strings;

public class Json {

	public static JsonValue get(JsonValue json, String path) {

		JsonValue current = json;

		for (String s : path.split("\\.")) {

			if (current.isObject())
				current = current.asObject().get(s);

			else if (current.isArray())
				current = current.asArray().get(Integer.parseInt(s));
		}

		return current;
	}

	public static boolean equals(JsonValue v1, JsonValue v2) {
		if (v1 == v2)
			return true;

		if (v1 == null || v1.isNull())
			return v2 == null || v2.isNull();

		if (v2 == null || v2.isNull())
			return v1 == null || v1.isNull();

		if (v1.getClass() != v2.getClass())
			return false;

		if (v1.isBoolean())
			return v1.equals(v2);

		if (v1.isNumber())
			return v1.equals(v2);

		if (v1.isString())
			return v1.equals(v2);

		if (v1.isObject()) {
			Set<String> fields = new HashSet<String>(v1.asObject().names());
			fields.addAll(v2.asObject().names());

			for (String fieldName : fields) {
				if (!equals(v1.asObject().get(fieldName), v2.asObject().get(fieldName))) {
					return false;
				}
			}
		}

		if (v1.isArray()) {
			if (v1.asArray().size() != v2.asArray().size())
				return false;

			for (int i = 0; i < v1.asArray().size(); i++) {
				if (!equals(v1.asArray().get(i), v2.asArray().get(i)))
					return false;
			}
		}

		return true;
	}

	public static JsonBuilder builder() {
		return new JsonBuilder();
	}

	public static JsonMerger merger() {
		return new JsonMerger();
	}

	public static class JsonMerger {

		private JsonObject merged;

		public JsonMerger add(JsonObject jsonObject) {
			if (merged == null)
				merged = jsonObject;
			else {
				for (String name : jsonObject.names()) {
					merged.add(name, jsonObject.get(name));
				}
			}
			return this;
		}

		public JsonObject get() {
			return merged == null ? new JsonObject() : merged;
		}

	}

	public static String prettyString(JsonValue json) {
		StringBuilder builder = new StringBuilder();
		prettyString(json, builder, 1);
		return builder.toString();
	}

	private static void prettyString(JsonValue json, StringBuilder builder, int level) {
		if (json.isArray()) {
			builder.append("[ ");
			for (int i = 0; i < json.asArray().size(); i++) {
				JsonValue item = json.asArray().get(i);
				prettyString(item, builder, level + 1);
				if (i == json.asArray().size() - 1)
					builder.append(" ]");
				else if (item.isString() && item.asString().length() > 35)
					builder.append(",\n");
				else
					builder.append(", ");
			}
		} else if (json.isObject()) {
			builder.append("{\n");
			json.asObject().forEach(member -> {
				prettyTab(builder, level);
				builder.append(member.getName()).append(" : ");
				prettyString(member.getValue(), builder, level + 1);
				builder.append('\n');
			});
			prettyTab(builder, level - 1);
			builder.append("}");
		} else {
			builder.append(json.toString());
		}
	}

	private static void prettyTab(StringBuilder builder, int level) {
		for (int i = 0; i < level * 4; i++)
			builder.append(' ');
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

	public static boolean isNull(JsonValue value) {
		return value == null || JsonValue.NULL.equals(value);
	}
}
