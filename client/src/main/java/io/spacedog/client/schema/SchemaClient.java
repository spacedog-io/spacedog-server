package io.spacedog.client.schema;

import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.utils.Json;

public class SchemaClient {

	private SpaceDog dog;

	public SchemaClient(SpaceDog session) {
		this.dog = session;
	}

	public SchemaClient reset(Schema schema) {
		delete(schema);
		return set(schema);
	}

	public SchemaClient delete(Schema schema) {
		return delete(schema.name());
	}

	public SchemaClient delete(String name) {
		dog.delete("/1/schemas/{name}")//
				.routeParam("name", name).go(200, 404);
		return this;
	}

	public SchemaClient set(Schema schema) {
		dog.put("/1/schemas/{name}").routeParam("name", schema.name())//
				.bodyJson(schema.mapping()).go(200, 201);
		return this;
	}

	public Schema get(String name) {
		ObjectNode node = dog.get("/1/schemas/{name}")//
				.routeParam("name", name).go(200).asJsonObject();
		return new Schema(name, node);
	}

	public SchemaClient setDefault(String name) {
		dog.put("/1/schemas/{name}").routeParam("name", name).go(201);
		return this;
	}

	public Set<Schema> getAll() {
		ObjectNode payload = dog.get("/1/schemas").go(200).asJsonObject();
		Set<Schema> schemas = Sets.newHashSet();
		Iterator<String> fieldNames = payload.fieldNames();
		while (fieldNames.hasNext()) {
			String name = fieldNames.next();
			ObjectNode node = Json.object(name, payload.get(name));
			schemas.add(new Schema(name, node));
		}
		return schemas;
	}

}
