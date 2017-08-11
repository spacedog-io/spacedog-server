/**
 * © David Attias 2015
 */
package io.spacedog.model;

import java.util.HashMap;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json7;
import io.spacedog.utils.SchemaBuilder;
import io.spacedog.utils.SchemaTranslator;
import io.spacedog.utils.SchemaValidator;

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

	public SchemaAcl acl() {
		JsonNode acl = content().get("_acl");
		if (acl == null)
			return null;

		try {
			return Json7.mapper().treeToValue(acl, SchemaAcl.class);
		} catch (JsonProcessingException e) {
			throw Exceptions.illegalArgument(e, "invalid schema [_acl] json field");
		}
	}

	public void acl(SchemaAcl acl) {
		content().set("_acl", Json7.mapper().valueToTree(acl));
	}

	public void acl(String role, DataPermission... permissions) {
		acl(role, Sets.newHashSet(permissions));
	}

	public void acl(String role, Set<DataPermission> permissions) {
		SchemaAcl acl = acl();
		if (acl == null)
			acl = new SchemaAcl();
		acl.put(role, permissions);
		acl(acl);
	}

	public static void checkName(String name) {
		if (reservedNames.contains(name))
			throw Exceptions.illegalArgument("schema name [%s] is reserved", name);
	}

	public static class SchemaAcl extends HashMap<String, Set<DataPermission>> {

		private static final long serialVersionUID = 7433673020746769733L;

		public static SchemaAcl defaultAcl() {

			return new SchemaAcl()//
					.set("all", DataPermission.read_all)//
					.set("user", DataPermission.create, DataPermission.update, //
							DataPermission.search, DataPermission.delete)//
					.set("admin", DataPermission.create, DataPermission.update_all, //
							DataPermission.search, DataPermission.delete_all);
		}

		public SchemaAcl set(String role, DataPermission... permissions) {
			put(role, Sets.newHashSet(permissions));
			return this;
		}
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
