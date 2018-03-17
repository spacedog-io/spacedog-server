/**
 * © David Attias 2015
 */
package io.spacedog.client.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.credentials.ObjectRolePermissions;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.push.Installation;
import io.spacedog.client.settings.Settings;
import io.spacedog.client.settings.SettingsBase;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class DataSettings implements Settings {

	// putain de merde

	@JsonIgnore
	private long version = MATCH_ANY_VERSIONS;
	private ObjectRolePermissions acl = new ObjectRolePermissions();

	public DataSettings() {
		acl.put(Installation.TYPE, Roles.user, Permission.create, Permission.readMine, //
				Permission.updateMine, Permission.deleteMine);
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

	public ObjectRolePermissions acl() {
		return acl;
	}

	public void acl(ObjectRolePermissions acl) {
		this.acl = acl;
	}

	@Override
	@JsonIgnore
	public String id() {
		return SettingsBase.id(getClass());
	}
}
