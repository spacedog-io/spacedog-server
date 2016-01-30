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

		checkPath("configuration file", configFilePath(), true, false);

		configuration.load(Files.newInputStream(configFilePath()));

		check("production", isProduction(), true);

		check("url", url(), true);
		check("main port", mainPort(), true);
		check("redirect port", redirectPort(), true);

		checkPath("home path", homePath(), true, true);
		checkPath("elasticsearch data path", elasticDataPath(), true, true);

		check("mail domain", mailDomain(), true);
		check("mailgun key", mailGunKey(), true);

		check("snapshots path", snapshotsPath(), false);
		check("snapshots bucket name", snapshotsBucketName(), true);
		check("snapshots bucket region", snapshotsBucketRegion(), true);

		check("superdogs", superdogs(), true);
		for (String superdog : superdogs()) {
			check("superdog username", superdog, true);
			check("superdog email", superdogEmail(superdog), true);
			check("superdog hashed password", superdogHashedPassword(superdog), true);
		}

		checkPath("SSL CRT file path", crtFilePath(), false, false);
		checkPath("SSL PEM file path", pemFilePath(), false, false);
		checkPath("SSL DER file path", derFilePath(), false, false);

		check("superdog notification topic", superdogNotificationTopic(), true);
		check("bucket root name", getBucketRootName(), true);
		check("offline server", isOffline(), true);

		if (isProduction()) {
			// Force Fluent HTTP to production mode
			System.setProperty("PROD_MODE", "true");
		}

	}

	private void check(String property, Object value, boolean required) {

		if (value != null && !value.toString().isEmpty())
			Utils.info("[SpaceDog] setting %s = %s", property, value);

		else if (required)
			throw new IllegalArgumentException(String.format(//
					"Configuration setting [%s] is required", property));
	}

	private void checkPath(String property, Optional<Path> path, boolean required, boolean directory) {
		if (path.isPresent())
			checkPath(property, path.get(), required, directory);
		else
			checkPath(property, (Path) null, required, directory);
	}

	private void checkPath(String property, Path path, boolean required, boolean directory) {

		if (path != null) {

			if (directory && !Files.isDirectory(path))
				throw new IllegalArgumentException(String.format(//
						"Configuration setting [%s] is not a directory", property));

			if (!directory && !Files.isReadable(path))
				throw new IllegalArgumentException(String.format(//
						"Configuration setting [%s] is not a readable file", property));

			Utils.info("[SpaceDog] setting %s = %s", property, path);

		} else if (required)
			throw new IllegalArgumentException(String.format(//
					"Configuration setting [%s] is required", property));
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

	public String url() {
		return configuration.getProperty("spacedog.url", "https://spacedog.io");
	}

	public int mainPort() {
		return Integer.valueOf(configuration.getProperty("spacedog.port.main", "4444"));
	}

	public int redirectPort() {
		return Integer.valueOf(configuration.getProperty("spacedog.port.redirect", "8888"));
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
		return isProduction()//
				? Optional.empty()//
				: Optional.of(Paths.get(configuration.getProperty("spacedog.snapshots.path")));
	}

	public Optional<String> snapshotsBucketName() {
		return isProduction() //
				? Optional.of(configuration.getProperty("spacedog.snapshots.bucket.name"))//
				: Optional.empty();
	}

	public Optional<String> snapshotsBucketRegion() {
		return isProduction() //
				? Optional.of(configuration.getProperty("spacedog.snapshots.bucket.region"))//
				: Optional.empty();
	}

	// public String[] superdogs() {
	// String value = configuration.getProperty("spacedog.superdogs");
	//
	// if (Strings.isNullOrEmpty(value))
	// return null;
	//
	// return value.split(", ");
	// }

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

	public String getBucketRootName() {
		return configuration.getProperty("spacedog.bucket.root.name");
	}

	public boolean isOffline() {
		return Boolean.parseBoolean(configuration.getProperty("spacedog.offline", "false"));
	}
}
