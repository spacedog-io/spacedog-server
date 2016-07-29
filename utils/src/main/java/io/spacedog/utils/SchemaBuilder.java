/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Arrays;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class SchemaBuilder {

	public static SchemaBuilder builder(String type) {
		return new SchemaBuilder(Json.objectBuilder().object(type));
	}

	public SchemaBuilder object(String key) {
		return property(key, SchemaType.OBJECT);
	}

	public SchemaBuilder enumm(String key) {
		return property(key, SchemaType.ENUM);
	}

	public SchemaBuilder string(String key) {
		return property(key, SchemaType.STRING);
	}

	public SchemaBuilder text(String key) {
		return property(key, SchemaType.TEXT);
	}

	public SchemaBuilder bool(String key) {
		return property(key, SchemaType.BOOLEAN);
	}

	public SchemaBuilder integer(String key) {
		return property(key, SchemaType.INTEGER);
	}

	public SchemaBuilder longg(String key) {
		return property(key, SchemaType.LONG);
	}

	public SchemaBuilder floatt(String key) {
		return property(key, SchemaType.FLOAT);
	}

	public SchemaBuilder doublee(String key) {
		return property(key, SchemaType.DOUBLE);
	}

	public SchemaBuilder geopoint(String key) {
		return property(key, SchemaType.GEOPOINT);
	}

	public SchemaBuilder date(String key) {
		return property(key, SchemaType.DATE);
	}

	public SchemaBuilder time(String key) {
		return property(key, SchemaType.TIME);
	}

	public SchemaBuilder timestamp(String key) {
		return property(key, SchemaType.TIMESTAMP);
	}

	public SchemaBuilder stash(String key) {
		return property(key, SchemaType.STASH);
	}

	public SchemaBuilder close() {
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

	public SchemaBuilder id(String key) {
		builder.put("_id", key);
		return this;
	}

	public SchemaBuilder array() {
		checkCurrentPropertyExists();
		checkCurrentPropertyByInvalidTypes("_array", SchemaType.STASH);
		builder.put("_array", true);
		return this;
	}

	public SchemaBuilder required() {
		checkCurrentPropertyExists();
		builder.put("_required", true);
		return this;
	}

	public SchemaBuilder values(Object... values) {
		checkCurrentPropertyExists();
		builder.array("_values").addAll(Arrays.asList(values)).end();
		return this;
	}

	public SchemaBuilder examples(Object... examples) {
		checkCurrentPropertyExists();
		builder.array("_examples").addAll(Arrays.asList(examples)).end();
		return this;
	}

	public SchemaBuilder french() {
		return language("french");
	}

	public SchemaBuilder english() {
		return language("english");
	}

	public SchemaBuilder language(String language) {
		checkCurrentPropertyExists();
		checkCurrentPropertyByValidType("_language", SchemaType.TEXT);
		builder.put("_language", language);
		return this;
	}

	public SchemaBuilder extra(Object... extra) {
		return extra(Json.object(extra));
	}

	public SchemaBuilder extra(ObjectNode extra) {
		builder.node("_extra", extra);
		return this;
	}

	public SchemaBuilder enumType(String type) {
		builder.put("_enumType", type);
		return this;
	}

	public SchemaBuilder labels(String... labels) {
		builder.node("_label", Json.object(//
				Arrays.copyOf(labels, labels.length, Object[].class)));
		return this;
	}

	public static enum SchemaType {

		OBJECT, TEXT, STRING, BOOLEAN, GEOPOINT, INTEGER, FLOAT, LONG, //
		DOUBLE, DATE, TIME, TIMESTAMP, ENUM, STASH;

		@Override
		public String toString() {
			return name().toLowerCase();
		}

		public boolean equals(String string) {
			return this.toString().equals(string);
		}

		public static SchemaType valueOfNoCase(String value) {
			return valueOf(value.toUpperCase());
		}

		public static boolean isValid(String value) {
			try {
				valueOfNoCase(value);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}

	//
	// Implementation
	//

	private JsonBuilder<ObjectNode> builder;
	private SchemaType currentPropertyType = SchemaType.OBJECT;

	private SchemaBuilder(JsonBuilder<ObjectNode> builder) {
		this.builder = builder;
	}

	private SchemaBuilder property(String key, SchemaType type) {
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
