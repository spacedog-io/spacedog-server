/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.utils.SchemaSettings.SchemaAcl;

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

	public SchemaAcl acl() {
		JsonNode acl = content().get("_acl");
		if (acl == null)
			return null;

		try {
			return Json.mapper().treeToValue(acl, SchemaAcl.class);
		} catch (JsonProcessingException e) {
			throw Exceptions.illegalArgument(e, "invalid schema [_acl] json field");
		}
	}

	public void acl(SchemaAcl acl) {
		content().set("_acl", Json.mapper().valueToTree(acl));
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
}
