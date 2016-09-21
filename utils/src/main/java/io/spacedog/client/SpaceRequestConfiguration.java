package io.spacedog.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.google.common.base.Strings;

import io.spacedog.utils.Internals;

public class SpaceRequestConfiguration {

	private Properties properties;

	public SpaceRequestConfiguration() {
		properties = new Properties();
	}

	public SpaceRequestConfiguration(Properties properties) {
		properties = new Properties(properties);
	}

	public SpaceRequestConfiguration(InputStream stream) throws IOException {
		properties = new Properties();
		properties.load(stream);
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
		return Boolean.valueOf(getProperty("spacedog.debug", "false"));
	}

	public void debug(boolean debug) {
		properties.setProperty("spacedog.debug", Boolean.toString(debug));
	}

	public SpaceTarget target() {
		return SpaceTarget.valueOf(getProperty("spacedog.target", "local"));
	}

	public void target(SpaceTarget target) {
		properties.setProperty("spacedog.target", target.toString());
	}

	public int httpTimeoutMillis() {
		return Integer.valueOf(configuration.getProperty("spacedog.http.timeout", "10000"));
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

	public String cesioSuperAdminUsername() {
		return getProperty("spacedog.cesio.superadmin.username");
	}

	public String cesioSuperAdminPassword() {
		return getProperty("spacedog.cesio.superadmin.password");
	}

	//
	// Implementation
	//

	public String getProperty(String key, String defaultValue) {
		String value = System.getProperty(key);
		if (Strings.isNullOrEmpty(value))
			value = properties.getProperty(key, defaultValue);
		return value;
	}

	public String getProperty(String key) {
		String value = System.getProperty(key);
		if (Strings.isNullOrEmpty(value))
			value = properties.getProperty(key);
		if (Strings.isNullOrEmpty(value))
			throw new IllegalStateException(String.format("configuration property [%s] not set", key));
		return value;
	}

	//
	// Singleton
	//

	private static SpaceRequestConfiguration configuration;

	public static SpaceRequestConfiguration get() {
		try {
			if (configuration == null) {

				String userHome = System.getProperty("user.home");

				if (!Strings.isNullOrEmpty(userHome)) {
					Path path = Paths.get(userHome, ".spacedog.client.properties");
					if (Files.exists(path))
						configuration = new SpaceRequestConfiguration(Files.newInputStream(path));
					else {
						path = Paths.get(userHome, "spacedog", "spacedog.client.properties");
						if (Files.exists(path))
							configuration = new SpaceRequestConfiguration(Files.newInputStream(path));
					}
				}

				if (configuration == null)
					configuration = new SpaceRequestConfiguration(//
							Internals.get().getFile("spacedog-artefact", "spacedog.client.properties"));
			}
			return configuration;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
