package io.spacedog.client.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.data.DataObjectBase;
import io.spacedog.client.schema.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Backend extends DataObjectBase {

	public enum Type {
		standard, dedicated
	}

	public final static String TYPE = "backend";

	public Type type;
	public String version;

	public Backend() {
	}

	public static Schema schema() {
		return Schema.builder(TYPE)//
				.keyword("type")//
				.keyword("version")//
				.build();
	}
}