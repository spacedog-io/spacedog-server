package io.spacedog.utils;

public class Settings {

	public String id() {
		return id(getClass());
	}

	public static String id(Class<? extends Settings> settingsClass) {
		return Utils.removeSuffix(settingsClass.getSimpleName(), "Settings")//
				.toLowerCase();
	}
}
