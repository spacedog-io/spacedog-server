package io.spacedog.client.email;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.model.SettingsBase;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailSettings extends SettingsBase {
	public SmtpSettings smtp;
	public MailGunSettings mailgun;
	public Set<String> authorizedRoles = Collections.emptySet();

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