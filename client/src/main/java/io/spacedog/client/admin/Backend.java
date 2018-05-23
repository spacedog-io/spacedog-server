package io.spacedog.client.admin;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.data.DataObjectBase;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.data.DataWrapAbstract;
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

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Wrap extends DataWrapAbstract<Backend> {

		private Backend source;

		@Override
		public Class<Backend> sourceClass() {
			return Backend.class;
		}

		@Override
		public Backend source() {
			return this.source;
		}

		@Override
		public DataWrap<Backend> source(Backend source) {
			this.source = source;
			return this;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Results {
		public long total;
		public List<Backend.Wrap> results;
	}
}