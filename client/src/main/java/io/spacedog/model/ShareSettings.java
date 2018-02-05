/**
 * © David Attias 2015
 */
package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShareSettings extends SettingsBase {

	public boolean enablePublicLocation = false;
	public RolePermissions permissions = new RolePermissions();
	public long sizeLimitInKB = 20000; // 20MB
}
