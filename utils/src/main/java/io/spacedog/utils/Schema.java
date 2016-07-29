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

	public ObjectNode rootNode() {
		return node;
	}

	public ObjectNode contentNode() {
		return (ObjectNode) node.get(name);
	}

	public boolean hasIdPath() {
		return contentNode().has("_id");
	}

	public String idPath() {
		return contentNode().get("_id").asText();
	}

	public static SchemaBuilder builder(String name) {
		return SchemaBuilder.builder(name);
	}
}
