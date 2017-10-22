/**
 * © David Attias 2015
 */
package io.spacedog.server;

import org.elasticsearch.action.index.IndexResponse;

import io.spacedog.model.Permission;
import io.spacedog.model.Schema.DataAcl;
import io.spacedog.model.InternalDataSettings;
import io.spacedog.utils.Credentials;

public class DataAccessControl {

	public static boolean check(String type, Permission... permissions) {
		return check(SpaceContext.credentials(), type, permissions);
	}

	public static boolean check(Credentials credentials, String type, Permission... permissions) {
		return schemaSettings()//
				.check(credentials, type, permissions);
	}

	public static String[] types(Permission permission, Credentials credentials) {
		return schemaSettings().types(permission, credentials);
	}

	public static void save(String type, DataAcl schemaAcl) {

		InternalDataSettings settings = schemaSettings();
		if (schemaAcl == null)
			schemaAcl = DataAcl.defaultAcl();

		settings.acl.put(type, schemaAcl);
		schemaSettings(settings);
	}

	public static void delete(String type) {

		InternalDataSettings settings = schemaSettings();
		settings.acl.remove(type);
		schemaSettings(settings);
	}

	private static InternalDataSettings schemaSettings() {
		return SettingsService.get().getAsObject(InternalDataSettings.class);
	}

	private static IndexResponse schemaSettings(InternalDataSettings settings) {
		return SettingsService.get().setAsObject(settings);
	}

}
