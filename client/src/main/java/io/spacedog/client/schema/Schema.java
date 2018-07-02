/**
 * © David Attias 2015
 */
package io.spacedog.client.schema;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.http.SpaceFields;
import io.spacedog.utils.Check;
import io.spacedog.utils.Json;

public class Schema implements MappingDirectives {

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

	public Schema enhance() {

		// add object mapping settings
		ObjectNode object = Json.checkObject(Json.get(mapping, name));
		object.put(m_dynamic, m_strict);
		object.put(m_date_detection, false);

		// add mapping for meta fields
		ObjectNode properties = Json.checkObject(Json.get(object, m_properties));
		properties.set(SpaceFields.OWNER_FIELD, Json.object(m_type, m_keyword));
		properties.set(SpaceFields.GROUP_FIELD, Json.object(m_type, m_keyword));
		properties.set(SpaceFields.CREATED_AT_FIELD, //
				Json.object(m_type, m_date, m_format, m_timestamp_format));
		properties.set(SpaceFields.UPDATED_AT_FIELD, //
				Json.object(m_type, m_date, m_format, m_timestamp_format));

		return this;
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
		Check.matchRegex("[a-z0-9]{1,}", name, "schema name");
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof Schema == false)
			return false;
		Schema other = (Schema) obj;
		return Objects.equals(name, other.name) //
				&& Objects.equals(mapping, other.mapping);
	}

}
