/**
 * © David Attias 2015
 */
package io.spacedog.services.file;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.file.FileBucket;
import io.spacedog.client.settings.Settings;
import io.spacedog.client.settings.SettingsBase;

@SuppressWarnings("serial")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalFileSettings extends HashMap<String, FileBucket> implements Settings {

	@JsonIgnore
	private String version;

	@JsonIgnore
	@Override
	public String id() {
		return SettingsBase.id(getClass());
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
}
