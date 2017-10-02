/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SchemaTranslator {

	private static JsonNode META_MAPPING = Json.builder().object()//
			.add("type", "object") //
			.object("properties") //
			.object("createdBy") //
			.add("type", "string") //
			.add("index", "not_analyzed") //
			.end() //
			.object("updatedBy") //
			.add("type", "string") //
			.add("index", "not_analyzed") //
			.end() //
			.object("createdAt") //
			.add("type", "date") //
			.add("format", "date_time") //
			.end() //
			.object("updatedAt") //
			.add("type", "date") //
			.add("format", "date_time") //
			.build(); //

	public static ObjectNode translate(String type, JsonNode schema) {

		ObjectNode subMapping = toElasticMapping(schema.get(type));
		subMapping.set("_meta", schema);

		return Json.builder().object().add(type, subMapping).build();
	}

	private static ObjectNode toElasticMapping(JsonNode schema) {

		String type = schema.path("_type").asText("object");

		ObjectNode propertiesNode = toElasticProperties(schema);
		propertiesNode.set("meta", META_MAPPING);

		if ("object".equals(type)) {
			JsonBuilder<ObjectNode> builder = Json.builder().object()//
					.add("dynamic", "strict")//
					.add("date_detection", false)//
					.add("properties", propertiesNode);

			return builder.build();
		} else
			throw Exceptions.illegalArgument("invalid schema root type [%s]", type);
	}

	private static ObjectNode toElasticProperties(JsonNode schema) {

		JsonBuilder<ObjectNode> builder = Json.builder().object();
		Iterator<String> fieldNames = schema.fieldNames();
		while (fieldNames.hasNext()) {
			String fieldName = fieldNames.next();
			if (fieldName.charAt(0) != '_') {
				builder.add(fieldName, toElasticProperty(fieldName, schema.get(fieldName)));
			}
		}
		return builder.build();
	}

	private static ObjectNode toElasticProperty(String key, JsonNode schema) {
		JsonBuilder<ObjectNode> mapping = Json.builder().object();
		String type = schema.path("_type").asText("object");

		if ("text".equals(type)) {
			mapping.add("type", "string");
			mapping.add("index", "analyzed");
			mapping.add("analyzer", schema.path("_language").asText("english"));
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
			throw Exceptions.illegalArgument("invalid type [%s] for property [%s]", type, key);
		}
		return mapping.build();
	}
}
