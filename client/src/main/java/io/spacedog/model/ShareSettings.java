/**
 * © David Attias 2015
 */
package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShareSettings extends SettingsBase {

	public boolean enableS3Location = false;
	public RolePermissions permissions = new RolePermissions();
}
