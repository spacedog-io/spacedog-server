/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.spacedog.utils.Credentials;
import io.spacedog.utils.DataAclSettings;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.Schema.SchemaAclSettings;

public class DataAccessControl {

	public static final String ACL_SETTINGS_ID = "dataAcl";

	public static boolean check(String type, DataPermission... permissions) {
		return check(SpaceContext.checkCredentials(), type, permissions);
	}

	public static boolean check(Credentials credentials, String type, DataPermission... permissions) {
		return load(credentials.backendId()).check(credentials, type, permissions);
	}

	public static String[] types(DataPermission permission, Credentials credentials) {
		return load(credentials.backendId()).types(permission, credentials);
	}

	private static DataAclSettings load(String backendId) {

		try {
			String acl = SettingsResource.get().doGet(ACL_SETTINGS_ID);
			return Json.mapper().readValue(acl, DataAclSettings.class);

		} catch (IOException e) {
			throw Exceptions.runtime(e);
		} catch (NotFoundException e) {
			return new DataAclSettings();
		}

	}

	public static void save(String backendId, String type, //
			SchemaAclSettings schemaAcl) {

		DataAclSettings acl = load(backendId);

		if (schemaAcl == null && !acl.containsKey(type))
			schemaAcl = SchemaAclSettings.defaultSettings();

		acl.put(type, schemaAcl);

		try {
			SettingsResource.get().doPut(ACL_SETTINGS_ID, //
					Json.mapper().writeValueAsString(acl));
		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e);
		}
	}
}
