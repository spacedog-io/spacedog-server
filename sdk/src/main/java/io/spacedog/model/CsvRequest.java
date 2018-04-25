package io.spacedog.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.utils.Json7;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class CsvRequest {

	public boolean refresh;
	public ObjectNode query = Json7.object();
	public Settings settings = new Settings();
	public List<Column> columns = Lists.newArrayList();

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class Settings {
		public char delimiter = ';';
		public boolean firstRowOfHeaders;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class Column {
		public String field;
		public String header;
		public Type type = Type.string;
		public String pattern;

		public Column() {
		}

		public Column(String field) {
			this.field = field;
		}

		public enum Type {
			bool, integral, floating, string, timestamp, date, time, other
		}
	}
}