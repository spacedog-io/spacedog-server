/**
 * © David Attias 2015
 */
package io.spacedog.model;

import java.util.Arrays;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;

import io.spacedog.client.http.SpaceException;
import io.spacedog.model.SchemaBuilder.SchemaType;
import io.spacedog.utils.Json;
import io.spacedog.utils.Optional7;

public class SchemaValidator implements SchemaDirectives {

	private static enum JsonType {
		OBJECT, ARRAY, BOOLEAN, STRING, NUMBER
	}

	public static JsonNode validate(String type, JsonNode schema) throws SchemaException {

		JsonNode rootObjectSchema = checkField(schema, type, true, JsonType.OBJECT).get();

		checkIfInvalidField(schema, false, type);

		String rootType = checkField(rootObjectSchema, s_type, false, JsonType.STRING)//
				.orElse(TextNode.valueOf("object")).asText();

		if (SchemaType.OBJECT.equals(rootType)) {
			checkCommonDirectives(rootObjectSchema);

			Optional7<JsonNode> opt = checkField(rootObjectSchema, s_acl, false, JsonType.OBJECT);
			if (opt.isPresent())
				checkAcl(type, opt.get());

			checkObjectProperties(type, rootObjectSchema);
		} else
			throw SchemaException.invalidType(type, rootType);

		return schema;
	}

	private static void checkAcl(String type, JsonNode json) throws SchemaException {
		// TODO implement this
	}

	private static void checkObjectProperties(String propertyName, JsonNode propertySchema) {

		boolean noFields = true;
		Iterator<String> names = propertySchema.fieldNames();

		while (names.hasNext()) {
			String name = names.next();

			if (Strings.isNullOrEmpty(name))
				throw new SchemaException(//
						"schema object field [%s] contains empty field names", propertyName);

			if (name.charAt(0) != '_') {
				noFields = false;
				checkProperty(name, propertySchema.get(name));
			}
		}

		if (noFields)
			throw SchemaException.noFields(propertyName);
	}

	private static void checkProperty(String propertyName, JsonNode propertySchema) throws SchemaException {

		if (!propertySchema.isObject())
			throw new SchemaException("invalid schema [%s] for field [%s]", //
					propertySchema, propertyName);

		Optional7<JsonNode> optional = checkField(propertySchema, s_type, false, JsonType.STRING);
		String propertyType = optional.isPresent() ? optional.get().asText() : "object";

		if (!SchemaType.isValid(propertyType))
			throw new SchemaException("invalid type [%s] for field [%s]", propertyType, propertyName);

		checkCommonDirectives(propertySchema);

		if (SchemaType.OBJECT.equals(propertyType))
			checkObjectProperties(propertyName, propertySchema);
		else
			checkNoFields(propertyName, propertySchema);

	}

	private static void checkCommonDirectives(JsonNode propertySchema) {
		checkField(propertySchema, s_required, false, JsonType.BOOLEAN);
		checkField(propertySchema, s_array, false, JsonType.BOOLEAN);
		checkField(propertySchema, s_extra, false, JsonType.OBJECT);
		checkField(propertySchema, s_values, false, JsonType.ARRAY);
		checkField(propertySchema, s_examples, false, JsonType.ARRAY);
		checkField(propertySchema, s_language, false, JsonType.STRING);
		checkField(propertySchema, "_gt", false, JsonType.NUMBER);
		checkField(propertySchema, "_gte", false, JsonType.NUMBER);
		checkField(propertySchema, "_lt", false, JsonType.NUMBER);
		checkField(propertySchema, "_lte", false, JsonType.NUMBER);
		checkField(propertySchema, "_pattern", false, JsonType.STRING);
		checkField(propertySchema, s_labels, false, JsonType.OBJECT);
		checkField(propertySchema, s_ref_type, false, JsonType.STRING);
	}

	private static void checkNoFields(String propertyName, JsonNode propertySchema) throws SchemaException {
		Iterator<String> fieldNames = propertySchema.fieldNames();
		while (fieldNames.hasNext()) {
			String fieldName = fieldNames.next();
			if (fieldName.charAt(0) != '_')
				throw SchemaException.invalidField(fieldName);
		}

		// Iterable<String> fieldNames = () -> json.fieldNames();
		// StreamSupport.stream(fieldNames.spliterator(), false)//
		// .filter(name -> name.charAt(0) != '_')//
		// .findAny()//
		// .ifPresent(name -> {
		// throw SchemaException.invalidField(name);
		// });
	}

	private static void checkIfInvalidField(JsonNode json, boolean checkSettingsOnly, String... validFieldNames) {

		Iterator<String> fieldNames = json.fieldNames();

		whileLoop: while (fieldNames.hasNext()) {
			String fieldName = fieldNames.next();
			if (checkSettingsOnly && fieldName.charAt(0) != ('_'))
				continue whileLoop;
			for (String validName : validFieldNames) {
				if (fieldName.equals(validName))
					continue whileLoop;
			}
			throw SchemaException.invalidField(fieldName, validFieldNames);
		}

		// Iterable<String> fieldNames = () -> json.fieldNames();
		// StreamSupport.stream(fieldNames.spliterator(), false)
		// .filter(name -> checkSettingsOnly ? name.charAt(0) == ('_') : true)//
		// .filter(name -> {
		// for (String validName : validFieldNames) {
		// if (name.equals(validName))
		// return false;
		// }
		// return true;
		// })//
		// .findFirst()//
		// .ifPresent(name -> {
		// throw SchemaException.invalidField(name, validFieldNames);
		// });
	}

	private static Optional7<JsonNode> checkField(JsonNode jsonObject, String fieldName, boolean required,
			JsonType fieldType) throws SchemaException {

		JsonNode fieldValue = jsonObject.get(fieldName);

		if (Json.isNull(fieldValue))
			if (required)
				throw new SchemaException("field [%s] required", fieldName);
			else
				return Optional7.empty();

		if ((fieldValue.isObject() && fieldType == JsonType.OBJECT)
				|| (fieldValue.isTextual() && fieldType == JsonType.STRING)
				|| (fieldValue.isArray() && fieldType == JsonType.ARRAY)
				|| (fieldValue.isBoolean() && fieldType == JsonType.BOOLEAN)
				|| (fieldValue.isNumber() && fieldType == JsonType.NUMBER))
			return Optional7.of(fieldValue);

		throw new SchemaException("invalid type [%s] for schema field [%s]. Must be [%s]", //
				getJsonType(fieldValue), fieldName, fieldType);
	}

	private static String getJsonType(JsonNode value) {
		return value.isTextual() ? "string"
				: value.isObject() ? "object"
						: value.isNumber() ? "number"
								: value.isArray() ? "array" : value.isBoolean() ? "boolean" : "null";
	}

	public static class SchemaException extends SpaceException {

		private static final long serialVersionUID = 6335047694807220133L;

		public SchemaException(String message, Object... args) {
			super(400, message, args);
		}

		private static SchemaException invalidField(String fieldName, String... expectedFiedNames) {
			return new SchemaException("invalid field [%s]: expected fields %s", fieldName,
					Arrays.toString(expectedFiedNames));
		}

		private static SchemaException invalidType(String schemaName, String type) {
			return new SchemaException("invalid schema type [%s]", type);
		}

		private static SchemaException noFields(String fieldName) {
			return new SchemaException("no fields in sub object [%s]", fieldName);
		}
	}

}
