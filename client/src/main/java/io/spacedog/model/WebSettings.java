package io.spacedog.model;

import io.spacedog.utils.Roles;

public class WebSettings extends SettingsBase {

	public String notFoundPage = "/404.html";
	public ObjectRolePermissions prefixPermissions = new ObjectRolePermissions()//
			.put("www", Roles.all, Permission.read);
}
