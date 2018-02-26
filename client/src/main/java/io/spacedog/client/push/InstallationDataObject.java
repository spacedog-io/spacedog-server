package io.spacedog.client.push;

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
public class InstallationDataObject extends DataObjectAbstract<Installation> {

	private Installation source;

	public Class<Installation> sourceClass() {
		return Installation.class;
	}

	public Installation source() {
		return this.source;
	}

	public DataObject<Installation> source(Installation source) {
		this.source = source;
		return this;
	}

	@Override
	public String type() {
		return "installation";
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Results {

		public long total;
		public ObjectNode aggregations;
		public List<InstallationDataObject> results;
	}
}
