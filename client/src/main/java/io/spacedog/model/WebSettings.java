package io.spacedog.model;

import io.spacedog.client.credentials.ObjectRolePermissions;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.settings.SettingsBase;

public class WebSettings extends SettingsBase {

	public String notFoundPage = "/404.html";
	public ObjectRolePermissions prefixPermissions = new ObjectRolePermissions()//
			.put("www", Roles.all, Permission.read);
}
