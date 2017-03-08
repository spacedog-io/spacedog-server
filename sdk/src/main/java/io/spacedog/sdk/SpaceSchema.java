package io.spacedog.sdk;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.utils.Schema;

public class SpaceSchema {

	SpaceDog dog;

	SpaceSchema(SpaceDog session) {
		this.dog = session;
	}

	public SpaceSchema reset(Schema schema) {
		delete(schema);
		return set(schema);
	}

	public SpaceSchema delete(Schema schema) {
		return delete(schema.name());
	}

	public SpaceSchema delete(String name) {
		SpaceRequest.delete("/1/schema/{name}")//
				.routeParam("name", name).auth(dog).go(200, 404);
		return this;
	}

	public SpaceSchema set(Schema schema) {
		SpaceRequest.put("/1/schema/{name}").routeParam("name", schema.name())//
				.auth(dog).bodySchema(schema).go(200, 201);
		return this;
	}

	public Schema get(String name) {
		ObjectNode node = SpaceRequest.get("/1/schema/{name}")//
				.routeParam("name", name).auth(dog).go(200).objectNode();
		return new Schema(name, node);
	}

	public SpaceSchema setDefault(String name) {
		SpaceRequest.put("/1/schema/{name}").routeParam("name", name).auth(dog).go(201);
		return this;
	}

}
