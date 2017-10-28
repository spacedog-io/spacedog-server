package io.spacedog.client;

import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.model.Schema;
import io.spacedog.utils.Json;

public class SchemaEndpoint {

	SpaceDog dog;

	SchemaEndpoint(SpaceDog session) {
		this.dog = session;
	}

	public SchemaEndpoint reset(Schema schema) {
		delete(schema);
		return set(schema);
	}

	public SchemaEndpoint delete(Schema schema) {
		return delete(schema.name());
	}

	public SchemaEndpoint delete(String name) {
		dog.delete("/1/schemas/{name}")//
				.routeParam("name", name).go(200, 404);
		return this;
	}

	public SchemaEndpoint set(Schema schema) {
		dog.put("/1/schemas/{name}").routeParam("name", schema.name())//
				.bodySchema(schema).go(200, 201);
		return this;
	}

	public Schema get(String name) {
		ObjectNode node = dog.get("/1/schemas/{name}")//
				.routeParam("name", name).go(200).asJsonObject();
		return new Schema(name, node);
	}

	public SchemaEndpoint setDefault(String name) {
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
