/**
 * © David Attias 2015
 */
package io.spacedog.services;

import io.spacedog.utils.Credentials;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Schema.SchemaAcl;
import io.spacedog.utils.SchemaSettings;

public class DataAccessControl {

	public static boolean check(String type, DataPermission... permissions) {
		return check(SpaceContext.getCredentials(), type, permissions);
	}

	public static boolean check(Credentials credentials, String type, DataPermission... permissions) {
		return SettingsResource.get().load(SchemaSettings.class)//
				.check(credentials, type, permissions);
	}

	public static String[] types(DataPermission permission, Credentials credentials) {
		return SettingsResource.get().load(SchemaSettings.class)//
				.types(permission, credentials);
	}

	public static void save(String type, SchemaAcl schemaAcl) {

		SchemaSettings settings = SettingsResource.get().load(SchemaSettings.class);

		if (schemaAcl == null)
			schemaAcl = SchemaAcl.defaultAcl();

		settings.acl.put(type, schemaAcl);
		SettingsResource.get().save(settings);
	}

	public static void delete(String type) {

		SchemaSettings settings = SettingsResource.get().load(SchemaSettings.class);
		settings.acl.remove(type);
		SettingsResource.get().save(settings);
	}

}
