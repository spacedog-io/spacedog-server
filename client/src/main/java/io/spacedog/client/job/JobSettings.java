package io.spacedog.client.job;

import java.util.Set;

import io.spacedog.client.settings.SettingsBase;

public class JobSettings extends SettingsBase {

	public String secretKey;
	public Set<String> rolesAllowedToCharge;
	public Set<String> rolesAllowedToPay;
}
