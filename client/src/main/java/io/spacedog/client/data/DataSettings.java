/**
 * © David Attias 2015
 */
package io.spacedog.client.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;

import io.spacedog.client.credentials.ObjectRolePermissions;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.push.Installation;
import io.spacedog.client.settings.SettingsBase;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class DataSettings extends SettingsBase {

	private ObjectRolePermissions acl = new ObjectRolePermissions();

	public DataSettings() {
		acl.put(Installation.TYPE, Roles.user, Permission.create, Permission.readMine, //
				Permission.updateMine, Permission.deleteMine);
	}

	public ObjectRolePermissions acl() {
		return acl;
	}

	public void acl(ObjectRolePermissions acl) {
		this.acl = acl;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DataSettings == false)
			return false;
		return Objects.equal(acl, ((DataSettings) obj).acl);
	}
}
