package io.spacedog.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.google.common.base.Strings;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Utils;

public class SpaceEnv {

	private Properties properties;

	public SpaceEnv() {
		this.properties = new Properties();
	}

	public SpaceEnv(Properties properties) {
		this.properties = properties;
	}

	public String superdogNotificationTopic() {
		return get("spacedog_superdog_notification_topic");
	}

	public boolean debug() {
		return get("spacedog.debug", true);
	}

	public void debug(boolean debug) {
		properties.setProperty("spacedog.debug", Boolean.toString(debug));
	}

	public SpaceTarget target() {
		return SpaceTarget.valueOf(get("spacedog.target", "production"));
	}

	public void target(SpaceTarget target) {
		properties.setProperty("spacedog.target", target.toString());
	}

	public int httpTimeoutMillis() {
		return get("spacedog.http.timeout", 30000);
	}

	public void httpTimeoutMillis(int timeout) {
		properties.setProperty("spacedog.http.timeout", Integer.toString(timeout));
	}

	//
	// Generic public methods
	//

	public void set(String propertyName, String propertyValue) {
		properties.setProperty(propertyName, propertyValue);
	}

	public String get(String propertyName, String defaultValue) {
		String value = doGet(propertyName);
		return value == null ? defaultValue : value;
	}

	public String get(String propertyName) {
		String value = doGet(propertyName);
		if (value == null)
			throw Exceptions.runtime("env property [%s] not set", propertyName);
		return value;
	}

	public boolean get(String propertyName, boolean defaultValue) {
		String value = doGet(propertyName);
		return (value == null) ? defaultValue : Boolean.parseBoolean(value);
	}

	public int get(String propertyName, int defaultValue) {
		String value = doGet(propertyName);
		return value == null ? defaultValue : Integer.parseInt(value);
	}

	public void log() {
		Utils.info("Env =");
		properties.forEach((key, value) -> Utils.info("-- %s: %s", key, value));
	}

	//
	// Implementation
	//

	private String doGet(String propertyName) {
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

	private static SpaceEnv defaultEnv;

	public static SpaceEnv defaultEnv() {
		if (defaultEnv == null) {

			try {
				InputStream input = null;
				String userHome = System.getProperty("user.home");

				if (!Strings.isNullOrEmpty(userHome)) {
					Path path = Paths.get(userHome, "spacedog", "spacedog.client.properties");

					if (Files.exists(path))
						input = Files.newInputStream(path);

					else {
						path = Paths.get(userHome, ".spacedog.client.properties");

						if (Files.exists(path))
							input = Files.newInputStream(path);
					}
				}

				Properties properties = new Properties();
				if (input != null)
					properties.load(input);

				defaultEnv = new SpaceEnv(properties);

				if (defaultEnv.debug())
					defaultEnv.log();

			} catch (IOException e) {
				throw Exceptions.runtime(e, "error loading env properties");
			}

		}

		return defaultEnv;
	}
}
