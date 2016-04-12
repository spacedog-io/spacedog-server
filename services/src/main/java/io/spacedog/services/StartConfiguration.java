package io.spacedog.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import com.google.common.base.Strings;

import io.spacedog.utils.Utils;

public class StartConfiguration {

	private Properties configuration = new Properties();

	public StartConfiguration() throws IOException {

		checkPath("configuration file", configFilePath(), false);

		configuration.load(Files.newInputStream(configFilePath()));

		checkPath("home path", homePath(), true);
		check("production", isProduction());
		check("offline", isOffline());

		check("ssl url", sslUrl());
		check("ssl port", sslPort());
		check("non ssl url", nonSslUrl());
		check("non ssl port", nonSslPort());

		checkPath("elastic data path", elasticDataPath(), true);
		check("elastic http enabled", isElasticHttpEnabled());
		checkPath("elastic snapshots path", snapshotsPath(), true);

		check("AWS region", awsRegion());
		check("AWS bucket prefix", getAwsBucketPrefix());
		check("AWS superdog notification topic", superdogAwsNotificationTopic());

		check("mail domain", mailDomain());
		check("mailgun key", mailGunKey());

		if (isProduction()) {
			// Force Fluent HTTP to production mode
			System.setProperty("PROD_MODE", "true");
		}

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
		String path = configuration.getProperty("spacedog.elastic.data.path");
		return path == null ? //
				homePath().resolve("data") : Paths.get(path);
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

	public String mailGunKey() {
		return configuration.getProperty("spacedog.mail.mailgun.key");
	}

	public String mailDomain() {
		return configuration.getProperty("spacedog.mail.domain");
	}

	public Optional<Path> snapshotsPath() {
		String path = configuration.getProperty("spacedog.elastic.snapshots.path");
		return Strings.isNullOrEmpty(path) ? Optional.empty()//
				: Optional.of(Paths.get(path));
	}

	public String superdogAwsNotificationTopic() {
		return configuration.getProperty("spacedog.aws.superdog.notification.topic");
	}

	public boolean isOffline() {
		return Boolean.parseBoolean(configuration.getProperty("spacedog.offline", "false"));
	}

	public boolean isElasticHttpEnabled() {
		return Boolean.parseBoolean(configuration.getProperty("spacedog.elastic.http.enabled", "false"));
	}

	//
	// Implementation
	//

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

}
