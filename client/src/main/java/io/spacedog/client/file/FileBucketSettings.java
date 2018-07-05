package io.spacedog.client.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.credentials.RolePermissions;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileBucketSettings {

	public FileBucketSettings() {
	}

	public FileBucketSettings(String name) {
		this.name = name;
	}

	public String name;
	public long sizeLimitInKB = 20000; // 20MB
	public boolean isWebEnabled;
	public String notFoundPage = "/404.html";
	public RolePermissions permissions = new RolePermissions();
}