package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class ObjectNodeWithMetadataDataObject extends DataObjectAbstract<ObjectNodeWithMetadata> {

	private ObjectNodeWithMetadata source;

	public Class<ObjectNodeWithMetadata> sourceClass() {
		return ObjectNodeWithMetadata.class;
	}

	public ObjectNodeWithMetadata source() {
		return this.source;
	}

	public DataObject<ObjectNodeWithMetadata> source(ObjectNodeWithMetadata source) {
		this.source = source;
		return this;
	}
}
