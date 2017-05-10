package io.spacedog.model;

import java.util.Set;

public class StripeSettings extends Settings {

	public String secretKey;
	public Set<String> rolesAllowedToCharge;
	public Set<String> rolesAllowedToPay;
}
