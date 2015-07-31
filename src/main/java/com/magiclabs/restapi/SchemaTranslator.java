package com.magiclabs.restapi;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class SchemaTranslator {

	private static JsonObject META_MAPPING = Json.builder()
			.add("type", "object") //
			.stObj("properties") //
			.stObj("createdBy") //
			.add("type", "string") //
			.add("index", "not_analyzed") //
			.end() //
			.stObj("updatedBy") //
			.add("type", "string") //
			.add("index", "not_analyzed") //
			.end() //
			.stObj("createdAt") //
			.add("type", "date") //
			.add("format", "date_time") //
			.end() //
			.stObj("updatedAt") //
			.add("type", "date") //
			.add("format", "date_time") //
			.build(); //

	public static JsonObject translate(String type, JsonObject schema) {

		JsonObject subMapping = toElasticMapping(schema.get(type).asObject());
		subMapping.add("_meta", schema);

		JsonObject mapping = new JsonObject();
		mapping.add(type, subMapping);

		System.out.println(Json.prettyString(mapping));
		return mapping;
	}

	private static JsonObject toElasticMapping(JsonObject schema) {

		String type = schema.getString("_type", "object");

		if ("object".equals(type)) {
			JsonBuilder builder = Json
					.builder()
					// Enable _timestamp when I find out how to read/get it back
					.stObj("_timestamp")
					.add("enabled", false)
					.end()
					.add("dynamic", "strict")
					.add("date_detection", false)
					.addJson(
							"properties",
							toElasticProperties(schema).add("meta",
									META_MAPPING));

			JsonValue id = schema.get("_id");
			if (id != null)
				builder = builder.stObj("_id") //
						.add("path", id.asString()).end();

			return builder.build();
		} else
			throw new IllegalArgumentException(String.format(
					"Invalid schema root type [%s]", type));
	}

	private static JsonObject toElasticProperties(JsonObject schema) {

		JsonBuilder builder = Json.builder();
		for (String key : schema.names()) {
			if (key.charAt(0) != '_') {
				builder.addJson(key,
						toElasticProperty(key, schema.get(key).asObject()));
			}
		}
		return builder.build();
	}

	private static JsonValue toElasticProperty(String key, JsonObject schema) {
		JsonObject mapping = new JsonObject();
		String type = schema.getString("_type", "object");

		if ("text".equals(type)) {
			mapping.add("type", "string");
			mapping.add("index", "analyzed");
			mapping.add("analyzer", schema.getString("_language", "english"));
		} else if ("string".equals(type)) {
			mapping.add("type", "string");
			mapping.add("index", "not_analyzed");
		} else if ("boolean".equals(type)) {
			mapping.add("type", "boolean");
		} else if ("integer".equals(type)) {
			mapping.add("type", "integer");
			mapping.add("coerce", "false");
		} else if ("long".equals(type)) {
			mapping.add("type", "long");
			mapping.add("coerce", "false");
		} else if ("float".equals(type)) {
			mapping.add("type", "float");
			mapping.add("coerce", "false");
		} else if ("double".equals(type)) {
			mapping.add("type", "double");
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
			mapping.add("properties", toElasticProperties(schema));
		} else if ("stash".equals(type)) {
			mapping.add("type", "object");
			mapping.add("enabled", false);
		} else {
			throw new IllegalArgumentException("Invalid type [" + type
					+ "] for property [" + key + "]");
		}
		return mapping;
	}
}
