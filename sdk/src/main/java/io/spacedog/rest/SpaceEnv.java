package io.spacedog.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
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
		// properties.forEach((key, value) -> Utils.info("-- %s: %s", key,
		// value));
		for (Entry<Object, Object> property : properties.entrySet())
			Utils.info("-- %s: %s", property.getKey(), property.getValue());
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
					File file = new File(userHome, "spacedog");
					if (file.isDirectory()) {
						file = new File(file, "spacedog.client.properties");
						if (file.exists())
							input = new FileInputStream(file);
					}

					if (input == null) {
						file = new File(userHome, ".spacedog.client.properties");
						if (file.exists())
							input = new FileInputStream(file);
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
