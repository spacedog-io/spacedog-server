package io.spacedog.services;

import com.eclipsesource.json.JsonObject;

public class SchemaBuilder {

	public static SchemaBuilder builder(String type) {
		SchemaBuilder builder = new SchemaBuilder();
		JsonObject nextJson = new JsonObject();
		builder.currentObject.add(type, nextJson);
		return new SchemaBuilder(builder, nextJson);
	}

	private SchemaBuilder parentBuilder;
	private JsonObject currentObject;
	private JsonObject currentProperty;

	private SchemaBuilder() {
		this.currentObject = new JsonObject();
	}

	private SchemaBuilder(SchemaBuilder parent, JsonObject current) {
		this.parentBuilder = parent;
		this.currentObject = current;
		this.currentProperty = currentObject;
	}

	public SchemaBuilder add(String key, String type) {
		currentProperty = new JsonObject();
		currentProperty.add("_type", type);
		currentObject.add(key, currentProperty);
		return this;
	}

	public SchemaBuilder startObject(String key) {
		JsonObject nextJson = new JsonObject();
		nextJson.add("_type", "object");
		currentObject.add(key, nextJson);
		return new SchemaBuilder(this, nextJson);
	}

	public SchemaBuilder end() {
		return parentBuilder;
	}

	public JsonObject build() {
		return parentBuilder == null ? currentObject : parentBuilder.build();
	}

	public SchemaBuilder id(String key) {
		currentObject.add("_id", key);
		return this;
	}

	public SchemaBuilder array() {
		checkCurrentPropertyExists();
		checkCurrentPropertyByInvalidTypes("_array", Schema.PropertyTypes.STASH);
		currentProperty.add("_array", true);
		return this;
	}

	public SchemaBuilder required() {
		checkCurrentPropertyExists();
		currentProperty.add("_required", true);
		return this;
	}

	public SchemaBuilder language(String language) {
		checkCurrentPropertyExists();
		checkCurrentPropertyByValidType("_language", Schema.PropertyTypes.TEXT);
		currentProperty.add("_language", language);
		return this;
	}

	private void checkCurrentPropertyExists() {
		if (currentProperty == null)
			throw new IllegalStateException(String.format(
					"no current property to set in object [%s]",
					currentObject.toString()));
	}

	private void checkCurrentPropertyByValidType(String fieldName,
			Schema.PropertyTypes... validTypes) {
		Schema.PropertyTypes propertyType = Schema.PropertyTypes
				.valueOfIgnoreCase(this.currentProperty.get("_type").asString());

		for (Schema.PropertyTypes validType : validTypes) {
			if (propertyType.equals(validType))
				return;
		}

		throw new IllegalStateException(String.format(
				"invalid property type [%s] for this field [%s]", propertyType,
				fieldName));
	}

	private void checkCurrentPropertyByInvalidTypes(String fieldName,
			Schema.PropertyTypes... invalidTypes) {
		Schema.PropertyTypes propertyType = Schema.PropertyTypes
				.valueOfIgnoreCase(this.currentProperty.get("_type").asString());

		for (Schema.PropertyTypes invalidType : invalidTypes) {
			if (propertyType.equals(invalidType))
				throw new IllegalStateException(String.format(
						"invalid property type [%s] for this field [%s]",
						propertyType, fieldName));
		}
	}

}
