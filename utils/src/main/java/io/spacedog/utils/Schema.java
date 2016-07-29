/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class Schema {

	private String name;
	private ObjectNode node;

	public Schema(String name, ObjectNode node) {
		this.name = name;
		this.node = node;
	}

	public String name() {
		return name;
	}

	public ObjectNode node() {
		return node;
	}

	public ObjectNode content() {
		return (ObjectNode) node.get(name);
	}

	public boolean hasIdPath() {
		return content().has("_id");
	}

	public String idPath() {
		return content().get("_id").asText();
	}

	public static SchemaBuilder builder(String name) {
		return SchemaBuilder.builder(name);
	}

	public void validate() {
		SchemaValidator.validate(name, node);
	}

	public ObjectNode translate() {
		return SchemaTranslator.translate(name, node);
	}

	@Override
	public String toString() {
		return node.toString();
	}
}
