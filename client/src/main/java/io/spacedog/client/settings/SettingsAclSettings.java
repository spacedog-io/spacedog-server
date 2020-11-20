package io.spacedog.client.settings;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.credentials.ObjectRolePermissions;

@SuppressWarnings("serial")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class SettingsAclSettings extends ObjectRolePermissions implements Settings {

	@JsonIgnore
	private String version;

	@Override
	@JsonIgnore
	public String version() {
		return version;
	}

	@Override
	public void version(String version) {
		this.version = version;
	}

	@Override
	@JsonIgnore
	public String id() {
		return SettingsBase.id(getClass());
	}
}
