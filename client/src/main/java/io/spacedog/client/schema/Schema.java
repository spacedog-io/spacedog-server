/**
 * © David Attias 2015
 */
package io.spacedog.client.schema;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.http.SpaceFields;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class Schema implements MappingDirectives {

	public static final int SHARDS_DEFAULT = 1;
	public static final int REPLICAS_DEFAULT = 0;

	private String name;
	private ObjectNode mapping;
	private ObjectNode settings;

	public Schema() {
	}

	public Schema(String name, ObjectNode mapping) {
		this(name, mapping, null);
	}

	public Schema(String name, ObjectNode mapping, ObjectNode settings) {
		this.name = name;
		this.mapping = mapping;
		this.settings = settings;
	}

	public String name() {
		return name;
	}

	public Schema name(String name) {
		this.name = name;
		return this;
	}

	public ObjectNode mapping() {
		return mapping;
	}

	public Schema mapping(ObjectNode mapping) {
		this.mapping = mapping;
		return this;
	}

	public ObjectNode settings(boolean forUpdate) {
		if (settings == null)
			settings = Json.object();

		if (forUpdate)
			settings.remove("number_of_shards");
		else
			setIfNecessary(settings, "number_of_shards", SHARDS_DEFAULT);

		setIfNecessary(settings, "number_of_replicas", REPLICAS_DEFAULT);

		return settings;
	}

	private void setIfNecessary(ObjectNode settings, String fieldName, int defaultValue) {
		if (!settings.has(fieldName))
			settings.put(fieldName, defaultValue);
	}

	public Schema settings(ObjectNode settings) {
		this.settings = settings;
		return this;
	}

	public Schema enhance() {

		// add object mapping settings
		mapping.put(m_dynamic, m_strict);
		mapping.put(m_date_detection, false);

		// add mapping for meta fields
		JsonNode node = Json.get(mapping, m_properties);
		if (Json.isNull(node))
			throw Exceptions.illegalArgument(//
					"schema mapping [%s] has no properties field", mapping);

		ObjectNode properties = Json.checkObject(node);
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
		return Json.object("name", name, "mapping", mapping, "settings", settings).toString();
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
