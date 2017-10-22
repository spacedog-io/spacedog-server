/**
 * © David Attias 2015
 */
package io.spacedog.model;

public class ShareSettings extends SettingsBase {

	public boolean enableS3Location = false;
	public RolePermissions sharePermissions = new RolePermissions();

	public ShareSettings() {
	}
}
