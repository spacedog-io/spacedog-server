package io.spacedog.cli;

import com.beust.jcommander.IStringConverter;

import io.spacedog.client.http.SpaceBackend;

public class SpaceBackendConverter implements IStringConverter<SpaceBackend> {

	@Override
	public SpaceBackend convert(String value) {
		return SpaceBackend.valueOf(value);
	}
}
