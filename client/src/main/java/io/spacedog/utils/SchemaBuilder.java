/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Arrays;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.Permission;
import io.spacedog.model.Schema;
import io.spacedog.model.Schema.DataAcl;

public class SchemaBuilder {

	public static final String _LABELS = "_labels";
	public static final String _ENUM_TYPE = "_enumType";
	public static final String _EXTRA = "_extra";
	public static final String _EXAMPLES = "_examples";
	public static final String _VALUES = "_values";
	public static final String _REQUIRED = "_required";
	public static final String _ARRAY = "_array";
	public static final String _ACL = "_acl";
	public static final String _LANGUAGE = "_language";
	public static final String _REF_TYPE = "_ref_type";

	public static SchemaBuilder builder(String type) {
		return new SchemaBuilder(type);
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

	public SchemaBuilder object(String key) {
		return property(key, SchemaType.OBJECT);
	}

	public SchemaBuilder close() {
		currentPropertyType = null;
		builder.end();
		return this;
	}

	public Schema build() {
		ObjectNode node = builder.build();
		if (acl != null)
			node.with(name).set(_ACL, Json.mapper().valueToTree(acl));
		return new Schema(name, node);
	}

	@Override
	public String toString() {
		return build().toString();
	}

	public SchemaBuilder acl(String role, Permission... permissions) {
		if (acl == null)
			acl = new DataAcl();

		acl.set(role, permissions);
		return this;
	}

	public SchemaBuilder array() {
		checkCurrentPropertyExists();
		checkCurrentPropertyByInvalidTypes(_ARRAY, SchemaType.STASH);
		builder.add(_ARRAY, true);
		return this;
	}

	public SchemaBuilder required() {
		checkCurrentPropertyExists();
		builder.add(_REQUIRED, true);
		return this;
	}

	public SchemaBuilder values(Object... values) {
		checkCurrentPropertyExists();
		builder.array(_VALUES).add(values).end();
		return this;
	}

	public SchemaBuilder examples(Object... examples) {
		checkCurrentPropertyExists();
		builder.array(_EXAMPLES).add(examples).end();
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
		checkCurrentPropertyByValidType(_LANGUAGE, SchemaType.TEXT);
		builder.add(_LANGUAGE, language);
		return this;
	}

	public SchemaBuilder refType(String type) {
		checkCurrentPropertyExists();
		checkCurrentPropertyByValidType(_REF_TYPE, SchemaType.STRING);
		builder.add(_REF_TYPE, type);
		return this;
	}

	public SchemaBuilder extra(Object... extra) {
		return extra(Json.object(extra));
	}

	public SchemaBuilder extra(ObjectNode extra) {
		builder.add(_EXTRA, extra);
		return this;
	}

	public SchemaBuilder enumType(String type) {
		builder.add(_ENUM_TYPE, type);
		return this;
	}

	public SchemaBuilder labels(String... labels) {
		builder.add(_LABELS, Json.object(//
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

	private String name;
	private JsonBuilder<ObjectNode> builder;
	private DataAcl acl;
	private SchemaType currentPropertyType = SchemaType.OBJECT;

	private SchemaBuilder(String name) {
		this.name = name;
		this.builder = Json.builder().object().object(name);
	}

	private SchemaBuilder property(String key, SchemaType type) {
		if (currentPropertyType != SchemaType.OBJECT)
			builder.end();

		currentPropertyType = type;
		builder.object(key).add("_type", type.toString());
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
