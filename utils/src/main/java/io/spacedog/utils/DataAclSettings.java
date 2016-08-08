/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Sets;

import io.spacedog.utils.Schema.SchemaAclSettings;

public class DataAclSettings extends HashMap<String, SchemaAclSettings> {

	private static final long serialVersionUID = 4064111112532790399L;

	public boolean check(String type, String role, DataPermission permission) {
		SchemaAclSettings roles = get(type);
		if (roles == null)
			roles = SchemaAclSettings.defaultSettings();
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

		for (String type : keySet())
			for (String role : credentials.roles())
				if (check(type, role, permission))
					types.add(type);

		return types.toArray(new String[types.size()]);
	}

	public DataAclSettings add(Schema schema) {
		put(schema.name(), schema.acl());
		return this;
	}
}
