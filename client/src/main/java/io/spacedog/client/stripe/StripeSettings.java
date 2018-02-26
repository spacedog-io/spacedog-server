package io.spacedog.client.stripe;

import java.util.Set;

import io.spacedog.client.settings.SettingsBase;

public class StripeSettings extends SettingsBase {

	public String secretKey;
	public Set<String> rolesAllowedToCharge;
	public Set<String> rolesAllowedToPay;
}
