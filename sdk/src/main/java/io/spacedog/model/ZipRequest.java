package io.spacedog.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class ZipRequest {
	public String fileName;
	public List<String> paths;

	public boolean isValid() {
		return !Utils.isNullOrEmpty(paths);
	}

	public void checkValid() {
		if (!isValid())
			throw Exceptions.illegalArgument("paths field is null or empty");
	}
}
