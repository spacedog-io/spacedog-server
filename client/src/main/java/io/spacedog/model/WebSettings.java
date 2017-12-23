package io.spacedog.model;

public class WebSettings extends SettingsBase {

	public String notFoundPage = "/404.html";
	public ObjectRolePermissions prefixPermissions = new ObjectRolePermissions()//
			.put("www", Roles.all, Permission.read);
}
