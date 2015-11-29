/**
 * © David Attias 2015
 */
package io.spacedog.services;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class SchemaBuilder {

	public static SchemaBuilder builder(String type) {
		return new SchemaBuilder(Json.objectBuilder().object(type));
	}

	private JsonBuilder<ObjectNode> builder;
	private String currentPropertyType;

	private SchemaBuilder(JsonBuilder<ObjectNode> builder) {
		this.builder = builder;
	}

	public SchemaBuilder property(String key, String type) {
		currentPropertyType = type;
		builder.object(key).put("_type", type);
		return this;
	}

	public SchemaBuilder objectProperty(String key) {
		currentPropertyType = "object";
		builder.object(key).put("_type", "object");
		return this;
	}

	public SchemaBuilder end() {
		currentPropertyType = null;
		builder.end();
		return this;
	}

	public ObjectNode build() {
		return builder.build();
	}

	public SchemaBuilder id(String key) {
		builder.put("_id", key);
		return this;
	}

	public SchemaBuilder array() {
		checkCurrentPropertyExists();
		checkCurrentPropertyByInvalidTypes("_array", Schema.PropertyTypes.STASH);
		builder.put("_array", true);
		return this;
	}

	public SchemaBuilder required() {
		checkCurrentPropertyExists();
		builder.put("_required", true);
		return this;
	}

	public SchemaBuilder language(String language) {
		checkCurrentPropertyExists();
		checkCurrentPropertyByValidType("_language", Schema.PropertyTypes.TEXT);
		builder.put("_language", language);
		return this;
	}

	private void checkCurrentPropertyExists() {
		if (currentPropertyType == null)
			throw new IllegalStateException(String.format("no current property in [%s]", builder.toString()));
	}

	private void checkCurrentPropertyByValidType(String fieldName, Schema.PropertyTypes... validTypes) {
		Schema.PropertyTypes propertyType = Schema.PropertyTypes.valueOfIgnoreCase(this.currentPropertyType);

		for (Schema.PropertyTypes validType : validTypes) {
			if (propertyType.equals(validType))
				return;
		}

		throw new IllegalStateException(
				String.format("invalid property type [%s] for this field [%s]", propertyType, fieldName));
	}

	private void checkCurrentPropertyByInvalidTypes(String fieldName, Schema.PropertyTypes... invalidTypes) {
		Schema.PropertyTypes propertyType = Schema.PropertyTypes.valueOfIgnoreCase(this.currentPropertyType);

		for (Schema.PropertyTypes invalidType : invalidTypes) {
			if (propertyType.equals(invalidType))
				throw new IllegalStateException(
						String.format("invalid property type [%s] for this field [%s]", propertyType, fieldName));
		}
	}

}
