package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class MetaWrapperDataObject extends DataObjectAbstract<MetaWrapper> {

	private MetaWrapper source;

	public Class<MetaWrapper> sourceClass() {
		return MetaWrapper.class;
	}

	public MetaWrapper source() {
		return this.source;
	}

	public DataObject<MetaWrapper> source(MetaWrapper source) {
		this.source = source;
		return this;
	}
}
