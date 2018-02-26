package io.spacedog.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.data.DataObject;
import io.spacedog.client.data.DataObjectAbstract;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class JsonDataObject extends DataObjectAbstract<ObjectNode> {

	private ObjectNode source;

	public Class<ObjectNode> sourceClass() {
		return ObjectNode.class;
	}

	public ObjectNode source() {
		return this.source;
	}

	public DataObject<ObjectNode> source(ObjectNode source) {
		this.source = source;
		return this;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Results {

		public long total;
		public ObjectNode aggregations;
		public List<JsonDataObject> results;
	}
}
