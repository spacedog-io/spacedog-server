package io.spacedog.model;

import java.util.AbstractMap.SimpleEntry;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, //
		getterVisibility = Visibility.PUBLIC_ONLY, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class KeyValue extends SimpleEntry<String, Object> {

	private static final long serialVersionUID = 3160616523589666590L;

	public KeyValue() {
		super(null, null);
	}

	public KeyValue(String key, Object value) {
		super(key, value);
	}
}