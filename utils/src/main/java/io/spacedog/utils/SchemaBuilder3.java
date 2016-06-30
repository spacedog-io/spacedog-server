/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Arrays;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class SchemaBuilder3 {

	public static SchemaBuilder3 builder(String type) {
		return new SchemaBuilder3(Json.objectBuilder().object(type));
	}

	public SchemaBuilder3 object(String key) {
		return property(key, SchemaType.OBJECT);
	}

	public SchemaBuilder3 enumm(String key) {
		return property(key, SchemaType.ENUM);
	}

	public SchemaBuilder3 string(String key) {
		return property(key, SchemaType.STRING);
	}

	public SchemaBuilder3 text(String key) {
		return property(key, SchemaType.TEXT);
	}

	public SchemaBuilder3 bool(String key) {
		return property(key, SchemaType.BOOLEAN);
	}

	public SchemaBuilder3 integer(String key) {
		return property(key, SchemaType.INTEGER);
	}

	public SchemaBuilder3 decimal(String key) {
		return property(key, SchemaType.DECIMAL);
	}

	public SchemaBuilder3 geopoint(String key) {
		return property(key, SchemaType.GEOPOINT);
	}

	public SchemaBuilder3 date(String key) {
		return property(key, SchemaType.DATE);
	}

	public SchemaBuilder3 time(String key) {
		return property(key, SchemaType.TIME);
	}

	public SchemaBuilder3 timestamp(String key) {
		return property(key, SchemaType.TIMESTAMP);
	}

	public SchemaBuilder3 stash(String key) {
		return property(key, SchemaType.STASH);
	}

	public SchemaBuilder3 close() {
		currentPropertyType = null;
		builder.end();
		return this;
	}

	public ObjectNode build() {
		return builder.build();
	}

	@Override
	public String toString() {
		return builder.build().toString();
	}

	public SchemaBuilder3 id(String key) {
		builder.put("_id", key);
		return this;
	}

	public SchemaBuilder3 array() {
		checkCurrentPropertyExists();
		checkCurrentPropertyByInvalidTypes("_array", SchemaType.STASH);
		builder.put("_array", true);
		return this;
	}

	public SchemaBuilder3 required() {
		checkCurrentPropertyExists();
		builder.put("_required", true);
		return this;
	}

	public SchemaBuilder3 values(Object... values) {
		checkCurrentPropertyExists();
		builder.array("_values").addAll(Arrays.asList(values)).end();
		return this;
	}

	public SchemaBuilder3 examples(Object... examples) {
		checkCurrentPropertyExists();
		builder.array("_examples").addAll(Arrays.asList(examples)).end();
		return this;
	}

	public SchemaBuilder3 french() {
		return language("french");
	}

	public SchemaBuilder3 language(String language) {
		checkCurrentPropertyExists();
		checkCurrentPropertyByValidType("_language", SchemaType.TEXT);
		builder.put("_language", language);
		return this;
	}

	public SchemaBuilder3 extra(Object... extra) {
		return extra(Json.object(extra));
	}

	public SchemaBuilder3 extra(ObjectNode extra) {
		builder.node("_extra", extra);
		return this;
	}

	public SchemaBuilder3 enumType(String type) {
		builder.put("_enumType", type);
		return this;
	}

	public SchemaBuilder3 labels(String... labels) {
		builder.node("_label", Json.object(//
				Arrays.copyOf(labels, labels.length, Object[].class)));
		return this;
	}

	//
	// Implementation
	//

	private JsonBuilder<ObjectNode> builder;
	private SchemaType currentPropertyType = SchemaType.OBJECT;

	private SchemaBuilder3(JsonBuilder<ObjectNode> builder) {
		this.builder = builder;
	}

	private SchemaBuilder3 property(String key, SchemaType type) {
		if (currentPropertyType != SchemaType.OBJECT)
			builder.end();

		currentPropertyType = type;
		builder.object(key).put("_type", type.toString());
		return this;
	}

	private void checkCurrentPropertyExists() {
		if (currentPropertyType == null)
			throw Exceptions.illegalState("no current property in [%s]", builder);
	}

	private void checkCurrentPropertyByValidType(String fieldName, SchemaType... validTypes) {

		for (SchemaType validType : validTypes)
			if (currentPropertyType.equals(validType))
				return;

		throw Exceptions.illegalState("invalid property type [%s] for this field [%s]", //
				currentPropertyType, fieldName);
	}

	private void checkCurrentPropertyByInvalidTypes(String fieldName, SchemaType... invalidTypes) {

		for (SchemaType invalidType : invalidTypes)
			if (currentPropertyType.equals(invalidType))
				throw Exceptions.illegalState("invalid property type [%s] for this field [%s]", //
						currentPropertyType, fieldName);
	}
}
