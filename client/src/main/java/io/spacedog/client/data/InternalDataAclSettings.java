/**
 * © David Attias 2015
 */
package io.spacedog.client.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.spacedog.client.credentials.ObjectRolePermissions;
import io.spacedog.client.settings.Settings;
import io.spacedog.client.settings.SettingsBase;

@SuppressWarnings("serial")
public class InternalDataAclSettings extends ObjectRolePermissions implements Settings {

	@JsonIgnore
	private long version = MATCH_ANY_VERSIONS;

	@Override
	@JsonIgnore
	public long version() {
		return version;
	}

	@Override
	public void version(long version) {
		this.version = version;
	}

	@Override
	@JsonIgnore
	public String id() {
		return SettingsBase.id(getClass());
	}
}
