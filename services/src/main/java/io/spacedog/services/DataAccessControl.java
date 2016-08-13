/**
 * © David Attias 2015
 */
package io.spacedog.services;

import io.spacedog.utils.Credentials;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Schema.SchemaAclSettings;
import io.spacedog.utils.SchemaSettings;

public class DataAccessControl {

	public static boolean check(String type, DataPermission... permissions) {
		return check(SpaceContext.checkCredentials(), type, permissions);
	}

	public static boolean check(Credentials credentials, String type, DataPermission... permissions) {
		return SettingsResource.get().load(SchemaSettings.class)//
				.check(credentials, type, permissions);
	}

	public static String[] types(DataPermission permission, Credentials credentials) {
		return SettingsResource.get().load(SchemaSettings.class)//
				.types(permission, credentials);
	}

	public static void save(String type, SchemaAclSettings schemaAcl) {

		SchemaSettings settings = SettingsResource.get().load(SchemaSettings.class);

		if (schemaAcl == null && !settings.acl.containsKey(type))
			schemaAcl = SchemaAclSettings.defaultSettings();

		settings.acl.put(type, schemaAcl);

		SettingsResource.get().save(settings);
	}
}
