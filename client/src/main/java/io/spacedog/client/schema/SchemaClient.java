package io.spacedog.client.schema;

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

import io.spacedog.client.SpaceDog;
import io.spacedog.utils.Json;

public class SchemaClient {

	private SpaceDog dog;

	public SchemaClient(SpaceDog session) {
		this.dog = session;
	}

	public void reset(Schema schema) {
		delete(schema);
		set(schema);
	}

	public void delete(Schema schema) {
		delete(schema.name());
	}

	public void delete(String name) {
		dog.delete("/2/schemas/{name}").routeParam("name", name).go(200);
	}

	public void deleteAll() {
		dog.delete("/2/schemas").go(200);
	}

	public void set(Schema schema) {
		dog.put("/2/schemas/{name}").routeParam("name", schema.name())//
				.bodyJson(schema.mapping()).go(200);
	}

	public Schema get(String name) {
		ObjectNode node = dog.get("/2/schemas/{name}")//
				.routeParam("name", name).go(200).asJsonObject();
		return new Schema(name, node);
	}

	public void setDefault(String name) {
		dog.put("/2/schemas/{name}").routeParam("name", name).go(200);
	}

	public Map<String, Schema> getAll() {
		ObjectNode payload = dog.get("/2/schemas").go(200).asJsonObject();
		Map<String, Schema> schemas = Maps.newHashMap();
		Iterator<String> fieldNames = payload.fieldNames();
		while (fieldNames.hasNext()) {
			String name = fieldNames.next();
			ObjectNode mapping = Json.object(name, payload.get(name));
			schemas.put(name, new Schema(name, mapping));
		}
		return schemas;
	}

}
