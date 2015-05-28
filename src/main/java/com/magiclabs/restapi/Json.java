package com.magiclabs.restapi;

import java.util.HashSet;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class Json {

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
				if (!equals(v1.asObject().get(fieldName),
						v2.asObject().get(fieldName))) {
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
}
