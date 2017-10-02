package io.spacedog.model;

import io.spacedog.utils.Utils;

public class SettingsBase implements Settings {

	public String id() {
		return id(getClass());
	}

	public static String id(Class<? extends Settings> settingsClass) {
		return Utils.removeSuffix(settingsClass.getSimpleName(), "Settings")//
				.toLowerCase();
	}
}
