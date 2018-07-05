/**
 * © David Attias 2015
 */
package io.spacedog.services.file;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.file.FileBucketSettings;
import io.spacedog.client.settings.Settings;
import io.spacedog.client.settings.SettingsBase;

@SuppressWarnings("serial")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalFileSettings extends HashMap<String, FileBucketSettings> implements Settings {

	@JsonIgnore
	private long version = MATCH_ANY_VERSIONS;

	@JsonIgnore
	@Override
	public String id() {
		return SettingsBase.id(getClass());
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
}
