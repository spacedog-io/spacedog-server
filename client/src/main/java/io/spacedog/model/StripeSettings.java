package io.spacedog.model;

import java.util.Set;

public class StripeSettings extends SettingsBase {

	public String secretKey;
	public Set<String> rolesAllowedToCharge;
	public Set<String> rolesAllowedToPay;
}
