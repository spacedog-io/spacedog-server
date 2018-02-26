package io.spacedog.client.schema;

public interface SchemaDirectives {

	// fields
	String s_type = "_type";
	String s_labels = "_labels";
	String s_enum_type = "_enumType";
	String s_extra = "_extra";
	String s_examples = "_examples";
	String s_values = "_values";
	String s_required = "_required";
	String s_array = "_array";
	String s_acl = "_acl";
	String s_language = "_language";
	String s_ref_type = "_ref_type";

	// types
	String s_double = "double";
	String s_float = "float";
	String s_long = "long";
	String s_integer = "integer";
	String s_boolean = "boolean";
	String s_text = "text";
	String s_string = "string";
	String s_enum = "enum";
	String s_geopoint = "geopoint";
	String s_stash = "stash";
	String s_date = "date";
	String s_time = "time";
	String s_timestamp = "timestamp";
}
