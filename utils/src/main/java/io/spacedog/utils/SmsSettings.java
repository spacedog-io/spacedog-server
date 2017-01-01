package io.spacedog.utils;

import java.util.Map;
import java.util.Set;

public class SmsSettings extends Settings {
	public Set<String> rolesAllowedToSendSms;
	public TwilioSettings twilio;
	public Map<String, SmsTemplate> templates;

	public static class TwilioSettings {
		public String defaultFrom;
		public String accountSid;
		public String authToken;
	}
}