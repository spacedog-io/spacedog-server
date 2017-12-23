/**
 * © David Attias 2015
 */
package io.spacedog.model;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.http.SpaceFields;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;

public class SchemaTranslator implements SpaceFields, SchemaDirectives, MappingDirectives {

	private static final ObjectNode STASH = //
			Json.object(type_, object_, enable_, false);
	private static final ObjectNode STRING = //
			Json.object(type_, string_, index_, not_analyzed_);

	private static final ObjectNode TIMESTAMP = //
			Json.object(type_, date_, format_, "date_time");
	private static final ObjectNode DATE = //
			Json.object(type_, date_, format_, "date");
	private static final ObjectNode TIME = //
			Json.object(type_, date_, format_, "hour_minute_second");

	private static final ObjectNode BOOLEAN = //
			Json.object(type_, boolean_);
	private static final ObjectNode INTEGER = //
			Json.object(type_, integer_, coerce_, false);
	private static final ObjectNode LONG = //
			Json.object(type_, longg, coerce_, false);
	private static final ObjectNode FLOAT = //
			Json.object(type_, float_, coerce_, false);
	private static final ObjectNode DOUBLE = //
			Json.object(type_, double_, coerce_, false);
	private static final ObjectNode GEOPOINT = //
			Json.object(type_, "geo_point", "lat_lon", true, "geohash", true, //
					"geohash_precision", "1m", "geohash_prefix", true);

	public static ObjectNode translate(String type, JsonNode schema) {

		ObjectNode subMapping = toElasticMapping(schema.get(type));
		subMapping.set("_meta", schema);

		return Json.builder().object().add(type, subMapping).build();
	}

	private static ObjectNode toElasticMapping(JsonNode schema) {

		String type = schema.path(_TYPE).asText(object_);

		String defaultLanguage = language(schema, "french");
		ObjectNode propertiesNode = toElasticProperties(schema, defaultLanguage);
		addMetadataFields(propertiesNode);

		if (object_.equals(type))
			return Json.builder().object()//
					.add("dynamic", "strict")//
					.add("date_detection", false)//
					.add(properties_, propertiesNode)//
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
		return schema.path(_LANGUAGE).asText(defaultLanguage);
	}

	private static ObjectNode toElasticProperty(String key, JsonNode schema, String defaultLanguage) {
		String type = schema.path(_TYPE).asText(object_);
		if ("text".equals(type))
			return Json.object(type_, string_, index_, "analyzed", //
					"analyzer", language(schema, defaultLanguage));
		else if (string_.equals(type))
			return STRING;
		else if (boolean_.equals(type))
			return BOOLEAN;
		else if (integer_.equals(type))
			return INTEGER;
		else if (longg.equals(type))
			return LONG;
		else if (float_.equals(type))
			return FLOAT;
		else if (double_.equals(type))
			return DOUBLE;
		else if (date_.equals(type))
			return DATE;
		else if ("time".equals(type))
			return TIME;
		else if ("timestamp".equals(type))
			return TIMESTAMP;
		else if ("enum".equals(type))
			return STRING;
		else if ("geopoint".equals(type))
			return GEOPOINT;
		else if (object_.equals(type))
			return Json.object(type_, object_, //
					properties_, toElasticProperties(schema, language(schema, defaultLanguage)));
		else if ("stash".equals(type))
			return STASH;

		throw Exceptions.illegalArgument("invalid type [%s] for property [%s]", type, key);
	}
}
