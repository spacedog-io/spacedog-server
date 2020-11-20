package io.spacedog.client.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.spacedog.utils.Utils;

public class SettingsBase implements Settings {

	@JsonIgnore
	private String version;

	@JsonIgnore
	@Override
	public String id() {
		return id(getClass());
	}

	@Override
	@JsonIgnore
	public String version() {
		return version;
	}

	@Override
	public void version(String version) {
		this.version = version;
	}

	public static String id(Class<? extends Settings> settingsClass) {
		return Utils.trimSuffix(settingsClass.getSimpleName(), "Settings")//
				.toLowerCase();
	}

}
