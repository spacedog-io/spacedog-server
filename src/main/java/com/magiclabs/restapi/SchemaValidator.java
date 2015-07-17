package com.magiclabs.restapi;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.elasticsearch.common.collect.Lists;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class SchemaValidator {

	private static enum JsonType {
		OBJECT, ARRAY, BOOLEAN, STRING, NUMBER
	}

	private static enum MagicTypes {
		OBJECT, ARRAY, TEXT, CODE, BOOLEAN, GEOPOINT, NUMBER, DATE, TIME, TIMESTAMP, ENUM, STASH
	}

	public static JsonObject validate(String type, JsonObject schema)
			throws InvalidSchemaException {

		JsonObject rootObject = checkField(schema, type, true, JsonType.OBJECT)
				.get().asObject();

		checkAllFieldsAreValid(schema, Collections.singletonList(type));

		String rootType = checkField(rootObject, "_type", false,
				JsonType.STRING).orElse(JsonValue.valueOf("object")).asString();

		// if (rootType.equals("stash")) {
		// checkStashProperty(type, rootObject);
		// } else

		if (rootType.equals("object")) {
			checkField(rootObject, "_id", false, JsonType.STRING);
			Optional<JsonValue> opt = checkField(rootObject, "_acl", false,
					JsonType.OBJECT);
			if (opt.isPresent()) {
				checkAcl(type, opt.get().asObject());
			}
			checkAllSettingsAreValid(rootObject,
					Lists.newArrayList("_acl", "_id"));
			checkObjectProperties(type, rootObject);
		} else
			throw InvalidSchemaException.invalidObjectType(type, rootType);

		return schema;
	}

	private static void checkAcl(String type, JsonObject json)
			throws InvalidSchemaException {
		// TODO implement this
	}

	private static void checkObjectProperties(String propertyName,
			JsonObject json) {
		json.names()
				.stream()
				.filter(name -> name.charAt(0) != '_')
				.findFirst()
				.orElseThrow(
						() -> InvalidSchemaException.noProperty(propertyName));

		json.names().stream().filter(name -> name.charAt(0) != '_')
				.forEach(name -> checkProperty(name, json.get(name)));
	}

	private static void checkObjectProperty(String propertyName, JsonObject json)
			throws InvalidSchemaException {
		checkField(json, "_type", false, JsonType.STRING,
				JsonValue.valueOf("object"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));

		checkObjectProperties(propertyName, json);
	}

	private static void checkProperty(String propertyName, JsonValue jsonValue)
			throws InvalidSchemaException {

		if (!jsonValue.isObject())
			throw new InvalidSchemaException(String.format(
					"invalid value [%s] for object property [%s]", jsonValue,
					propertyName));

		JsonObject jsonObject = jsonValue.asObject();
		Optional<JsonValue> optType = checkField(jsonObject, "_type", false,
				JsonType.STRING);
		String type = optType.isPresent() ? optType.get().asString() : "object";

		if (type.equals("text"))
			checkTextProperty(propertyName, jsonObject);
		else if (type.equals("string"))
			checkCodeProperty(propertyName, jsonObject);
		else if (type.equals("date"))
			checkDateProperty(propertyName, jsonObject);
		else if (type.equals("time"))
			checkTimeProperty(propertyName, jsonObject);
		else if (type.equals("timestamp"))
			checkTimestampProperty(propertyName, jsonObject);
		else if (type.equals("integer"))
			checkIntegerProperty(propertyName, jsonObject);
		else if (type.equals("long"))
			checkLongProperty(propertyName, jsonObject);
		else if (type.equals("float"))
			checkFloatProperty(propertyName, jsonObject);
		else if (type.equals("double"))
			checkDoubleProperty(propertyName, jsonObject);
		else if (type.equals("boolean"))
			checkBooleanProperty(propertyName, jsonObject);
		else if (type.equals("object"))
			checkObjectProperty(propertyName, jsonObject);
		else if (type.equals("array"))
			checkArrayProperty(propertyName, jsonObject);
		else if (type.equals("enum"))
			checkEnumProperty(propertyName, jsonObject);
		else if (type.equals("geopoint"))
			checkGeoPointProperty(propertyName, jsonObject);
		else if (type.equals("stash"))
			checkStashProperty(propertyName, jsonObject);
		else
			throw new InvalidSchemaException("Invalid field type: " + type);
	}

	private static void checkStashProperty(String type, JsonObject json) {
		checkNoProperties(type, "stash", json);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
	}

	private static void checkGeoPointProperty(String propertyName,
			JsonObject json) throws InvalidSchemaException {
		checkNoProperties(propertyName, "geopoint", json);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
	}

	private static void checkEnumProperty(String propertyName, JsonObject json)
			throws InvalidSchemaException {
		checkNoProperties(propertyName, "enum", json);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
	}

	private static void checkCodeProperty(String propertyName, JsonObject json)
			throws InvalidSchemaException {
		checkNoProperties(propertyName, "code", json);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
	}

	private static void checkTextProperty(String propertyName, JsonObject json)
			throws InvalidSchemaException {
		checkNoProperties(propertyName, "text", json);
		checkAllSettingsAreValid(json,
				Lists.newArrayList("_type", "_required", "_language"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
		checkField(json, "_language", false, JsonType.STRING);
	}

	private static void checkArrayProperty(String propertyName, JsonObject json)
			throws InvalidSchemaException {
		checkNoProperties(propertyName, "array", json);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
	}

	private static void checkBooleanProperty(String propertyName,
			JsonObject json) throws InvalidSchemaException {
		checkNoProperties(propertyName, "boolean", json);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
	}

	private static void checkIntegerProperty(String propertyName,
			JsonObject json) throws InvalidSchemaException {
		checkNoProperties(propertyName, "integer", json);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
	}

	private static void checkLongProperty(String propertyName, JsonObject json)
			throws InvalidSchemaException {
		checkNoProperties(propertyName, "long", json);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
	}

	private static void checkFloatProperty(String propertyName, JsonObject json)
			throws InvalidSchemaException {
		checkNoProperties(propertyName, "float", json);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
	}

	private static void checkDoubleProperty(String propertyName, JsonObject json)
			throws InvalidSchemaException {
		checkNoProperties(propertyName, "double", json);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
	}

	private static void checkTimestampProperty(String propertyName,
			JsonObject json) throws InvalidSchemaException {
		checkNoProperties(propertyName, "timestamp", json);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
	}

	private static void checkTimeProperty(String propertyName, JsonObject json)
			throws InvalidSchemaException {
		checkNoProperties(propertyName, "time", json);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
	}

	private static void checkDateProperty(String propertyName, JsonObject json)
			throws InvalidSchemaException {
		checkNoProperties(propertyName, "date", json);
		checkAllSettingsAreValid(json, Lists.newArrayList("_type", "_required"));
		checkField(json, "_required", false, JsonType.BOOLEAN);
	}

	private static void checkAllFieldsAreValid(JsonObject json,
			List<String> validFieldNames) {
		json.names()
				.stream()
				.filter(name -> !validFieldNames.contains(name))
				.findFirst()
				.ifPresent(
						name -> {
							throw new InvalidSchemaException(String.format(
									"invalid field [%s]", name));
						});
	}

	private static void checkNoProperties(String propertyName,
			String propertyType, JsonObject json) {
		json.names()
				.stream()
				.filter(name -> name.charAt(0) != ('_'))
				.findAny()
				.ifPresent(
						fieldName -> {
							throw InvalidSchemaException.noProperties(
									fieldName, propertyName, propertyType);
						});
	}

	private static void checkAllSettingsAreValid(JsonObject json,
			List<String> validSettingNames) {
		json.names()
				.stream()
				.filter(name -> name.charAt(0) == ('_'))
				.filter(name -> !validSettingNames.contains(name))
				.findFirst()
				.ifPresent(
						name -> {
							throw new InvalidSchemaException(String.format(
									"invalid property setting [%s]", name));
						});
	}

	private static void checkField(JsonObject jsonObject, String fieldName,
			boolean required, JsonType fieldType,
			JsonValue anticipatedFieldValue) throws InvalidSchemaException {

		checkField(jsonObject, fieldName, required, fieldType) //
				.ifPresent(
						fieldValue -> {
							if (!fieldValue.equals(anticipatedFieldValue))
								throw InvalidSchemaException.invalidFieldValue(
										fieldName, fieldValue,
										anticipatedFieldValue);
						});
	}

	private static Optional<JsonValue> checkField(JsonObject jsonObject,
			String fieldName, boolean required, JsonType fieldType)
			throws InvalidSchemaException {

		JsonValue fieldValue = jsonObject.get(fieldName);

		if (fieldValue == null)
			if (required)
				throw new InvalidSchemaException(
						"This schema field is required: " + fieldName);
			else
				return Optional.empty();

		if ((fieldValue.isObject() && fieldType == JsonType.OBJECT)
				|| (fieldValue.isString() && fieldType == JsonType.STRING)
				|| (fieldValue.isArray() && fieldType == JsonType.ARRAY)
				|| (fieldValue.isBoolean() && fieldType == JsonType.BOOLEAN)
				|| (fieldValue.isNumber() && fieldType == JsonType.NUMBER))
			return Optional.of(fieldValue);

		throw new InvalidSchemaException(String.format(
				"Invalid type [%s] for schema field [%s]. Must be [%s]",
				getJsonType(fieldValue), fieldName, fieldType));
	}

	private static String getJsonType(JsonValue value) {
		return value.isString() ? "string" : value.isObject() ? "object"
				: value.isNumber() ? "number" : value.isArray() ? "array"
						: value.isBoolean() ? "boolean" : "null";
	}

	public static class InvalidSchemaException extends RuntimeException {

		private static final long serialVersionUID = 6335047694807220133L;

		public InvalidSchemaException(String message) {
			super(message);
		}

		public static InvalidSchemaException noProperties(String fieldName,
				String propertyName, String propertyType) {
			return new InvalidSchemaException(
					String.format(
							"invalid field [%s], property [%s] of type [%s] should not have any nested properties",
							fieldName, propertyName, propertyType));
		}

		public static InvalidSchemaException invalidObjectType(String type,
				String rootType) {
			return new InvalidSchemaException(String.format(
					"invalid root object type [%s]", rootType));
		}

		public static InvalidSchemaException invalidFieldValue(
				String fieldName, JsonValue fieldValue,
				JsonValue anticipatedFieldValue) {
			return new InvalidSchemaException(String.format(
					"schema field [%s] equal to [%s] should be equal to [%s]",
					fieldName, fieldValue, anticipatedFieldValue));
		}

		public static InvalidSchemaException noProperty(String propertyName) {
			return new InvalidSchemaException(String.format(
					"property [%s] of type [object] has no properties",
					propertyName));
		}
	}

}
