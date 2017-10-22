/**
 * © David Attias 2015
 */
package io.spacedog.model;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Sets;

import io.spacedog.utils.Credentials;

public class RolePermissions extends HashMap<String, Set<Permission>> {

	private static final long serialVersionUID = -617928580943619516L;

	public RolePermissions() {
	}

	public boolean check(String role, Permission... permissions) {

		Set<Permission> rolePermissions = get(role);

		if (rolePermissions == null)
			return false;

		for (Permission permission : permissions)
			if (rolePermissions.contains(permission))
				return true;

		return false;
	}

	public boolean check(Credentials credentials, Permission... permissions) {
		if (credentials.isAtLeastSuperAdmin())
			return true;

		if (check("all", permissions))
			return true;

		for (String role : credentials.roles())
			if (check(role, permissions))
				return true;

		return false;
	}

	public RolePermissions put(String role, Permission... permissions) {
		put(role, Sets.newHashSet(permissions));
		return this;
	}
}
