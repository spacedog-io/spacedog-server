package io.spacedog.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsSettings extends SettingsBase {
	public Set<String> authorizedRoles;
	public TwilioSettings twilio;

	public static class TwilioSettings {
		public String defaultFrom;
		public String accountSid;
		public String authToken;
	}
}