package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class MetadataDataObject extends DataObjectAbstract<Metadata> {

	private Metadata source;

	@Override
	public Class<Metadata> sourceClass() {
		return Metadata.class;
	}

	@Override
	public Metadata source() {
		return this.source;
	}

	@Override
	public DataObject<Metadata> source(Metadata source) {
		this.source = source;
		return this;
	}
}
