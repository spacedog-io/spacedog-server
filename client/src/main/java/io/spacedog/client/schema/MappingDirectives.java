package io.spacedog.client.schema;

public interface MappingDirectives {

	// types
	String m_double = "double";
	String m_float = "float";
	String m_long = "long";
	String m_integer = "integer";
	String m_boolean = "boolean";
	String m_date = "date";
	String m_text = "text";
	String m_keyword = "keyword";
	String m_object = "object";
	String m_geo_point = "geo_point";

	// parameters
	String m_format = "format";
	String m_coerce = "coerce";
	String m_index = "index";
	String m_enabled = "enabled";
	String m_type = "type";
	String m_properties = "properties";
	String m_analyzer = "analyzer";
	String m_dynamic = "dynamic";
	String m_date_detection = "date_detection";
	String m_strict = "strict";
	String m_meta = "_meta";

	// values
	String m_french = "french";
	String m_english = "english";

	// date formats
	String m_date_format = "date";
	String m_time_format = "hour_minute_second";
	String m_timestamp_format = "date_time";
}
