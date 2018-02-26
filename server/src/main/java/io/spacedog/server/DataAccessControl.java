/**
 * © David Attias 2015
 */
package io.spacedog.server;

import org.elasticsearch.action.index.IndexResponse;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.RolePermissions;
import io.spacedog.client.data.InternalDataAclSettings;

public class DataAccessControl {

	public static RolePermissions roles(String type) {
		return getDataAclSettings().get(type);
	}

	public static String[] types(Credentials credentials, Permission permission) {
		return getDataAclSettings().accessList(credentials, permission);
	}

	public static void save(String type, RolePermissions schemaAcl) {
		InternalDataAclSettings settings = getDataAclSettings();
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
