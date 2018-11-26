package io.spacedog.client.schema;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.type.TypeFactory;

import io.spacedog.client.SpaceDog;

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
		dog.delete("/2/schemas/{name}").routeParam("name", name).go(200).asVoid();
	}

	public void deleteAll() {
		dog.delete("/2/schemas").go(200).asVoid();
	}

	public void set(Schema schema) {
		dog.put("/2/schemas/{name}").routeParam("name", schema.name())//
				.bodyPojo(schema).go(200).asVoid();
	}

	public Schema get(String name) {
		return dog.get("/2/schemas/{name}")//
				.routeParam("name", name).go(200).asPojo(Schema.class);
	}

	public void setDefault(String name) {
		dog.put("/2/schemas/{name}").routeParam("name", name).go(200).asVoid();
	}

	public Map<String, Schema> getAll() {
		return dog.get("/2/schemas").go(200).asPojo(TypeFactory.defaultInstance()//
				.constructMapLikeType(HashMap.class, String.class, Schema.class));
	}

}
