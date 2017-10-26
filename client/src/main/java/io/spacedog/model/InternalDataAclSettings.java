/**
 * © David Attias 2015
 */
package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
