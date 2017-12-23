/**
 * © David Attias 2015
 */
package io.spacedog.model;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Sets;

@SuppressWarnings("serial")
public class ObjectRolePermissions extends HashMap<String, RolePermissions> {

	public ObjectRolePermissions put(String objectId, String role, Permission... permissions) {
		RolePermissions rolePermissions = super.get(objectId);
		if (rolePermissions == null) {
			rolePermissions = new RolePermissions();
			super.put(objectId, rolePermissions);
		}
		rolePermissions.put(role, permissions);
		return this;
	}

	public boolean containsOne(String objectId, String role, Permission... permissions) {
		RolePermissions roles = super.get(objectId);
		return roles == null ? false : roles.containsOne(role, permissions);
	}

	@Override
	public RolePermissions get(Object objectId) {
		RolePermissions roles = super.get(objectId);
		return roles == null ? new RolePermissions() : roles;
	}

	public String[] accessList(Credentials credentials, Permission permission) {
		Set<String> ids = Sets.newHashSet();

		if (credentials.isAtLeastSuperAdmin())
			ids.addAll(keySet());
		else
			for (String id : keySet()) {
				if (containsOne(id, Roles.all, permission))
					ids.add(id);
				else
					for (String role : credentials.roles())
						if (containsOne(id, role, permission))
							ids.add(id);
			}

		return ids.toArray(new String[ids.size()]);
	}

}
