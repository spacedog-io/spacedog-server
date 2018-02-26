/**
 * © David Attias 2015
 */
package io.spacedog.client.schema;

import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.RolePermissions;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

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

	public static SchemaBuilder builder(String name) {
		return SchemaBuilder.builder(name);
	}

	public Schema validate() {
		SchemaValidator.validate(name, node);
		return this;
	}

	public ObjectNode translate() {
		return SchemaTranslator.translate(name, node);
	}

	@Override
	public String toString() {
		return node.toString();
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public RolePermissions acl() {
		JsonNode acl = content().get("_acl");
		if (acl == null)
			return null;

		try {
			return Json.mapper().treeToValue(acl, RolePermissions.class);
		} catch (JsonProcessingException e) {
			throw Exceptions.illegalArgument(e, "invalid schema [_acl] json field");
		}
	}

	public Schema acl(RolePermissions acl) {
		content().set("_acl", Json.mapper().valueToTree(acl));
		return this;
	}

	public Schema acl(String role, Permission... permissions) {
		acl(role, Sets.newHashSet(permissions));
		return this;
	}

	public Schema acl(String role, Set<Permission> permissions) {
		RolePermissions acl = acl();
		if (acl == null)
			acl = new RolePermissions();
		acl.put(role, permissions);
		acl(acl);
		return this;
	}

	public static void checkName(String name) {
		if (reservedNames.contains(name))
			throw Exceptions.illegalArgument("schema name [%s] is reserved", name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || name == null || node == null)
			return false;
		if (this == obj)
			return true;
		if (obj instanceof Schema) {
			Schema other = (Schema) obj;
			return name.equals(other.name) && node.equals(other.node);
		}
		return false;
	}

	private static Set<String> reservedNames = Sets.newHashSet("settings");
}
