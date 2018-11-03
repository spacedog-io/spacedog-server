package io.spacedog.client.http;

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

	private static final String DEBUG = "spacedog.debug";
	private static final String HTTP_TIMEOUT = "spacedog.http.timeout";
	private static final String SUPERDOG_PASSWORD = "spacedog.superdog.password";
	private static final String SUPERDOG_NOTIFICATION_TOPIC = "spacedog.superdog.notification.topic";
	private static final String BACKEND_WWW_PUBLIC_URL = "spacedog.backend.www.public.url";
	private static final String BACKEND_API_PUBLIC_URL = "spacedog.backend.api.public.url";

	private Properties properties;

	public SpaceEnv() {
		this.properties = new Properties();
	}

	public SpaceEnv(Properties properties) {
		this.properties = properties == null //
				? new Properties()
				: properties;
	}

	public Optional7<String> superdogNotificationTopic() {
		return get(SUPERDOG_NOTIFICATION_TOPIC);
	}

	public boolean debug() {
		return get(DEBUG, true);
	}

	public void debug(boolean debug) {
		properties.setProperty(DEBUG, Boolean.toString(debug));
	}

	private SpaceBackend apiBackend;

	public SpaceBackend apiBackend() {
		if (apiBackend == null)
			apiBackend = SpaceBackend.valueOf(//
					get(BACKEND_API_PUBLIC_URL, SpaceBackend.production.name()));
		return apiBackend;
	}

	public void apiBackend(SpaceBackend backend) {
		properties.setProperty(BACKEND_API_PUBLIC_URL, backend.toString());
	}

	private SpaceBackend wwwBackend;

	public SpaceBackend wwwBackend() {
		if (wwwBackend == null)
			wwwBackend = SpaceBackend.valueOf(//
					get(BACKEND_WWW_PUBLIC_URL, SpaceBackend.wwwProduction.name()));
		return wwwBackend;
	}

	public void wwwBackend(SpaceBackend backend) {
		wwwBackend = backend;
		properties.setProperty(BACKEND_WWW_PUBLIC_URL, backend.toString());
	}

	public int httpTimeoutMillis() {
		return get(HTTP_TIMEOUT, 30000);
	}

	public void httpTimeoutMillis(int timeout) {
		properties.setProperty(HTTP_TIMEOUT, Integer.toString(timeout));
	}

	private String superdogPassword;

	public String superdogPassword() {
		if (superdogPassword == null)
			superdogPassword = getOrElseThrow(SUPERDOG_PASSWORD);
		return superdogPassword;
	}

	//
	// Generic public methods
	//

	public Optional7<String> getFromSystem(String propertyName) {
		String value = System.getenv(propertyName);
		if (value == null)
			value = System.getProperty(propertyName.replaceAll("\\.", "_"));
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
		throw Exceptions.runtime("env property [%s] not found", propertyName);
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

	private static SpaceEnv env;

	public static SpaceEnv env() {
		if (env == null)
			env = defaultEnv();
		return env;
	}

	public static void env(SpaceEnv env) {
		SpaceEnv.env = env;
	}

	public static SpaceEnv defaultEnv() {
		Properties properties = tryCustomConfigurationPath();

		if (properties == null)
			properties = tryDefaultConfigurationPaths();

		SpaceEnv defaultEnv = new SpaceEnv(properties);

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
				"configuration file [%s] not found", path);
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
			Utils.closeSilently(input);
		}
	}
}
