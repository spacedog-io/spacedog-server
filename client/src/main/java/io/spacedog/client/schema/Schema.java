/**
 * © David Attias 2015
 */
package io.spacedog.client.schema;

import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.utils.Exceptions;

public class Schema {

	private String name;
	private ObjectNode mapping;

	public Schema(String name, ObjectNode mapping) {
		this.name = name;
		this.mapping = mapping;
	}

	public String name() {
		return name;
	}

	public ObjectNode mapping() {
		return mapping;
	}

	public static SchemaBuilder builder(String name) {
		return SchemaBuilder.builder(name);
	}

	@Override
	public String toString() {
		return mapping.toString();
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public static void checkName(String name) {
		if (reservedNames.contains(name))
			throw Exceptions.illegalArgument("schema name [%s] is reserved", name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || name == null || mapping == null)
			return false;
		if (this == obj)
			return true;
		if (obj instanceof Schema) {
			Schema other = (Schema) obj;
			return name.equals(other.name) && mapping.equals(other.mapping);
		}
		return false;
	}

	private static Set<String> reservedNames = Sets.newHashSet("settings");
}
