/**
 * © David Attias 2015
 */
package io.spacedog.services.data;

import java.util.Set;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.RolePermissions;
import io.spacedog.client.data.DataSettings;
import io.spacedog.server.Services;

public class DataAccessControl {

	public static RolePermissions roles(String type) {
		return getDataSettings().acl().get(type);
	}

	public static String[] types(Credentials credentials, Permission permission) {
		Set<String> types = Services.data().types();
		if (!credentials.isAtLeastSuperAdmin()) {
			Set<String> accessibleTypes = getDataSettings()//
					.acl().accessList(credentials, permission);
			types.retainAll(accessibleTypes);
		}
		return types.toArray(new String[types.size()]);
	}

	private static DataSettings getDataSettings() {
		return Services.settings().get(DataSettings.class).get();
	}

}
