package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.spacedog.utils.Utils;

public class SettingsBase implements Settings {

	@JsonIgnore
	private long version = MATCH_ANY_VERSIONS;

	@JsonIgnore
	@Override
	public String id() {
		return id(getClass());
	}

	@Override
	@JsonIgnore
	public long version() {
		return version;
	}

	@Override
	public void version(long version) {
		this.version = version;
	}

	public static String id(Class<? extends Settings> settingsClass) {
		return Utils.removeSuffix(settingsClass.getSimpleName(), "Settings")//
				.toLowerCase();
	}

}
