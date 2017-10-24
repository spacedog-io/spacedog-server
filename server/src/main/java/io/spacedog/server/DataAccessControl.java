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
		return dataAclSetting()//
				.check(type, credentials, permissions);
	}

	public static String[] types(Credentials credentials, Permission permission) {
		return dataAclSetting().accessList(credentials, permission);
	}

	public static void save(String type, RolePermissions schemaAcl) {
		if (schemaAcl == null)
			schemaAcl = new RolePermissions();

		InternalDataAclSettings settings = dataAclSetting();
		settings.put(type, schemaAcl);
		dataAclSetting(settings);
	}

	public static void delete(String type) {

		InternalDataAclSettings settings = dataAclSetting();
		settings.remove(type);
		dataAclSetting(settings);
	}

	private static InternalDataAclSettings dataAclSetting() {
		return SettingsService.get().getAsObject(InternalDataAclSettings.class);
	}

	private static IndexResponse dataAclSetting(InternalDataAclSettings settings) {
		return SettingsService.get().setAsObject(settings);
	}

}
