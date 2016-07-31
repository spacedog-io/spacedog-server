/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.HashMap;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

	public DataTypeAccessControl acl() {
		JsonNode acl = content().get("_acl");
		if (acl == null)
			return null;

		try {
			return Json.mapper().treeToValue(acl, DataTypeAccessControl.class);
		} catch (JsonProcessingException e) {
			// TODO add an more explicit message
			throw Exceptions.illegalArgument(e);
		}
	}

	public void acl(DataTypeAccessControl acl) {
		content().set("_acl", Json.mapper().valueToTree(acl));
	}

	public static class DataTypeAccessControl extends HashMap<String, Set<DataPermission>> {

		private static final long serialVersionUID = 7433673020746769733L;
	}
}
