/**
 * © David Attias 2015
 */
package io.spacedog.client.file;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Maps;

import io.spacedog.client.credentials.RolePermissions;
import io.spacedog.client.settings.SettingsBase;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalFileSettings extends SettingsBase {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class FileBucketSettings {

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

	public Map<String, FileBucketSettings> buckets = Maps.newHashMap();
}
