package com.magiclabs.restapi;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

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

	public JsonBuilder addObj() {
		if (currentJsonArray == null)
			throw new IllegalStateException("current json node not an array");

		JsonObject nextJson = new JsonObject();
		currentJsonArray.add(nextJson);
		return new JsonBuilder(this, nextJson);
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
