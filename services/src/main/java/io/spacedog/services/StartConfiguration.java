package io.spacedog.services;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import com.google.common.base.Strings;

import io.spacedog.utils.Backends;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Utils;

public class StartConfiguration {

	private static final String SPACEDOG_HOME_PATH = "spacedog.home.path";
	private static final String SPACEDOG_CONFIGURATION_FILE = "spacedog.configuration.file";
	private static final String SPACEDOG_ELASTIC_HTTP_ENABLED = "spacedog.elastic.http.enabled";
	private static final String SPACEDOG_ELASTIC_NETWORK_HOST = "spacedog.elastic.network.host";
	private static final String SPACEDOG_OFFLINE = "spacedog.offline";
	private static final String SPACEDOG_AWS_SUPERDOG_NOTIFICATION_TOPIC = "spacedog.aws.superdog.notification.topic";
	private static final String SPACEDOG_ELASTIC_SNAPSHOTS_PATH = "spacedog.elastic.snapshots.path";
	private static final String SPACEDOG_MAIL_SMTP_DEBUG = "spacedog.mail.smtp.debug";
	private static final String SPACEDOG_MAIL_DOMAIN = "spacedog.mail.domain";
	private static final String SPACEDOG_MAIL_MAILGUN_KEY = "spacedog.mail.mailgun.key";
	private static final String SPACEDOG_AWS_BUCKET_PREFIX = "spacedog.aws.bucket.prefix";
	private static final String SPACEDOG_AWS_REGION = "spacedog.aws.region";
	private static final String SPACEDOG_PRODUCTION = "spacedog.production";
	private static final String SPACEDOG_ELASTIC_DATA_PATH = "spacedog.elastic.data.path";
	private static final String SPACEDOG_SERVER_PORT = "spacedog.server.port";
	private static final String SPACEDOG_API_URL_SCHEME = "spacedog.api.url.scheme";
	private static final String SPACEDOG_API_URL_BASE = "spacedog.api.url.base";

	private Properties configuration = new Properties();

	public StartConfiguration() {

		checkPath(SPACEDOG_CONFIGURATION_FILE, configFilePath(), false);

		try {
			configuration.load(Files.newInputStream(configFilePath()));
		} catch (IOException e) {
			throw Exceptions.runtime(e, //
					"error loading [%s] configuration file", configFilePath());
		}

		checkPath(SPACEDOG_HOME_PATH, homePath(), true);
		check(SPACEDOG_PRODUCTION, isProduction());
		check(SPACEDOG_OFFLINE, isOffline());
		check(SPACEDOG_SERVER_PORT, serverPort());

		check("API URL", apiUrl());

		checkPath(SPACEDOG_ELASTIC_DATA_PATH, elasticDataPath(), true);
		check(SPACEDOG_ELASTIC_HTTP_ENABLED, isElasticHttpEnabled());
		check(SPACEDOG_ELASTIC_NETWORK_HOST, elasticNetworkHost());
		checkPath(SPACEDOG_ELASTIC_SNAPSHOTS_PATH, snapshotsPath(), true);

		check(SPACEDOG_AWS_REGION, awsRegion());
		check(SPACEDOG_AWS_BUCKET_PREFIX, getAwsBucketPrefix());
		check(SPACEDOG_AWS_SUPERDOG_NOTIFICATION_TOPIC, superdogAwsNotificationTopic());

		check(SPACEDOG_MAIL_DOMAIN, mailDomain());
		check(SPACEDOG_MAIL_SMTP_DEBUG, mailSmtpDebug());
		check(SPACEDOG_MAIL_MAILGUN_KEY, mailGunKey());

		if (isProduction()) {
			// Force Fluent HTTP to production mode
			System.setProperty("PROD_MODE", "true");
		}

	}

	public Path homePath() {
		String path = configuration.getProperty(SPACEDOG_HOME_PATH);
		return path == null //
				? Paths.get(System.getProperty("user.home"), "spacedog")//
				: Paths.get(path);
	}

