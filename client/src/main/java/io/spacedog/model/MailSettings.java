package io.spacedog.model;

import java.util.Map;

public class MailSettings extends SettingsBase {
	public boolean enableUserFullAccess;
	public MailGunSettings mailgun;
	public SmtpSettings smtp;
	public Map<String, MailTemplate> templates;

	public static class MailGunSettings {
		public String domain;
		public String key;
	}

	public static class SmtpSettings {
		public String host;
		public String login;
		public String password;
		public boolean startTlsRequired;
		public boolean sslOnConnect;
	}
}