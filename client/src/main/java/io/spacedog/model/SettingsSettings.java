package io.spacedog.model;

import java.util.Collections;
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
public class SettingsSettings extends SettingsBase {

	// only map to private fields
	@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class SettingsAcl {
		private Set<String> read = Collections.emptySet();
		private Set<String> update = Collections.emptySet();

		public Set<String> read() {
			return Sets.newHashSet(read);
		}

		public void read(String... roles) {
			this.read = Sets.newHashSet(roles);
		}

		public Set<String> update() {
			return Sets.newHashSet(update);
		}

		public void update(String... roles) {
			this.update = Sets.newHashSet(roles);
		}

		public static SettingsAcl defaultAcl() {
			return new SettingsAcl();
		}
	}

	private Map<String, SettingsAcl> acl;

	public SettingsAcl get(String settingsId) {
		if (acl != null)
			if (acl.containsKey(settingsId))
				return acl.get(settingsId);

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
