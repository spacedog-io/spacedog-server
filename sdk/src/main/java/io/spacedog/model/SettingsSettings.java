package io.spacedog.model;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

// ignore unknown fields
@JsonIgnoreProperties(ignoreUnknown = true)
// only map to private fields
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
getterVisibility = Visibility.NONE, //
isGetterVisibility = Visibility.NONE, //
setterVisibility = Visibility.NONE)
public class SettingsSettings extends Settings {

	// only map to private fields
	@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
	getterVisibility = Visibility.NONE, //
	isGetterVisibility = Visibility.NONE, //
	setterVisibility = Visibility.NONE)
	public static class SettingsAcl {
		private Set<String> read;
		private Set<String> update;

		public Set<String> read() {
			Set<String> set = Sets.newHashSet("super_admin");
			if (this.read != null)
				set.addAll(this.read);
			return set;
		}

		public void read(String... roles) {
			this.read = Sets.newHashSet(roles);
			this.read.remove("super_admin");
		}

		public Set<String> update() {
			Set<String> set = Sets.newHashSet("super_admin");
			if (this.update != null)
				set.addAll(this.update);
			return set;
		}

		public void update(String... roles) {
			this.update = Sets.newHashSet(roles);
			this.update.remove("super_admin");
		}

		public static SettingsAcl defaultAcl() {
			return new SettingsAcl();
		}
	}

	private Map<String, SettingsAcl> acl;

	public SettingsAcl get(String settingsId) {
		if (acl != null) {
			if (acl.containsKey(settingsId))
				return acl.get(settingsId);
		}
		return SettingsAcl.defaultAcl();
	}

	public void put(String settingsId, SettingsAcl settingsAcl) {
		if (acl == null)
			acl = Maps.newHashMap();
		acl.put(settingsId, settingsAcl);
	}

	public void remove(String settingsId) {
		if (acl != null)
			acl.remove(settingsId);
	}
}
