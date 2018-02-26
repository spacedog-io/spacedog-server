package io.spacedog.client.push;

import java.util.Set;

import com.google.common.collect.Sets;

import io.spacedog.model.SettingsBase;

public class PushSettings extends SettingsBase {

	public Set<String> authorizedRoles = Sets.newHashSet();
}
