/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.elasticsearch.action.index.IndexResponse;

import io.spacedog.model.DataPermission;
import io.spacedog.model.Schema.SchemaAcl;
import io.spacedog.model.SchemaSettings;
import io.spacedog.utils.Credentials;

public class DataAccessControl {

	public static boolean check(String type, DataPermission... permissions) {
		return check(SpaceContext.credentials(), type, permissions);
	}

	public static boolean check(Credentials credentials, String type, DataPermission... permissions) {
		return schemaSettings()//
				.check(credentials, type, permissions);
	}

	public static String[] types(DataPermission permission, Credentials credentials) {
		return schemaSettings().types(permission, credentials);
	}

	public static void save(String type, SchemaAcl schemaAcl) {

		SchemaSettings settings = schemaSettings();
		if (schemaAcl == null)
			schemaAcl = SchemaAcl.defaultAcl();

		settings.acl.put(type, schemaAcl);
		schemaSettings(settings);
	}

	public static void delete(String type) {

		SchemaSettings settings = schemaSettings();
		settings.acl.remove(type);
		schemaSettings(settings);
	}

	private static SchemaSettings schemaSettings() {
		return SettingsResource.get().getAsObject(SchemaSettings.class);
	}

	private static IndexResponse schemaSettings(SchemaSettings settings) {
		return SettingsResource.get().setAsObject(settings);
	}

}
