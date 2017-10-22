/**
 * © David Attias 2015
 */
package io.spacedog.model;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Sets;

import io.spacedog.utils.Credentials;

public class InternalDataSettings extends SettingsBase {

	public static final long serialVersionUID = 4064111112532790399L;

	public DataAclMap acl;

	public InternalDataSettings() {
		acl = new DataAclMap();
	}

	//
	// acl business logic
	//

	public boolean check(String type, String role, Permission... permissions) {
		RolePermissions roles = acl.get(type);
		return roles == null ? false : roles.check(role, permissions);
	}

	public boolean check(Credentials credentials, String type, Permission... permissions) {
		if (credentials.isAtLeastSuperAdmin())
			return true;

		RolePermissions roles = acl.get(type);
		return roles == null ? false : roles.check(credentials, permissions);
	}

	public String[] types(Permission permission, Credentials credentials) {
		Set<String> types = Sets.newHashSet();

		if (credentials.isAtLeastSuperAdmin())
			types.addAll(acl.keySet());
		else
			for (String type : acl.keySet()) {
				if (check(type, Credentials.ALL_ROLE, permission))
					types.add(type);
				else
					for (String role : credentials.roles())
						if (check(type, role, permission))
							types.add(type);
			}

		return types.toArray(new String[types.size()]);
	}

	public static class DataAclMap extends HashMap<String, RolePermissions> {

		private static final long serialVersionUID = 8813814959454404912L;
	}
}
