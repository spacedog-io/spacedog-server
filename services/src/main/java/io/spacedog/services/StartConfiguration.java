package io.spacedog.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.spacedog.utils.Utils;

public class StartConfiguration {

	private Properties configuration = new Properties();

	public StartConfiguration() throws IOException {

		checkPath("configuration file", configFilePath(), false);

		configuration.load(Files.newInputStream(configFilePath()));

		check("production", isProduction());
		check("offline", isOffline());

		check("ssl url", sslUrl());
		check("ssl port", sslPort());
		check("non ssl url", nonSslUrl());
		check("non ssl port", nonSslPort());

		checkPath("home path", homePath(), true);
		checkPath("elasticsearch data path", elasticDataPath(), true);

		check("AWS region", awsRegion());
		check("AWS bucket prefix", getAwsBucketPrefix());

		check("mail domain", mailDomain());
		check("mailgun key", mailGunKey());

		checkPath("snapshots path", snapshotsPath(), true);

		check("superdogs", superdogs());
		for (String superdog : superdogs()) {
			check("superdog username", superdog);
			check("superdog email", superdogEmail(superdog));
			check("superdog hashed password", superdogHashedPassword(superdog));
		}

		checkPath("SSL CRT file path", crtFilePath(), false);
		checkPath("SSL PEM file path", pemFilePath(), false);
		checkPath("SSL DER file path", derFilePath(), false);

		check("superdog notification topic", superdogNotificationTopic());

		if (isProduction()) {
			// Force Fluent HTTP to production mode
			System.setProperty("PROD_MODE", "true");
		}

	}

	private void check(String property, Object value) {

		if (value == null || value.toString().isEmpty())
			throw new IllegalArgumentException(String.format(//
					"Configuration setting [%s] is required", property));

		Utils.info("[SpaceDog] setting %s = %s", property, value);
	}

	private void checkPath(String property, Optional<Path> path, boolean directory) {
		if (path.isPresent())
			checkPath(property, path.get(), directory);
	}

	private void checkPath(String property, Path path, boolean directory) {

		if (path == null)
			throw new IllegalArgumentException(String.format(//
					"Configuration setting [%s] is required", property));

		if (directory && !Files.isDirectory(path))
			throw new IllegalArgumentException(String.format(//
					"Configuration setting [%s] is not a directory", property));

		if (!directory && !Files.isReadable(path))
			throw new IllegalArgumentException(String.format(//
					"Configuration setting [%s] is not a readable file", property));

		Utils.info("[SpaceDog] setting %s = %s", property, path);
	}

	public Path homePath() {
		String path = configuration.getProperty("spacedog.home.path");
		return path == null //
				? Paths.get(System.getProperty("user.home"), "spacedog")//
				: Paths.get(path);
	}

	public Path configFilePath() {
		String path = System.getProperty("spacedog.configuration.file");
		return path == null //
				? homePath().resolve("spacedog.server.properties")//
				: Paths.get(path);
	}

	public String sslUrl() {
		return sslUrl(null);
	}

	public String sslUrl(String backendId) {
		String url = configuration.getProperty("spacedog.url.ssl");
		return Strings.isNullOrEmpty(backendId)//
				? String.format(url, "")//
				: String.format(url, backendId + '.');
	}

	public String nonSslUrl() {
		return configuration.getProperty("spacedog.url.nonssl");
	}

	public int sslPort() {
		return Integer.valueOf(configuration.getProperty("spacedog.port.ssl"));
	}

	public int nonSslPort() {
		return Integer.valueOf(configuration.getProperty("spacedog.port.nonssl"));
	}

	public Path elasticDataPath() {
		String path = configuration.getProperty("spacedog.data.path");
		return path == null ? //
				homePath().resolve("data") : Paths.get(path);
	}

	public Optional<Path> derFilePath() {
		String path = configuration.getProperty("spacedog.ssl.der.file");
		return Strings.isNullOrEmpty(path) ? Optional.empty()//
				: Optional.of(Paths.get(path));
	}

	public Optional<Path> pemFilePath() {
		String path = configuration.getProperty("spacedog.ssl.pem.file");
		return Strings.isNullOrEmpty(path) ? Optional.empty()//
				: Optional.of(Paths.get(path));
	}

	public Optional<Path> crtFilePath() {
		String path = configuration.getProperty("spacedog.ssl.crt.file");
		return Strings.isNullOrEmpty(path) ? Optional.empty()//
				: Optional.of(Paths.get(path));
	}

	public boolean isProduction() {
		return Boolean.parseBoolean(configuration.getProperty("spacedog.production", "false"));
	}

	public String awsRegion() {
		return configuration.getProperty("spacedog.aws.region");
	}

	public String getAwsBucketPrefix() {
		return configuration.getProperty("spacedog.aws.bucket.prefix");
	}

	public boolean isSsl() {
		return crtFilePath().isPresent();
	}

	public String mailGunKey() {
		return configuration.getProperty("spacedog.mailgun.key");
	}

	public String mailDomain() {
		return configuration.getProperty("spacedog.mail.domain");
	}

	public Optional<Path> snapshotsPath() {
		String path = configuration.getProperty("spacedog.snapshots.path");
		return Strings.isNullOrEmpty(path) ? Optional.empty()//
				: Optional.of(Paths.get(path));
	}

	public List<String> superdogs() {
		List<String> superdogs = Lists.newArrayList();
		Enumeration<Object> keys = configuration.keys();
		while (keys.hasMoreElements()) {
			String value = keys.nextElement().toString();
			if (value.startsWith("spacedog.superdog.")//
					&& value.endsWith(".email"))
				superdogs.add(value.substring(18, value.length() - 6));
		}
		return superdogs;
	}

	public Optional<String> superdogHashedPassword(String username) {
		return Optional.ofNullable(configuration.getProperty("spacedog.superdog." + username + ".password"));
	}

	public boolean isSuperDog(String username) {
		return superdogHashedPassword(username).isPresent();
	}

	public Optional<String> superdogEmail(String username) {
		return Optional.ofNullable(configuration.getProperty("spacedog.superdog." + username + ".email"));
	}

	public String superdogNotificationTopic() {
		return configuration.getProperty("spacedog.superdog.notification.topic");
	}

	public boolean isOffline() {
		return Boolean.parseBoolean(configuration.getProperty("spacedog.offline", "false"));
	}
}
