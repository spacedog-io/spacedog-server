package io.spacedog.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SearchResults;
import io.spacedog.client.SearchResultsAbstract;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class ObjectNodeDataObject extends DataObjectAbstract<ObjectNode> {

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
	@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class Results extends SearchResultsAbstract<ObjectNodeDataObject> {

		private List<ObjectNodeDataObject> results;

		public List<ObjectNodeDataObject> results() {
			return results;
		}

		public SearchResults<ObjectNodeDataObject> results(List<ObjectNodeDataObject> results) {
			this.results = results;
			return this;
		}
	}
}
