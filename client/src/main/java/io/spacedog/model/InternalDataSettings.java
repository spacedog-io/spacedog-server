/**
 * © David Attias 2015
 */
package io.spacedog.model;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Sets;

import io.spacedog.model.Schema.DataAcl;
import io.spacedog.utils.Credentials;

public class InternalDataSettings extends Settings {

	public static final long serialVersionUID = 4064111112532790399L;

	public DataAclMap acl;

	public InternalDataSettings() {
		acl = new DataAclMap();
	}

	//
	// acl business logic
	//

	public boolean check(String type, String role, DataPermission... permissions) {
		DataAcl roles = acl.get(type);
		if (roles == null)
			roles = DataAcl.defaultAcl();

		Set<DataPermission> rolePermissions = roles.get(role);
		if (rolePermissions == null)
			return false;

		for (DataPermission permission : permissions)
			if (rolePermissions.contains(permission))
				return true;

		return false;
	}

	public boolean check(Credentials credentials, String type, DataPermission... permissions) {
		if (credentials.isAtLeastSuperAdmin())
			return true;

		if (check(type, Credentials.ALL_ROLE, permissions))
			return true;

		for (String role : credentials.roles())
			if (check(type, role, permissions))
				return true;

		return false;
	}

	public String[] types(DataPermission permission, Credentials credentials) {
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

	public static class DataAclMap extends HashMap<String, DataAcl> {

		private static final long serialVersionUID = 8813814959454404912L;
	}
}
