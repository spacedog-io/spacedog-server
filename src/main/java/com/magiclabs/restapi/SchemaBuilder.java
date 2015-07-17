package com.magiclabs.restapi;

import com.eclipsesource.json.JsonObject;

public class SchemaBuilder {

	public static SchemaBuilder builder(String type) {
		SchemaBuilder builder = new SchemaBuilder();
		JsonObject nextJson = new JsonObject();
		builder.currentJson.add(type, nextJson);
		return new SchemaBuilder(builder, nextJson);
	}

	private SchemaBuilder parentBuilder = null;
	private JsonObject currentJson = null;

	private SchemaBuilder() {
		this.currentJson = new JsonObject();
	}

	private SchemaBuilder(SchemaBuilder parent, JsonObject current) {
		this.parentBuilder = parent;
		this.currentJson = current;
	}

	public SchemaBuilder id(String key) {
		currentJson.add("_id", key);
		return this;
	}

	public SchemaBuilder add(String key, String type, boolean mandatory) {
		JsonObject nextJson = new JsonObject();
		nextJson.add("_type", type);
		if (mandatory)
			nextJson.add("_required", mandatory);
		currentJson.add(key, nextJson);
		return this;
	}

	public SchemaBuilder addText(String key, String language, boolean mandatory) {
		this.add(key, Schema.PropertyTypes.TEXT.toString(), mandatory);
		currentJson.get(key).asObject().add("_language", language);
		return this;
	}

	public SchemaBuilder startObject(String key, boolean mandatory) {
		JsonObject nextJson = new JsonObject();
		nextJson.add("_type", "object");
		if (mandatory)
			nextJson.add("_required", mandatory);
		currentJson.add(key, nextJson);
		return new SchemaBuilder(this, nextJson);
	}

	public SchemaBuilder end() {
		return parentBuilder;
	}

	public JsonObject build() {
		return parentBuilder == null ? currentJson : parentBuilder.build();
	}

}
