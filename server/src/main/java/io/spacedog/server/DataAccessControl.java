/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.Set;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.RolePermissions;
import io.spacedog.client.data.DataAclSettings;

public class DataAccessControl {

	public static RolePermissions roles(String type) {
		return getDataAclSettings().get(type);
	}

	public static String[] types(Credentials credentials, Permission permission) {
		Set<String> types = DataStore.allDataTypes();
		if (!credentials.isAtLeastSuperAdmin()) {
			Set<String> accessibleTypes = getDataAclSettings().accessList(credentials, permission);
			types.retainAll(accessibleTypes);
		}
		return types.toArray(new String[types.size()]);
	}

	private static DataAclSettings getDataAclSettings() {
		return SettingsService.get().getAsObject(DataAclSettings.class);
	}

}
