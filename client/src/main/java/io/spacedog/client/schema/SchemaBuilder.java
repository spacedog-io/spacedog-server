/**
 * © David Attias 2015
 */
package io.spacedog.client.schema;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;

public class SchemaBuilder implements MappingDirectives {

	public static SchemaBuilder builder(String type) {
		return new SchemaBuilder(type);
	}

	public SchemaBuilder dynamicStrict() {
		return dynamic(m_strict);
	}

	public SchemaBuilder dynamic(String type) {
		builder.add(m_dynamic, type);
		return this;
	}

	public SchemaBuilder dateDetection(boolean detection) {
		builder.add(m_date_detection, detection);
		return this;
	}

	public SchemaBuilder property(String name, String type) {
		if (openProperty)
			builder.end();
		else
			builder.object(m_properties);

		openProperty = true;
		builder.object(name);
		if (!Strings.isNullOrEmpty(type))
			builder.add(m_type, type);
		return this;
	}

	public SchemaBuilder keyword(String key) {
		property(key, m_keyword);
		return this;
	}

	public SchemaBuilder text(String key) {
		return property(key, m_text);
	}

	public SchemaBuilder bool(String key) {
		property(key, m_boolean);
		return this;
	}

	public SchemaBuilder integer(String key) {
		property(key, m_integer);
		builder.add(m_coerce, false);
		return this;
	}

	public SchemaBuilder longg(String key) {
		property(key, m_long);
		builder.add(m_coerce, false);
		return this;
	}

	public SchemaBuilder floatt(String key) {
		property(key, m_float);
		builder.add(m_coerce, false);
		return this;
	}

	public SchemaBuilder doublee(String key) {
		property(key, m_double);
		builder.add(m_coerce, false);
		return this;
	}

	public SchemaBuilder geopoint(String key) {
		property(key, m_geo_point);
		return this;
	}

	public SchemaBuilder date(String key) {
		property(key, m_date)//
				.builder.add(m_format, m_date_format);
		return this;
	}

	public SchemaBuilder time(String key) {
		property(key, m_date)//
				.builder.add(m_format, m_time_format);
		return this;
	}

	public SchemaBuilder timestamp(String key) {
		property(key, m_date)//
				.builder.add(m_format, m_timestamp_format);
		return this;
	}

	public SchemaBuilder stash(String key) {
		property(key, m_object)//
				.builder.add(m_enabled, false);
		return this;
	}

	public SchemaBuilder object(String key) {
		property(key, null);
		openProperty = false;
		return this;
	}

	public SchemaBuilder closeObject() {
		if (openProperty) {
			openProperty = false;
			// end property
			builder.end();
		}
		// end properties
		builder.end();
		// object is closed by next property
		openProperty = true;
		return this;
	}

	//
	// Sub fields
	//

	public SchemaBuilder subKeyword() {
		return subField(m_keyword);
	}

	public SchemaBuilder subText() {
		return subField(m_text);
	}

	public SchemaBuilder subText(String analyzer) {
		return subField(m_text, m_analyzer, analyzer);
	}

	public SchemaBuilder subField(String type, Object... attributes) {
		if (!this.openProperty)
			throw Exceptions.runtime("only applicable on a field");

		builder.object(m_fields)//
				.object(type)//
				.add(m_type, type)//
				.add(attributes)//
				.end()//
				.end();

		return this;
	}

	//
	// field attributes
	//

	public SchemaBuilder french() {
		return language("french");
	}

	public SchemaBuilder frenchMax() {
		return language("french_max");
	}

	public SchemaBuilder english() {
		return language("english");
	}

	public SchemaBuilder language(String language) {
		builder.add(m_analyzer, language);
		return this;
	}

	public SchemaBuilder enabled(boolean enabled) {
		builder.add(m_enabled, enabled);
		return this;
	}

	public SchemaBuilder index(boolean index) {
		builder.add(m_index, index);
		return this;
	}

	public Schema build() {
		return new Schema(name, builder.build());
	}

	@Override
	public String toString() {
		return build().toString();
	}

	//
	// Implementation
	//

	private String name;
	private JsonBuilder<ObjectNode> builder;
	private boolean openProperty = false;

	private SchemaBuilder(String name) {
		this.name = name;
		this.builder = Json.builder().object();
	}

}
