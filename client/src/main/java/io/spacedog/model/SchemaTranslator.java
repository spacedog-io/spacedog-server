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
			Json.object(m_type, m_object, m_enable, false);
	private static final ObjectNode KEYWORD = //
			Json.object(m_type, m_keyword);

	private static final ObjectNode TIMESTAMP = //
			Json.object(m_type, m_date, m_format, "date_time");
	private static final ObjectNode DATE = //
			Json.object(m_type, m_date, m_format, "date");
	private static final ObjectNode TIME = //
			Json.object(m_type, m_date, m_format, "hour_minute_second");

	private static final ObjectNode BOOLEAN = //
			Json.object(m_type, m_boolean);
	private static final ObjectNode INTEGER = //
			Json.object(m_type, m_integer, m_coerce, false);
	private static final ObjectNode LONG = //
			Json.object(m_type, m_long, m_coerce, false);
	private static final ObjectNode FLOAT = //
			Json.object(m_type, m_float, m_coerce, false);
	private static final ObjectNode DOUBLE = //
			Json.object(m_type, m_double, m_coerce, false);
	private static final ObjectNode GEOPOINT = //
			Json.object(m_type, "geo_point", "lat_lon", true, "geohash", true, //
					"geohash_precision", "1m", "geohash_prefix", true);

	public static ObjectNode translate(String type, JsonNode schema) {

		ObjectNode subMapping = toElasticMapping(schema.get(type));
		subMapping.set("_meta", schema);

		return Json.builder().object().add(type, subMapping).build();
	}

	private static ObjectNode toElasticMapping(JsonNode schema) {

		String type = schema.path(s_type).asText(m_object);

		String defaultLanguage = language(schema, m_french);
		ObjectNode propertiesNode = toElasticProperties(schema, defaultLanguage);
		addMetadataFields(propertiesNode);

		if (m_object.equals(type))
			return Json.builder().object()//
					.add(m_dynamic, m_strict)//
					.add(m_date_detection, false)//
					.add(m_properties, propertiesNode)//
					.object(m_all)//
					.add(m_analyzer, defaultLanguage)//
					.build();

		throw Exceptions.illegalArgument("invalid schema root type [%s]", type);
	}

	private static void addMetadataFields(ObjectNode propertiesNode) {
		propertiesNode.set(OWNER_FIELD, KEYWORD);
		propertiesNode.set(GROUP_FIELD, KEYWORD);
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
		return schema.path(s_language).asText(defaultLanguage);
	}

	private static ObjectNode toElasticProperty(String key, JsonNode schema, String defaultLanguage) {
		String type = schema.path(s_type).asText(m_object);
		if (s_text.equals(type))
			return Json.object(m_type, m_text, m_index, m_analyzed, //
					m_analyzer, language(schema, defaultLanguage));
		else if (s_string.equals(type))
			return KEYWORD;
		else if (s_boolean.equals(type))
			return BOOLEAN;
		else if (s_integer.equals(type))
			return INTEGER;
		else if (s_long.equals(type))
			return LONG;
		else if (s_float.equals(type))
			return FLOAT;
		else if (s_double.equals(type))
			return DOUBLE;
		else if (s_date.equals(type))
			return DATE;
		else if (s_time.equals(type))
			return TIME;
		else if (s_timestamp.equals(type))
			return TIMESTAMP;
		else if (s_enum.equals(type))
			return KEYWORD;
		else if (s_geopoint.equals(type))
			return GEOPOINT;
		else if (m_object.equals(type))
			return Json.object(m_type, m_object, m_properties, //
					toElasticProperties(schema, language(schema, defaultLanguage)));
		else if (s_stash.equals(type))
			return STASH;

		throw Exceptions.illegalArgument("invalid type [%s] for property [%s]", type, key);
	}
}
