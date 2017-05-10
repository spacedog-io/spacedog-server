package io.spacedog.sdk;

import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.model.Schema;

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
		dog.delete("/1/schema/{name}")//
				.routeParam("name", name).go(200, 404);
		return this;
	}

	public SchemaEndpoint set(Schema schema) {
		dog.put("/1/schema/{name}").routeParam("name", schema.name())//
				.bodySchema(schema).go(200, 201);
		return this;
	}

	public Schema get(String name) {
		ObjectNode node = dog.get("/1/schema/{name}")//
				.routeParam("name", name).go(200).asJsonObject();
		return new Schema(name, node);
	}

	public SchemaEndpoint setDefault(String name) {
		dog.put("/1/schema/{name}").routeParam("name", name).go(201);
		return this;
	}

	public List<Schema> getAll() {
		ObjectNode payload = dog.get("/1/schema").go(200).asJsonObject();
		List<Schema> schemas = Lists.newArrayList();
		Iterator<String> fieldNames = payload.fieldNames();
		while (fieldNames.hasNext()) {
			String name = fieldNames.next();
			schemas.add(new Schema(name, (ObjectNode) payload.get(name)));
		}
		return schemas;
	}

}
