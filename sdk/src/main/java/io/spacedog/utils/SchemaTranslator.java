/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SchemaTranslator {

	private static JsonNode META_MAPPING = Json7.objectBuilder()//
			.put("type", "object") //
			.object("properties") //
			.object("createdBy") //
			.put("type", "string") //
			.put("index", "not_analyzed") //
			.end() //
			.object("updatedBy") //
			.put("type", "string") //
			.put("index", "not_analyzed") //
			.end() //
			.object("createdAt") //
			.put("type", "date") //
			.put("format", "date_time") //
			.end() //
			.object("updatedAt") //
			.put("type", "date") //
			.put("format", "date_time") //
			.build(); //

	public static ObjectNode translate(String type, JsonNode schema) {

		ObjectNode subMapping = toElasticMapping(schema.get(type));
		subMapping.set("_meta", schema);

		return Json7.objectBuilder().node(type, subMapping).build();
	}

	private static ObjectNode toElasticMapping(JsonNode schema) {

		String type = schema.path("_type").asText("object");

		String defaultLanguage = language(schema, "french");
		ObjectNode propertiesNode = toElasticProperties(schema, defaultLanguage);
		propertiesNode.set("meta", META_MAPPING);

		if ("object".equals(type))
			return Json7.objectBuilder()//
					.put("dynamic", "strict")//
					.put("date_detection", false)//
					.node("properties", propertiesNode)//
					// This breaks when updating mappings since default language
					// of old indices was mostly english and it is now
					// replaced by french
					// .object("_all")//
					// .put("analyzer", defaultLanguage)//
					.build();

		throw Exceptions.illegalArgument("invalid schema root type [%s]", type);
	}

	private static ObjectNode toElasticProperties(JsonNode schema, String defaultLanguage) {

		JsonBuilder<ObjectNode> builder = Json7.objectBuilder();
		Iterator<String> fieldNames = schema.fieldNames();
		while (fieldNames.hasNext()) {
			String fieldName = fieldNames.next();
			if (fieldName.charAt(0) != '_') {
				builder.node(fieldName, toElasticProperty(fieldName, schema.get(fieldName), defaultLanguage));
			}
		}
		return builder.build();
	}

	private static String language(JsonNode schema, String defaultLanguage) {
		return schema.path("_language").asText(defaultLanguage);
	}

	private static ObjectNode toElasticProperty(String key, JsonNode schema, String defaultLanguage) {
		JsonBuilder<ObjectNode> mapping = Json7.objectBuilder();
		String type = schema.path("_type").asText("object");

		if ("text".equals(type)) {
			mapping.put("type", "string");
			mapping.put("index", "analyzed");
			mapping.put("analyzer", language(schema, defaultLanguage));
		} else if ("string".equals(type)) {
			mapping.put("type", "string");
			mapping.put("index", "not_analyzed");
		} else if ("boolean".equals(type)) {
			mapping.put("type", "boolean");
		} else if ("integer".equals(type)) {
			mapping.put("type", "integer");
			mapping.put("coerce", "false");
		} else if ("long".equals(type)) {
			mapping.put("type", "long");
			mapping.put("coerce", "false");
		} else if ("float".equals(type)) {
			mapping.put("type", "float");
			mapping.put("coerce", "false");
		} else if ("double".equals(type)) {
			mapping.put("type", "double");
			mapping.put("coerce", "false");
		} else if ("date".equals(type)) {
			mapping.put("type", "date");
			mapping.put("format", "date");
		} else if ("time".equals(type)) {
			mapping.put("type", "date");
			mapping.put("format", "hour_minute_second");
		} else if ("timestamp".equals(type)) {
			mapping.put("type", "date");
			mapping.put("format", "date_time");
		} else if ("enum".equals(type)) {
			mapping.put("type", "string");
			mapping.put("index", "not_analyzed");
		} else if ("geopoint".equals(type)) {
			mapping.put("type", "geo_point");
			mapping.put("lat_lon", true);
			mapping.put("geohash", true);
			mapping.put("geohash_precision", "1m");
			mapping.put("geohash_prefix", "true");
		} else if ("object".equals(type)) {
			mapping.put("type", "object");
			mapping.node("properties", //
					toElasticProperties(schema, language(schema, defaultLanguage)));
		} else if ("stash".equals(type)) {
			mapping.put("type", "object");
			mapping.put("enabled", false);
		} else {
			throw Exceptions.illegalArgument("invalid type [%s] for property [%s]", type, key);
		}
		return mapping.build();
	}
}
