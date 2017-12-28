package io.spacedog.model;

import java.util.Set;

import com.google.common.collect.Sets;

public class PushSettings extends SettingsBase {

	public Set<String> authorizedRoles = Sets.newHashSet();
}
