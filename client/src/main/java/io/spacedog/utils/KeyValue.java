package io.spacedog.utils;

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

	public static KeyValue parse(String tag) {
		int index = tag.indexOf('=');
		return index < 0 ? new KeyValue("", tag) //
				: new KeyValue(tag.substring(0, index), tag.substring(index + 1));
	}

	public String asTag() {
		return toString();
	}

}