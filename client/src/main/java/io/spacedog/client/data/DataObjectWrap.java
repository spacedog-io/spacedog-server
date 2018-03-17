package io.spacedog.client.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class DataObjectWrap extends DataWrapAbstract<DataObject> {

	private DataObject source;

	@Override
	public Class<DataObject> sourceClass() {
		return DataObject.class;
	}

	@Override
	public DataObject source() {
		return this.source;
	}

	@Override
	public DataWrap<DataObject> source(DataObject source) {
		this.source = source;
		return this;
	}
}
