package io.spacedog.client.file;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.credentials.RolePermissions;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileBucket {

	public String name;
	public FileStoreType type = FileStoreType.fs;
	public long sizeLimitInKB = 20000; // 20MB
	public boolean isWebEnabled;
	public String notFoundPage = "/404.html";
	public RolePermissions permissions = new RolePermissions();

	public FileBucket() {
	}

	public FileBucket(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (!(obj instanceof FileBucket))
			return false;

		FileBucket fb = (FileBucket) obj;
		return Objects.equals(name, fb.name);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}

	@Override
	public String toString() {
		return String.format("FileBucket[%s]", name);
	}

}