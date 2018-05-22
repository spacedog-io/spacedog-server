/**
 * © David Attias 2015
 */
package io.spacedog.client.credentials;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Sets;

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

		if (containsOne(Roles.all, permissions))
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

	public void checkRead(Credentials credentials, String owner, String group) {
		check(credentials, owner, group, Permission.read, Permission.readGroup, Permission.readMine);
	}

	public void checkUpdate(Credentials credentials, String owner, String group) {
		check(credentials, owner, group, Permission.update, Permission.updateGroup, Permission.updateMine);
	}

	public void checkDelete(Credentials credentials, String owner, String group) {
		check(credentials, owner, group, Permission.delete, Permission.deleteGroup, Permission.deleteMine);
	}

	public void check(Credentials credentials, String owner, String group, //
			Permission allPermission, Permission groupPermission, Permission minePermission) {

		if (containsOne(credentials, allPermission))
			return;

		if (containsOne(credentials, groupPermission))
			if (group.equals(credentials.group()))
				return;

		if (containsOne(credentials, minePermission))
			if (owner.equals(credentials.id()))
				return;

		throw Exceptions.insufficientCredentials(credentials);
	}
}
