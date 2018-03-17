package io.spacedog.client.data;

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
public class ObjectNodeWrap extends DataWrapAbstract<ObjectNode> {

	private ObjectNode source;

	public Class<ObjectNode> sourceClass() {
		return ObjectNode.class;
	}

	public ObjectNode source() {
		return this.source;
	}

	public DataWrap<ObjectNode> source(ObjectNode source) {
		this.source = source;
		return this;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Results {

		public long total;
		public ObjectNode aggregations;
		public List<ObjectNodeWrap> results;
	}
}
