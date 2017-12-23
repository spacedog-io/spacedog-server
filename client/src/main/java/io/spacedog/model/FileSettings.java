/**
 * © David Attias 2015
 */
package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.utils.Roles;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileSettings extends SettingsBase {

	public long sizeLimitInKB = 20000; // 20MB
	public ObjectRolePermissions permissions = new ObjectRolePermissions()//
			.put("www", Roles.all, Permission.read);
}
