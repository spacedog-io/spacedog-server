/**
 * © David Attias 2015
 */
package io.spacedog.model;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Sets;

import io.spacedog.utils.Credentials;

@SuppressWarnings("serial")
public class ObjectRolePermissions extends HashMap<String, RolePermissions> {

	public ObjectRolePermissions put(String objectId, String role, Permission... permissions) {
		RolePermissions rolePermissions = get(objectId);
		if (rolePermissions == null) {
			rolePermissions = new RolePermissions();
			super.put(objectId, rolePermissions);
		}
		rolePermissions.put(role, permissions);
		return this;
	}

	public boolean check(String objectId, String role, Permission... permissions) {
		RolePermissions roles = get(objectId);
		return roles == null ? false : roles.check(role, permissions);
	}

	public boolean check(String objectId, Credentials credentials, Permission... permissions) {
		if (credentials.isAtLeastSuperAdmin())
			return true;

		RolePermissions roles = get(objectId);
		return roles == null ? false : roles.check(credentials, permissions);
	}

	public String[] accessList(Credentials credentials, Permission permission) {
		Set<String> ids = Sets.newHashSet();

		if (credentials.isAtLeastSuperAdmin())
			ids.addAll(keySet());
		else
			for (String id : keySet()) {
				if (check(id, Credentials.ALL_ROLE, permission))
					ids.add(id);
				else
					for (String role : credentials.roles())
						if (check(id, role, permission))
							ids.add(id);
			}

		return ids.toArray(new String[ids.size()]);
	}

}
