package io.spacedog.client.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.credentials.RolePermissions;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileBucket {

	// public enum Storage {
	// system, elastic, s3;
	// }

	public FileBucket() {
	}

	public FileBucket(String name) {
		this.name = name;
	}

	public String name;
	// public Storage storage = Storage.system;
	public long sizeLimitInKB = 20000; // 20MB
	public boolean isWebEnabled;
	public String notFoundPage = "/404.html";
	public RolePermissions permissions = new RolePermissions();
}