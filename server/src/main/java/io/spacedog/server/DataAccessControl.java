/**
 * © David Attias 2015
 */
package io.spacedog.server;

import org.elasticsearch.action.index.IndexResponse;

import io.spacedog.model.InternalDataAclSettings;
import io.spacedog.model.Permission;
import io.spacedog.model.RolePermissions;
import io.spacedog.utils.Credentials;

public class DataAccessControl {

	public static boolean check(String type, Permission... permissions) {
		return check(SpaceContext.credentials(), type, permissions);
	}

	public static boolean check(Credentials credentials, String type, Permission... permissions) {
		return getDataAclSettings()//
				.check(type, credentials, permissions);
	}

	public static String[] types(Credentials credentials, Permission permission) {
		return getDataAclSettings().accessList(credentials, permission);
	}

	public static void save(String type, RolePermissions schemaAcl) {
		InternalDataAclSettings settings = getDataAclSettings();
		if (schemaAcl == null)
			settings.remove(type);
		else
			settings.put(type, schemaAcl);
		saveDataAclSetting(settings);
	}

	public static void delete(String type) {
		InternalDataAclSettings settings = getDataAclSettings();
		settings.remove(type);
		saveDataAclSetting(settings);
	}

	private static InternalDataAclSettings getDataAclSettings() {
		return SettingsService.get().getAsObject(InternalDataAclSettings.class);
	}

	private static IndexResponse saveDataAclSetting(InternalDataAclSettings settings) {
		return SettingsService.get().saveAsObject(settings);
	}

}
