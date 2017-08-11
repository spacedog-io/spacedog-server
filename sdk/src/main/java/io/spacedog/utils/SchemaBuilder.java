/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Arrays;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.DataPermission;
import io.spacedog.model.Schema;
import io.spacedog.model.Schema.SchemaAcl;

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
			node.with(name).set(_ACL, Json7.mapper().valueToTree(acl));
		return new Schema(name, node);
	}

	@Override
	public String toString() {
		return build().toString();
	}

	public SchemaBuilder acl(String role, DataPermission... permissions) {
		if (acl == null)
			acl = new SchemaAcl();

		acl.set(role, permissions);
		return this;
	}

	public SchemaBuilder array() {
		checkCurrentPropertyExists();
		checkCurrentPropertyByInvalidTypes(_ARRAY, SchemaType.STASH);
		builder.put(_ARRAY, true);
		return this;
	}

	public SchemaBuilder required() {
		checkCurrentPropertyExists();
		builder.put(_REQUIRED, true);
		return this;
	}

	public SchemaBuilder values(Object... values) {
		checkCurrentPropertyExists();
		builder.array(_VALUES).addAll(Arrays.asList(values)).end();
		return this;
	}

	public SchemaBuilder examples(Object... examples) {
		checkCurrentPropertyExists();
		builder.array(_EXAMPLES).addAll(Arrays.asList(examples)).end();
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
		builder.put(_LANGUAGE, language);
		return this;
	}

	public SchemaBuilder refType(String type) {
		checkCurrentPropertyExists();
		checkCurrentPropertyByValidType(_REF_TYPE, SchemaType.STRING);
		builder.put(_REF_TYPE, type);
		return this;
	}

	public SchemaBuilder extra(Object... extra) {
		return extra(Json7.object(extra));
	}

	public SchemaBuilder extra(ObjectNode extra) {
		builder.node(_EXTRA, extra);
		return this;
	}

	public SchemaBuilder enumType(String type) {
		builder.put(_ENUM_TYPE, type);
		return this;
	}

	public SchemaBuilder labels(String... labels) {
		builder.node(_LABELS, Json7.object(//
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
	private SchemaAcl acl;
	private SchemaType currentPropertyType = SchemaType.OBJECT;

	private SchemaBuilder(String name) {
		this.name = name;
		this.builder = Json7.objectBuilder().object(name);
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
