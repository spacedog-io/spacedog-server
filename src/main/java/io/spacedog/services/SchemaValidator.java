package io.spacedog.services;

import java.util.Arrays;
import java.util.Optional;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class SchemaValidator {

	private static enum JsonType {
		OBJECT, ARRAY, BOOLEAN, STRING, NUMBER
	}

	public static JsonObject validate(String type, JsonObject schema)
			throws InvalidSchemaException {

		JsonObject rootObject = checkField(schema, type, true, JsonType.OBJECT)
				.get().asObject();

		checkIfInvalidField(schema, false, type);

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
			checkIfInvalidField(rootObject, true, "_acl", "_id");
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
		checkField(json, "_array", false, JsonType.BOOLEAN);
		checkIfInvalidField(json, true, "_type", "_required", "_array");
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
			checkSimpleProperty(jsonObject, propertyName, type);
		else if (type.equals("date"))
			checkSimpleProperty(jsonObject, propertyName, type);
		else if (type.equals("time"))
			checkSimpleProperty(jsonObject, propertyName, type);
		else if (type.equals("timestamp"))
			checkSimpleProperty(jsonObject, propertyName, type);
		else if (type.equals("integer"))
			checkSimpleProperty(jsonObject, propertyName, type);
		else if (type.equals("long"))
			checkSimpleProperty(jsonObject, propertyName, type);
		else if (type.equals("float"))
			checkSimpleProperty(jsonObject, propertyName, type);
		else if (type.equals("double"))
			checkSimpleProperty(jsonObject, propertyName, type);
		else if (type.equals("boolean"))
			checkSimpleProperty(jsonObject, propertyName, type);
		else if (type.equals("object"))
			checkObjectProperty(propertyName, jsonObject);
		else if (type.equals("enum"))
			checkEnumProperty(propertyName, jsonObject);
		else if (type.equals("geopoint"))
			checkSimpleProperty(jsonObject, propertyName, type);
		else if (type.equals("stash"))
			checkStashProperty(propertyName, jsonObject);
		else
			throw new InvalidSchemaException("Invalid field type: " + type);
	}

	private static void checkSimpleProperty(JsonObject json,
			String propertyName, String propertyType)
			throws InvalidSchemaException {
		checkField(json, "_required", false, JsonType.BOOLEAN);
		checkField(json, "_array", false, JsonType.BOOLEAN);
		checkIfInvalidField(json, false, "_type", "_required", "_array");
	}

	private static void checkStashProperty(String type, JsonObject json) {
		checkField(json, "_required", false, JsonType.BOOLEAN);
		checkIfInvalidField(json, false, "_type", "_required");
	}

	private static void checkEnumProperty(String propertyName, JsonObject json)
			throws InvalidSchemaException {
		checkField(json, "_required", false, JsonType.BOOLEAN);
		checkField(json, "_array", false, JsonType.BOOLEAN);
		checkIfInvalidField(json, false, "_type", "_required", "_array");
	}

	private static void checkTextProperty(String propertyName, JsonObject json)
			throws InvalidSchemaException {
		checkIfInvalidField(json, false, "_type", "_required", "_language",
				"_array");
		checkField(json, "_required", false, JsonType.BOOLEAN);
		checkField(json, "_language", false, JsonType.STRING);
		checkField(json, "_array", false, JsonType.BOOLEAN);
	}

	private static void checkIfInvalidField(JsonObject json,
			boolean checkSettingsOnly, String... validFieldNames) {
		json.names()
				.stream()
				.filter(name -> checkSettingsOnly ? name.charAt(0) == ('_')
						: true)
				.filter(name -> {
					for (String validName : validFieldNames) {
						if (name.equals(validName))
							return false;
					}
					return true;
				})
				.findFirst()
				.ifPresent(
						name -> {
							throw InvalidSchemaException.invalidField(name,
									validFieldNames);
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

		public static InvalidSchemaException invalidField(String fieldName,
				String... expectedFiedNames) {
			return new InvalidSchemaException(String.format(
					"invalid field [%s]: expected fields are %s", fieldName,
					Arrays.toString(expectedFiedNames)));
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
