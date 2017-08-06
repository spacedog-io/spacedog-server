/**
 * © David Attias 2015
 */
package io.spacedog.model;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Sets;

import io.spacedog.model.Schema.SchemaAcl;
import io.spacedog.utils.Credentials;

public class SchemaSettings extends Settings implements NonDirectlyUpdatableSettings {

	public static final long serialVersionUID = 4064111112532790399L;

	public SchemaAclMap acl;

	public SchemaSettings() {
		acl = new SchemaAclMap();
	}

	//
	// acl business logic
	//

	public boolean check(String type, String role, DataPermission... permissions) {
		SchemaAcl roles = acl.get(type);
		if (roles == null)
			roles = SchemaAcl.defaultAcl();

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

		if (check(type, "all", permissions))
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
				if (check(type, "all", permission))
					types.add(type);
				else
					for (String role : credentials.roles())
						if (check(type, role, permission))
							types.add(type);
			}

		return types.toArray(new String[types.size()]);
	}

	public static class SchemaAclMap extends HashMap<String, SchemaAcl> {

		private static final long serialVersionUID = 8813814959454404912L;
	}
}
