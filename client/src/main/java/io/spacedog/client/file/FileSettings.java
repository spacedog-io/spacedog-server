/**
 * © David Attias 2015
 */
package io.spacedog.client.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.credentials.ObjectRolePermissions;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.settings.SettingsBase;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileSettings extends SettingsBase {

	public long sizeLimitInKB = 20000; // 20MB
	public ObjectRolePermissions permissions = new ObjectRolePermissions()//
			.put("www", Roles.all, Permission.read);
}