	public Path configFilePath() {
		String path = System.getProperty(SPACEDOG_CONFIGURATION_FILE);
		return path == null //
				? homePath().resolve("spacedog.server.properties")//
				: Paths.get(path);
	}

	public String apiUrl() {
		return apiUrl(Backends.ROOT_API);
	}

	public String apiUrl(String backendId) {
		if (Strings.isNullOrEmpty(backendId))
			backendId = Backends.ROOT_API;
		return new StringBuilder(apiUrlScheme()).append("://")//
				.append(backendId).append(apiUrlBase()).toString();
	}

	public String apiUrlBase() {
		return configuration.getProperty(SPACEDOG_API_URL_BASE);
	}

	public String apiUrlScheme() {
		return configuration.getProperty(SPACEDOG_API_URL_SCHEME);
	}

	public int serverPort() {
		return Integer.valueOf(configuration.getProperty(SPACEDOG_SERVER_PORT));
	}

	public Path elasticDataPath() {
		String path = configuration.getProperty(SPACEDOG_ELASTIC_DATA_PATH);
		return path == null ? //
				homePath().resolve("data") : Paths.get(path);
	}

	public boolean isProduction() {
		return Boolean.parseBoolean(//
				configuration.getProperty(SPACEDOG_PRODUCTION, "false"));
	}

	public String awsRegion() {
		return configuration.getProperty(SPACEDOG_AWS_REGION);
	}

	public String getAwsBucketPrefix() {
		return configuration.getProperty(SPACEDOG_AWS_BUCKET_PREFIX);
	}

	public String mailGunKey() {
		return configuration.getProperty(SPACEDOG_MAIL_MAILGUN_KEY);
	}

	public String mailDomain() {
		return configuration.getProperty(SPACEDOG_MAIL_DOMAIN);
	}

	public boolean mailSmtpDebug() {
		return Boolean.parseBoolean(//
				configuration.getProperty(SPACEDOG_MAIL_SMTP_DEBUG, "false"));
	}

	public Optional<Path> snapshotsPath() {
		String path = configuration.getProperty(SPACEDOG_ELASTIC_SNAPSHOTS_PATH);
		return Strings.isNullOrEmpty(path) ? Optional.empty()//
				: Optional.of(Paths.get(path));
	}

	public String superdogAwsNotificationTopic() {
		return configuration.getProperty(SPACEDOG_AWS_SUPERDOG_NOTIFICATION_TOPIC);
	}

	public boolean isOffline() {
		return Boolean.parseBoolean(//
				configuration.getProperty(SPACEDOG_OFFLINE, "false"));
	}

	public boolean isElasticHttpEnabled() {
		return Boolean.parseBoolean(//
				configuration.getProperty(SPACEDOG_ELASTIC_HTTP_ENABLED, "false"));
	}

	public String elasticNetworkHost() {
		String ip = configuration.getProperty(SPACEDOG_ELASTIC_NETWORK_HOST);
		if (!Strings.isNullOrEmpty(ip))
			return ip;

		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			throw Exceptions.illegalState(e, "no IP address for server");
		}
	}

	//
	// Implementation
	//

	private void check(String property, Object value) {

		if (value == null || value.toString().isEmpty())
			throw Exceptions.illegalArgument(//
					"Configuration setting [%s] is required", property);

		Utils.info("[SpaceDog] %s = %s", property, value);
	}

	private void checkPath(String property, Optional<Path> path, boolean directory) {
		if (path.isPresent())
			checkPath(property, path.get(), directory);
	}

	private void checkPath(String property, Path path, boolean directory) {

		if (path == null)
			throw Exceptions.illegalArgument(//
					"SpaceDog configuration [%s][%s] is required", property, path);

		if (directory && !Files.isDirectory(path))
			throw Exceptions.illegalArgument(//
					"SpaceDog configuration [%s][%s] is not a directory", property, path);

		if (!directory && !Files.isReadable(path))
			throw Exceptions.illegalArgument(//
					"SpaceDog configuration [%s][%s] is not a readable file", property, path);

		Utils.info("[SpaceDog] %s = %s", property, path);
	}

}
