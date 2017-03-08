/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ShareSettings extends Settings {

	public static final Map<String, Set<DataPermission>> defaultAcl = Maps.newHashMap();

	static {
		defaultAcl.put("key", Sets.newHashSet(DataPermission.read_all));
		defaultAcl.put("user", Sets.newHashSet(DataPermission.create, DataPermission.read_all, //
				DataPermission.delete));
		defaultAcl.put("admin", Sets.newHashSet(DataPermission.create, DataPermission.search, //
				DataPermission.delete_all));
	}

	public Map<String, Set<DataPermission>> acl;
	public boolean enableS3Location = true;

	public ShareSettings() {
		this.acl = defaultAcl;
	}

	public boolean check(String role, DataPermission... permissions) {
		Set<DataPermission> rolePermissions = acl.get(role);
		if (rolePermissions == null)
			return false;
		for (DataPermission permission : permissions)
			if (rolePermissions.contains(permission))
				return true;
		return false;
	}

	public boolean check(Credentials credentials, DataPermission... permissions) {
		for (String role : credentials.roles())
			if (check(role, permissions))
				return true;

		return false;
	}
}
