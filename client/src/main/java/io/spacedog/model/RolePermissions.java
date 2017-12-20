/**
 * © David Attias 2015
 */
package io.spacedog.model;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Sets;

import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;

@SuppressWarnings("serial")
public class RolePermissions extends HashMap<String, Set<Permission>> {

	public RolePermissions() {
	}

	public boolean containsOne(String role, Permission... permissions) {

		Set<Permission> rolePermissions = get(role);

		if (rolePermissions == null)
			return false;

		for (Permission permission : permissions)
			if (rolePermissions.contains(permission))
				return true;

		return false;
	}

	public boolean containsOne(Credentials credentials, Permission... permissions) {
		if (credentials.isAtLeastSuperAdmin())
			return true;

		if (containsOne("all", permissions))
			return true;

		for (String role : credentials.roles())
			if (containsOne(role, permissions))
				return true;

		return false;
	}

	public void check(Credentials credentials, Permission... permissions) {
		if (!containsOne(credentials, permissions))
			throw Exceptions.insufficientCredentials(credentials);
	}

	public RolePermissions put(String role, Permission... permissions) {
		put(role, Sets.newHashSet(permissions));
		return this;
	}
}
