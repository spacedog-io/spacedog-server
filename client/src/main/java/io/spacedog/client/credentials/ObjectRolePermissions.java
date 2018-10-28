/**
 * © David Attias 2015
 */
package io.spacedog.client.credentials;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Sets;

@SuppressWarnings("serial")
public class ObjectRolePermissions extends HashMap<String, RolePermissions> {

	public ObjectRolePermissions put(String objectId, String role, Permission... permissions) {
		RolePermissions rolePermissions = get(objectId);
		rolePermissions.put(role, permissions);
		return this;
	}

	public boolean hasOne(String objectId, String role, Permission... permissions) {
		RolePermissions roles = super.get(objectId);
		return roles == null ? false : roles.hasOne(role, permissions);
	}

	@Override
	public RolePermissions get(Object objectId) {
		RolePermissions roles = super.get(objectId);
		if (roles == null) {
			roles = new RolePermissions();
			super.put(objectId.toString(), roles);
		}
		return roles;
	}

	public Set<String> accessList(Credentials credentials, Permission permission) {
		Set<String> ids = Sets.newHashSet();

		if (credentials.isAtLeastSuperAdmin())
			ids.addAll(keySet());
		else
			for (String id : keySet()) {
				if (hasOne(id, Roles.all, permission))
					ids.add(id);
				else
					for (String role : credentials.roles())
						if (hasOne(id, role, permission))
							ids.add(id);
			}

		return ids;
	}

}
