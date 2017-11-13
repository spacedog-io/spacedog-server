package io.spacedog.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MailSettings extends SettingsBase {
	public SmtpSettings smtp;
	public MailGunSettings mailgun;
	public Set<String> authorizedRoles = Collections.emptySet();
	public Map<String, MailTemplate> templates;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class MailGunSettings {
		public String domain;
		public String key;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SmtpSettings {
		public String host;
		public String login;
		public String password;
		public boolean startTlsRequired;
		public boolean sslOnConnect;
	}
}