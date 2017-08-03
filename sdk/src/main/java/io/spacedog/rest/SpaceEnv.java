package io.spacedog.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

import com.google.common.base.Strings;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Optional7;
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
		return getOrElseThrow("spacedog_superdog_notification_topic");
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

	public Optional7<String> getFromSystem(String propertyName) {
		String value = System.getenv(propertyName);
		if (value == null)
			value = System.getProperty(propertyName);
		return Optional7.ofNullable(value);
	}

	public Optional7<String> get(String propertyName) {
		Optional7<String> value = getFromSystem(propertyName);
		return value.isPresent() ? value //
				: Optional7.ofNullable(properties.getProperty(propertyName));
	}

	public String getOrElseThrow(String propertyName) {
		Optional7<String> optional = get(propertyName);
		if (optional.isPresent())
			return optional.get();
		throw Exceptions.illegalState("env property [%s] not found", propertyName);
	}

	public void set(String propertyName, String propertyValue) {
		properties.setProperty(propertyName, propertyValue);
	}

	public String get(String propertyName, String defaultValue) {
		return get(propertyName).orElse(defaultValue);
	}

	public boolean get(String propertyName, boolean defaultValue) {
		Optional7<String> value = get(propertyName);
		return value.isPresent() //
				? Boolean.parseBoolean(value.get())
				: defaultValue;
	}

	public int get(String propertyName, int defaultValue) {
		Optional7<String> value = get(propertyName);
		return value.isPresent() //
				? Integer.parseInt(value.get())
				: defaultValue;
	}

	public void log() {
		Utils.info("[SpaceDog] Env =");
		for (Entry<Object, Object> property : properties.entrySet())
			Utils.info("-- %s: %s", property.getKey(), property.getValue());
	}

	//
	// Default env singleton
	//

	private static final String SPACEDOG_CONFIGURATION_PATH = "spacedog.configuration.path";

	private static SpaceEnv defaultEnv;

	public static SpaceEnv defaultEnv() {
		if (defaultEnv == null) {

			Properties properties = tryCustomConfigurationPath();

			if (properties == null)
				properties = tryDefaultConfigurationPaths();

			defaultEnv = new SpaceEnv(properties);

			if (defaultEnv.debug())
				defaultEnv.log();
		}
		return defaultEnv;
	}

	private static Properties tryCustomConfigurationPath() {
		String path = System.getProperty(SPACEDOG_CONFIGURATION_PATH);
		if (Strings.isNullOrEmpty(path))
			return null;

		File file = new File(path);
		if (file.isFile())
			return loadProperties(file);

		throw Exceptions.illegalArgument(//
				"SpaceDog configuration file [%s] not found", path);
	}

	private static Properties tryDefaultConfigurationPaths() {
		String userHome = System.getProperty("user.home");
		if (Strings.isNullOrEmpty(userHome))
			return null;

		File file = new File(userHome, "spacedog");
		if (file.isDirectory()) {
			file = new File(file, "spacedog.properties");
			if (file.isFile())
				return loadProperties(file);
		}

		file = new File(userHome, ".spacedog");
		if (file.isDirectory()) {
			file = new File(file, "spacedog.properties");
			if (file.isFile())
				return loadProperties(file);
		}

		return null;
	}

	private static Properties loadProperties(File file) {
		InputStream input = null;
		try {
			Utils.info("[SpaceDog] Loading configuration from [%s]", file);
			input = new FileInputStream(file);
			Properties properties = new Properties();
			properties.load(input);
			return properties;

		} catch (IOException e) {
			throw Exceptions.runtime(e, "error loading env properties");
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException ignore) {
					ignore.printStackTrace();
				}
		}
	}
}
