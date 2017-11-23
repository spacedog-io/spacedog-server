/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SchemaTranslator implements SpaceFields {

	private static final ObjectNode STASH = //
			Json.object("type", "object", "enabled", false);
	private static final ObjectNode STRING = //
			Json.object("type", "string", "index", "not_analyzed");

	private static final ObjectNode TIMESTAMP = //
			Json.object("type", "date", "format", "date_time");
	private static final ObjectNode DATE = //
			Json.object("type", "date", "format", "date");
	private static final ObjectNode TIME = //
			Json.object("type", "date", "format", "hour_minute_second");

	private static final ObjectNode BOOLEAN = //
			Json.object("type", "boolean");
	private static final ObjectNode INTEGER = //
			Json.object("type", "integer", "coerce", "false");
	private static final ObjectNode LONG = //
			Json.object("type", "long", "coerce", "false");
	private static final ObjectNode FLOAT = //
			Json.object("type", "float", "coerce", "false");
	private static final ObjectNode DOUBLE = //
			Json.object("type", "double", "coerce", "false");
	private static final ObjectNode GEOPOINT = //
			Json.object("type", "geo_point", "lat_lon", true, "geohash", true, //
					"geohash_precision", "1m", "geohash_prefix", "true");

	public static ObjectNode translate(String type, JsonNode schema) {

		ObjectNode subMapping = toElasticMapping(schema.get(type));
		subMapping.set("_meta", schema);

		return Json.builder().object().add(type, subMapping).build();
	}

	private static ObjectNode toElasticMapping(JsonNode schema) {

		String type = schema.path("_type").asText("object");

		String defaultLanguage = language(schema, "french");
		ObjectNode propertiesNode = toElasticProperties(schema, defaultLanguage);
		addMetadataFields(propertiesNode);

		if ("object".equals(type))
			return Json.builder().object()//
					.add("dynamic", "strict")//
					.add("date_detection", false)//
					.add("properties", propertiesNode)//
					.object("_all")//
					.add("analyzer", defaultLanguage)//
					.build();

		throw Exceptions.illegalArgument("invalid schema root type [%s]", type);
	}

	private static void addMetadataFields(ObjectNode propertiesNode) {
		propertiesNode.set(OWNER_FIELD, STRING);
		propertiesNode.set(GROUP_FIELD, STRING);
		propertiesNode.set(CREATED_AT_FIELD, TIMESTAMP);
		propertiesNode.set(UPDATED_AT_FIELD, TIMESTAMP);
	}

	private static ObjectNode toElasticProperties(JsonNode schema, String defaultLanguage) {

		JsonBuilder<ObjectNode> builder = Json.builder().object();
		Iterator<String> fieldNames = schema.fieldNames();
		while (fieldNames.hasNext()) {
			String fieldName = fieldNames.next();
			if (fieldName.charAt(0) != '_') {
				builder.add(fieldName, toElasticProperty(//
						fieldName, schema.get(fieldName), defaultLanguage));
			}
		}
		return builder.build();
	}

	private static String language(JsonNode schema, String defaultLanguage) {
		return schema.path("_language").asText(defaultLanguage);
	}

	private static ObjectNode toElasticProperty(String key, JsonNode schema, String defaultLanguage) {
		String type = schema.path("_type").asText("object");
		if ("text".equals(type))
			return Json.object("type", "string", "index", "analyzed", //
					"analyzer", language(schema, defaultLanguage));
		else if ("string".equals(type))
			return STRING;
		else if ("boolean".equals(type))
			return BOOLEAN;
		else if ("integer".equals(type))
			return INTEGER;
		else if ("long".equals(type))
			return LONG;
		else if ("float".equals(type))
			return FLOAT;
		else if ("double".equals(type))
			return DOUBLE;
		else if ("date".equals(type))
			return DATE;
		else if ("time".equals(type))
			return TIME;
		else if ("timestamp".equals(type))
			return TIMESTAMP;
		else if ("enum".equals(type))
			return STRING;
		else if ("geopoint".equals(type))
			return GEOPOINT;
		else if ("object".equals(type))
			return Json.object("type", "object", //
					"properties", toElasticProperties(schema, language(schema, defaultLanguage)));
		else if ("stash".equals(type))
			return STASH;

		throw Exceptions.illegalArgument("invalid type [%s] for property [%s]", type, key);
	}
}
