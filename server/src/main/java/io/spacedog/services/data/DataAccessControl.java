/**
 * © David Attias 2015
 */
package io.spacedog.services.data;

import java.util.Set;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.RolePermissions;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.utils.Exceptions;

public class DataAccessControl {

	public static RolePermissions roles(String type) {
		return Services.data().settings().acl().get(type);
	}

	public static String[] types(Credentials credentials, Permission permission) {
		Set<String> types = Services.data().types();
		if (!credentials.isAtLeastSuperAdmin()) {
			Set<String> accessibleTypes = Services.data().settings()//
					.acl().accessList(credentials, permission);
			types.retainAll(accessibleTypes);
		}
		return types.toArray(new String[types.size()]);
	}

	// shortcut
	public static void checkPermission(String type, Permission permission) {
		Credentials credentials = Server.context().credentials();
		if (!roles(type).hasOne(credentials, permission))
			throw Exceptions.insufficientCredentials(credentials);
	}

}
