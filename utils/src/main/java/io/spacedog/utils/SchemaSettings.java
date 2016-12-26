/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Sets;

import io.spacedog.utils.Schema.SchemaAcl;

public class SchemaSettings extends Settings implements NonDirectlyUpdatableSettings {

	public static final long serialVersionUID = 4064111112532790399L;

	public SchemaAclMap acl;

	public SchemaSettings() {
		acl = new SchemaAclMap();
	}

	//
	// acl business logic
	//

	public boolean check(String type, String role, DataPermission permission) {
		SchemaAcl roles = acl.get(type);
		if (roles == null)
			roles = SchemaAcl.defaultAcl();
		Set<DataPermission> permissions = roles.get(role);
		if (permissions == null)
			return false;
		return permissions.contains(permission);
	}

	public boolean check(Credentials credentials, String type, DataPermission... permissions) {
		for (String role : credentials.roles())
			for (DataPermission permission : permissions)
				if (check(type, role, permission))
					return true;

		return false;
	}

	public String[] types(DataPermission permission, Credentials credentials) {
		Set<String> types = Sets.newHashSet();

		for (String type : acl.keySet())
			for (String role : credentials.roles())
				if (check(type, role, permission))
					types.add(type);

		return types.toArray(new String[types.size()]);
	}

	public static class SchemaAclMap extends HashMap<String, SchemaAcl> {

		private static final long serialVersionUID = 8813814959454404912L;
	}
}
