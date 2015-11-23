/**
 * © David Attias 2015
 */
package io.spacedog.services;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class SchemaBuilder2 {

	public static SchemaBuilder2 builder(String type) {
		return new SchemaBuilder2(type, null);
	}

	public static SchemaBuilder2 builder(String type, String idPropertyPath) {
		return new SchemaBuilder2(type, idPropertyPath);
	}

	private SchemaBuilder builder;

	private SchemaBuilder2(String type, String idPropertyPath) {
		builder = SchemaBuilder.builder(type);
		if (idPropertyPath != null)
			builder.id(idPropertyPath);
	}

	public SchemaBuilder2 stringProperty(String key, boolean required) {
		return stringProperty(key, required, false);
	}

	public SchemaBuilder2 stringProperty(String key, boolean required, boolean array) {
		return simpleProperty(key, "string", required, array);
	}

	public SchemaBuilder2 simpleProperty(String key, String type, boolean required) {
		return simpleProperty(key, type, required, false);
	}

	public SchemaBuilder2 simpleProperty(String key, String type, boolean required, boolean array) {
		builder.property(key, type);
		if (required)
			builder.required();
		if (array)
			builder.array();
		builder.end();
		return this;
	}

	public SchemaBuilder2 textProperty(String key, String language, boolean required) {
		return textProperty(key, language, required, false);
	}

	public SchemaBuilder2 textProperty(String key, String language, boolean required, boolean array) {
		builder.property(key, "text").language(language);
		if (required)
			builder.required();
		if (array)
			builder.array();
		builder.end();
		return this;
	}

	public SchemaBuilder2 startObjectProperty(String key, boolean required) {
		return startObjectProperty(key, required, false);
	}

	public SchemaBuilder2 startObjectProperty(String key, boolean required, boolean array) {
		builder.objectProperty(key);
		if (required)
			builder.required();
		if (array)
			builder.array();
		return this;
	}

	public SchemaBuilder2 endObjectProperty() {
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
}
