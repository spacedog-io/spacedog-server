/**
 * © David Attias 2015
 */
package io.spacedog.model;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.spacedog.utils.Credentials;

public class ShareSettings extends SettingsBase {

	public static final Map<String, Set<Permission>> defaultAcl = Maps.newHashMap();

	static {
		defaultAcl.put("all", Sets.newHashSet(Permission.read_all));
		defaultAcl.put("user", Sets.newHashSet(Permission.create, Permission.read_all, //
				Permission.delete));
		defaultAcl.put("admin", Sets.newHashSet(Permission.create, Permission.search, //
				Permission.delete_all));
	}

	public Map<String, Set<Permission>> acl;
	public boolean enableS3Location = true;

	public ShareSettings() {
		this.acl = defaultAcl;
	}

	public boolean check(String role, Permission... permissions) {
		Set<Permission> rolePermissions = acl.get(role);
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
}
