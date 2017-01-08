package io.spacedog.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.google.common.base.Strings;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Internals;
import io.spacedog.utils.Utils;

public class SpaceRequestConfiguration {

	private Properties properties;

	public SpaceRequestConfiguration() {
		this.properties = new Properties();
	}

	public SpaceRequestConfiguration(Properties properties) {
		this.properties = new Properties(properties);
	}

	public String superdogName() {
		return getProperty("spacedog.superdog.username");
	}

	public String superdogPassword() {
		return getProperty("spacedog.superdog.password");
	}

	public String superdogNotificationTopic() {
		return getProperty("spacedog.superdog.notification.topic");
	}

	public boolean debug() {
		return Boolean.valueOf(getProperty("spacedog.debug", "true"));
	}

	public void debug(boolean debug) {
		properties.setProperty("spacedog.debug", Boolean.toString(debug));
	}

	public SpaceTarget target() {
		return SpaceTarget.valueOf(getProperty("spacedog.target", "production"));
	}

	public void target(SpaceTarget target) {
		properties.setProperty("spacedog.target", target.toString());
	}

	public int httpTimeoutMillis() {
		return Integer.valueOf(getProperty("spacedog.http.timeout", "30000"));
	}

	public void httpTimeoutMillis(int timeout) {
		properties.setProperty("spacedog.http.timeout", Integer.toString(timeout));
	}

	public String testSmtpLogin() {
		return getProperty("spacedog.test.smtp.login");
	}

	public String testSmtpPassword() {
		return getProperty("spacedog.test.smtp.password");
	}

	public String testLinkedinClientId() {
		return getProperty("spacedog.test.linkedin.client.id");
	}

	public String testLinkedinClientSecret() {
		return getProperty("spacedog.test.linkedin.client.secret");
	}

	public String testSendPulseClientId() {
		return getProperty("spacedog.test.sendpulse.client.id");
	}

	public String testSendPulseClientSecret() {
		return getProperty("spacedog.test.sendpulse.client.secret");
	}

	public String testStripeSecretKey() {
		return getProperty("spacedog.test.stripe.secret.key");
	}

	public String cesioSuperAdminUsername() {
		return getProperty("spacedog.cesio.superadmin.username");
	}

	public String cesioSuperAdminPassword() {
		return getProperty("spacedog.cesio.superadmin.password");
	}

	//
	// Generic public methods
	//

	public String getProperty(String propertyName, String defaultValue) {
		String value = get(propertyName);
		return value == null ? defaultValue : value;
	}

	public String getProperty(String propertyName) {
		String value = get(propertyName);
		if (value == null)
			throw Exceptions.runtime("configuration property [%s] not set", propertyName);
		return value;
	}

	public boolean getBoolean(String propertyName, boolean defaultValue) {
		String value = get(propertyName);
		return (value == null) ? defaultValue : Boolean.parseBoolean(value);
	}

	public int getInt(String propertyName, int defaultValue) {
		String value = get(propertyName);
		return value == null ? defaultValue : Integer.parseInt(value);
	}

	//
	// Implementation
	//

	private String get(String propertyName) {
		String value = System.getenv(propertyName);
		if (value == null)
			value = System.getProperty(propertyName);
		if (value == null)
			value = properties.getProperty(propertyName);
		return value;
	}

	//
	// Singleton
	//

	private static SpaceRequestConfiguration configuration;

	public static SpaceRequestConfiguration get() {
		if (configuration == null) {

			try {
				InputStream input = null;
				String userHome = System.getProperty("user.home");

				if (!Strings.isNullOrEmpty(userHome)) {
					Path path = Paths.get(userHome, ".spacedog.client.properties");

					if (Files.exists(path))
						input = Files.newInputStream(path);

					else {
						path = Paths.get(userHome, "spacedog", "spacedog.client.properties");
						if (Files.exists(path))
							input = Files.newInputStream(path);
					}
				}

				try {
					// TODO
					// remove this when watchdog jobs are using env properties
					if (input == null)
						input = Internals.get()//
								.getFile("spacedog-artefact", "spacedog.client.properties");
				} catch (Exception ignore) {
				}

				Properties properties = new Properties();
				if (input != null)
					properties.load(input);

				Utils.info("Configuration properties = %s", properties);

				configuration = new SpaceRequestConfiguration(properties);

			} catch (IOException e) {
				throw Exceptions.runtime(e, "error loading configuration properties");
			}

		}

		return configuration;
	}
}
