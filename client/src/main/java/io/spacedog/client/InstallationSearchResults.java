package io.spacedog.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.model.InstallationDataObject;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class InstallationSearchResults extends SearchResultsAbstract<InstallationDataObject> {

	private List<InstallationDataObject> results;

	public List<InstallationDataObject> results() {
		return results;
	}

	public SearchResults<InstallationDataObject> results(List<InstallationDataObject> results) {
		this.results = results;
		return this;
	}
}