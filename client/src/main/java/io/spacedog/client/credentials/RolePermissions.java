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

	public RolePermissions put(String role, Permission... permissions) {
		put(role, Sets.newHashSet(permissions));
		return this;
	}

	public boolean hasOne(String role, Permission... permissions) {

		Set<Permission> rolePermissions = get(role);

		if (rolePermissions == null)
			return false;

		for (Permission permission : permissions)
			if (rolePermissions.contains(permission))
				return true;

		return false;
	}

	public boolean hasOne(Credentials credentials, Permission... permissions) {
		if (credentials.isAtLeastSuperAdmin())
			return true;

		if (hasOne(Roles.all, permissions))
			return true;

		for (String role : credentials.roles())
			if (hasOne(role, permissions))
				return true;

		return false;
	}

	public void checkPermission(Credentials credentials, Permission... permissions) {
		if (!hasOne(credentials, permissions))
			throw Exceptions.insufficientPermissions(credentials);
	}

	public void checkReadPermission(Credentials credentials, String owner, String group) {
		check(credentials, owner, group, Permission.read, Permission.readGroup, Permission.readMine);
	}

	public void checkUpdatePermission(Credentials credentials, String owner, String group) {
		check(credentials, owner, group, Permission.update, Permission.updateGroup, Permission.updateMine);
	}

	public void checkDeletePermission(Credentials credentials, String owner, String group) {
		check(credentials, owner, group, Permission.delete, Permission.deleteGroup, Permission.deleteMine);
	}

	private void check(Credentials credentials, String owner, String group, //
			Permission allPermission, Permission groupPermission, Permission minePermission) {

		if (hasOne(credentials, allPermission))
			return;

		if (hasOne(credentials, groupPermission))
			if (credentials.hasGroupAccess(group))
				return;

		if (hasOne(credentials, minePermission))
			if (owner.equals(credentials.id()))
				return;

		throw Exceptions.insufficientPermissions(credentials);
	}

	public void checkGroupCreate(String group, Credentials credentials) {
		if (hasOne(credentials, Permission.create))
			return;

		if (hasOne(credentials, Permission.createGroup))
			credentials.checkGroupAccess(group);

		else if (hasOne(credentials, Permission.createMine))
			credentials.checkGroupIsMine(group);

		else
			throw Exceptions.insufficientPermissions(credentials);
	}

	public void checkGroupUpdate(String group, Credentials credentials) {
		if (hasOne(credentials, Permission.update))
			return;

		if (hasOne(credentials, Permission.updateGroup))
			credentials.checkGroupAccess(group);

		else if (hasOne(credentials, Permission.updateMine))
			credentials.checkGroupIsMine(group);

		else
			throw Exceptions.insufficientPermissions(credentials);
	}
}
