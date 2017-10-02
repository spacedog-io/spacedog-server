package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class MetadataDataObject extends DataObjectAbstract<MetadataBase> {

	private MetadataBase source;

	public Class<MetadataBase> sourceClass() {
		return MetadataBase.class;
	}

	public MetadataBase source() {
		return this.source;
	}

	public DataObject<MetadataBase> source(MetadataBase source) {
		this.source = source;
		return this;
	}
}
