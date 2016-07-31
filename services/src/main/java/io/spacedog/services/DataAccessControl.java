/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Schema.DataTypeAccessControl;

public class DataAccessControl extends HashMap<String, DataTypeAccessControl> {

	private static final String ACL_SETTINGS_ID = "acl";
	private static final long serialVersionUID = 4064111112532790399L;

	public boolean check(String type, String role, DataPermission permission) {
		DataTypeAccessControl roles = get(type);
		if (roles == null)
			roles = defaultRoles();
		Set<DataPermission> permissions = roles.get(role);
		if (permissions == null)
			return false;
		return permissions.contains(permission);
	}

	private static DataTypeAccessControl defaultRoles() {

		DataTypeAccessControl roles = new DataTypeAccessControl();

		roles.put("key", Sets.newHashSet(DataPermission.read_all));

		roles.put("user", Sets.newHashSet(DataPermission.create, //
				DataPermission.update, DataPermission.search, DataPermission.delete));

		roles.put("admin", Sets.newHashSet(DataPermission.create, //
				DataPermission.update_all, DataPermission.search, DataPermission.delete_all));

		return roles;
	}

	public static boolean check(String type, DataPermission... permissions) {
		return check(SpaceContext.checkCredentials(), type, permissions);
	}

	public static boolean check(Credentials credentials, String type, DataPermission... permissions) {
		DataAccessControl acl = load(credentials.backendId());

		for (String role : credentials.roles())
			for (DataPermission permission : permissions)
				if (acl.check(type, role, permission))
					return true;

		return false;
	}

	public static String[] types(DataPermission permission, Credentials credentials) {
		DataAccessControl acl = load(credentials.backendId());
		Set<String> types = Sets.newHashSet();
		for (String type : acl.keySet()) {
			for (String role : credentials.roles()) {
				if (acl.check(type, role, permission))
					types.add(type);
			}
		}
		return types.toArray(new String[types.size()]);
	}

	private static DataAccessControl load(String backendId) {

		try {
			String acl = SettingsResource.get().doGet(ACL_SETTINGS_ID);
			return Json.mapper().readValue(acl, DataAccessControl.class);

		} catch (IOException e) {
			throw Exceptions.runtime(e);
		} catch (NotFoundException e) {
			return new DataAccessControl();
		}

	}

	public static void save(String backendId, String type, //
			DataTypeAccessControl schemaAcl) {

		DataAccessControl acl = load(backendId);

		if (schemaAcl == null && !acl.containsKey(type))
			schemaAcl = defaultRoles();

		acl.put(type, schemaAcl);

		try {
			SettingsResource.get().doPut(ACL_SETTINGS_ID, //
					Json.mapper().writeValueAsString(acl));
		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e);
		}
	}
}
