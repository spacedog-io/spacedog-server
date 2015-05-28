package com.magiclabs.restapi;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class SchemaTranslator {

	public static JsonObject translate(String type, JsonObject schema) {

		JsonObject subMapping = toElasticType(schema.get(type).asObject());
		subMapping.add("_meta", schema);

		JsonObject mapping = new JsonObject();
		mapping.add(type, subMapping);

		return mapping;
	}

	private static JsonObject toElasticType(JsonObject schema) {
		JsonObject subMapping = new JsonObject() //
				.add("_timestamp", new JsonObject().add("enabled", true)) //
				.add("dynamic", "strict") //
				.add("date_detection", false) //
				.add("properties", toElasticProperties(schema));

		JsonValue id = schema.get("_id");
		if (id != null)
			subMapping.add("_id", new JsonObject().add("path", id));

		return subMapping;
	}

	private static JsonObject toElasticProperties(JsonObject schema) {
		JsonObject mapping = new JsonObject();
		for (String key : schema.names()) {
			if (key.charAt(0) != '_') {
				mapping.add(key,
						toElasticProperty(key, schema.get(key).asObject()));
			}
		}
		return mapping;
	}

	private static JsonValue toElasticProperty(String key, JsonObject schema) {
		JsonObject mapping = new JsonObject();
		String type = schema.getString("_type", "object");

		if ("text".equals(type)) {
			mapping.add("type", "string");
		} else if ("code".equals(type)) {
			mapping.add("type", "string");
			mapping.add("index", "not_analyzed");
		} else if ("boolean".equals(type)) {
			mapping.add("type", "boolean");
		} else if ("integer".equals(type)) {
			mapping.add("type", "integer");
			mapping.add("coerce", "false");
		} else if ("float".equals(type)) {
			mapping.add("type", "float");
			mapping.add("coerce", "false");
		} else if ("date".equals(type)) {
			mapping.add("type", "date");
			mapping.add("format", "date");
		} else if ("time".equals(type)) {
			mapping.add("type", "date");
			mapping.add("format", "hour_minute_second");
		} else if ("timestamp".equals(type)) {
			mapping.add("type", "date");
			mapping.add("format", "date_time");
		} else if ("enum".equals(type)) {
			mapping.add("type", "string");
			mapping.add("index", "not_analyzed");
		} else if ("geopoint".equals(type)) {
			mapping.add("type", "geo_point");
			mapping.add("lat_lon", true);
			mapping.add("geohash", true);
			mapping.add("geohash_precision", "1m");
			mapping.add("geohash_prefix", "true");
		} else if ("object".equals(type)) {
			mapping.add("type", "object");
			mapping.add("properties", toElasticProperties(schema.asObject()));
		} else if ("stash".equals(type)) {
			mapping.add("type", "object");
			mapping.add("enabled", false);
			// mapping.add("dynamic", "true");
		} else {
			throw new IllegalArgumentException("Invalid type [" + type
					+ "] for property [" + key + "]");
		}
		return mapping;
	}
}
