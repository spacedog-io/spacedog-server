package io.spacedog.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.model.ObjectNodeDataObject;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class ObjectNodeSearchResults extends SearchResultsAbstract<ObjectNodeDataObject> {

	private List<ObjectNodeDataObject> results;

	public List<ObjectNodeDataObject> results() {
		return results;
	}

	public SearchResults<ObjectNodeDataObject> results(List<ObjectNodeDataObject> results) {
		this.results = results;
		return this;
	}
}