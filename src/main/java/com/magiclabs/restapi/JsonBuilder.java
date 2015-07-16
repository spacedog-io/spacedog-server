package com.magiclabs.restapi;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class JsonBuilder {

	private JsonBuilder parentBuilder = null;
	private JsonObject currentJsonObject = null;
	private JsonArray currentJsonArray = null;

	JsonBuilder() {
		this.currentJsonObject = new JsonObject();
	}

	private JsonBuilder(JsonBuilder parent, JsonObject current) {
		this.parentBuilder = parent;
		this.currentJsonObject = current;
	}

	private JsonBuilder(JsonBuilder parent, JsonArray current) {
		this.parentBuilder = parent;
		this.currentJsonArray = current;
	}

	//
	// object methods
	//

	public JsonBuilder add(String key, String value) {
		if (currentJsonObject == null)
			throw new IllegalStateException("current json node not an object");

		currentJsonObject.add(key, value);
		return this;
	}

	public JsonBuilder add(String key, boolean value) {
		if (currentJsonObject == null)
			throw new IllegalStateException("current json node not an object");

		currentJsonObject.add(key, value);
		return this;
	}

	public JsonBuilder add(String key, int value) {
		if (currentJsonObject == null)
			throw new IllegalStateException("current json node not an object");

		currentJsonObject.add(key, value);
		return this;
	}

	public JsonBuilder add(String key, long value) {
		if (currentJsonObject == null)
			throw new IllegalStateException("current json node not an object");

		currentJsonObject.add(key, value);
		return this;
	}

	public JsonBuilder add(String key, double value) {
		if (currentJsonObject == null)
			throw new IllegalStateException("current json node not an object");

		currentJsonObject.add(key, value);
		return this;
	}

	public JsonBuilder add(String key, float value) {
		if (currentJsonObject == null)
			throw new IllegalStateException("current json node not an object");

		currentJsonObject.add(key, value);
		return this;
	}

	public JsonBuilder addJson(String key, JsonValue value) {
		if (currentJsonObject == null)
			throw new IllegalStateException("current json node not an object");

		currentJsonObject.add(key, value);
		return this;
	}

	public JsonBuilder addJson(String key, String jsonText) {
		return addJson(key, JsonValue.readFrom(jsonText));
	}

	public JsonBuilder stObj(String key) {
		if (currentJsonObject == null)
			throw new IllegalStateException("current json node not an object");

		JsonObject nextJson = new JsonObject();
		currentJsonObject.add(key, nextJson);
		return new JsonBuilder(this, nextJson);
	}

	//
	// array methods
	//

	public JsonBuilder stArr(String key) {
		if (currentJsonObject == null)
			throw new IllegalStateException("current json node not an object");

		JsonArray nextJson = new JsonArray();
		currentJsonObject.add(key, nextJson);
		return new JsonBuilder(this, nextJson);
	}

	public JsonBuilder add(String value) {
		if (currentJsonArray == null)
			throw new IllegalStateException("current json node not an array");

		currentJsonArray.add(value);
		return this;
	}

	public JsonBuilder add(boolean value) {
		if (currentJsonArray == null)
			throw new IllegalStateException("current json node not an array");

		currentJsonArray.add(value);
		return this;
	}

	public JsonBuilder add(int value) {
		if (currentJsonArray == null)
			throw new IllegalStateException("current json node not an array");

		currentJsonArray.add(value);
		return this;
	}

	public JsonBuilder add(long value) {
		if (currentJsonArray == null)
			throw new IllegalStateException("current json node not an array");

		currentJsonArray.add(value);
		return this;
	}

	public JsonBuilder add(double value) {
		if (currentJsonArray == null)
			throw new IllegalStateException("current json node not an array");

		currentJsonArray.add(value);
		return this;
	}

	public JsonBuilder add(float value) {
		if (currentJsonArray == null)
			throw new IllegalStateException("current json node not an array");

		currentJsonArray.add(value);
		return this;
	}

	public JsonBuilder addJson(JsonValue jsonValue) {
		if (currentJsonArray == null)
			throw new IllegalStateException("current json node not an array");

		currentJsonArray.add(jsonValue);
		return this;
	}

	public JsonBuilder addJson(String jsonText) {
		return addJson(JsonValue.readFrom(jsonText));
	}

	public JsonBuilder addObj() {
		if (currentJsonArray == null)
			throw new IllegalStateException("current json node not an array");

		JsonObject nextJson = new JsonObject();
		currentJsonArray.add(nextJson);
		return new JsonBuilder(this, nextJson);
	}

	public <T extends Object> JsonBuilder add(Iterable<T> values) {
		if (currentJsonArray == null)
			throw new IllegalStateException("current json node not an array");

		for (Object value : values) {
			addGenericToArray(value);
		}

		return this;
	}

	private void addGenericToArray(Object value) {
		if (value instanceof Integer)
			currentJsonArray.add((Integer) value);
		else if (value instanceof Long)
			currentJsonArray.add((Long) value);
		else if (value instanceof Float)
			currentJsonArray.add((Float) value);
		else if (value instanceof Double)
			currentJsonArray.add((Double) value);
		else if (value instanceof String)
			currentJsonArray.add((String) value);
		else if (value instanceof JsonValue)
			currentJsonArray.add((JsonValue) value);
		else if (value instanceof Boolean)
			currentJsonArray.add((Boolean) value);
		else
			throw new IllegalArgumentException(String.format(
					"invalif array value type [%s]", value.getClass()
							.getSimpleName()));
	}

	//
	// other methods
	//

	public JsonBuilder end() {
		return parentBuilder;
	}

	public JsonObject build() {
		return parentBuilder == null ? currentJsonObject : parentBuilder
				.build();
	}
}
