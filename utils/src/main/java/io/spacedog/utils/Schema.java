/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

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

	public SchemaAclSettings acl() {
		JsonNode acl = content().get("_acl");
		if (acl == null)
			return null;

		try {
			return Json.mapper().treeToValue(acl, SchemaAclSettings.class);
		} catch (JsonProcessingException e) {
			throw Exceptions.illegalArgument(e, "invalid schema [_acl] json field");
		}
	}

	public void acl(SchemaAclSettings acl) {
		content().set("_acl", Json.mapper().valueToTree(acl));
	}

	public void acl(String role, HashSet<DataPermission> permissions) {
		SchemaAclSettings acl = acl();
		acl.put(role, permissions);
		acl(acl);
	}

	public static class SchemaAclSettings extends HashMap<String, Set<DataPermission>> {

		private static final long serialVersionUID = 7433673020746769733L;

		public SchemaAclSettings() {
		}

		// TODO create a single default singleton instance
		public static SchemaAclSettings defaultSettings() {

			SchemaAclSettings roles = new SchemaAclSettings();

			roles.put("key", Sets.newHashSet(DataPermission.read_all));

			roles.put("user", Sets.newHashSet(DataPermission.create, //
					DataPermission.update, DataPermission.search, DataPermission.delete));

			roles.put("admin", Sets.newHashSet(DataPermission.create, //
					DataPermission.update_all, DataPermission.search, DataPermission.delete_all));

			return roles;
		}
	}
}
