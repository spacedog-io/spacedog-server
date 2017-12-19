package io.spacedog.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class BasicDataObject extends DataObjectAbstract<Object> {

	private Object source;

	@Override
	public Class<Object> sourceClass() {
		return Object.class;
	}

	@Override
	public Object source() {
		return this.source;
	}

	@Override
	public DataObject<Object> source(Object source) {
		this.source = source;
		return this;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Results {

		public long total;
		public ObjectNode aggregations;
		public List<BasicDataObject> results;
	}
}
