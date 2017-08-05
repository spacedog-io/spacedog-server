package io.spacedog.services;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceTarget;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Utils;

public class ServerConfiguration {

	private static final String SPACEDOG_HOME_PATH = "spacedog.home.path";
	private static final String SPACEDOG_PRODUCTION = "spacedog.production";
	private static final String SPACEDOG_OFFLINE = "spacedog.offline";
	private static final String SPACEDOG_BACKEND_API_PUBLIC_URL = "spacedog.backend.api.public.url";
	private static final String SPACEDOG_BACKEND_WWW_PUBLIC_URL = "spacedog.backend.www.public.url";
	private static final String SPACEDOG_ONLY_SUPERDOG_CAN_CREATE_BACKEND //
			= "spacedog.only.superdog.can.create.backend";
	private static final String SPACEDOG_SERVER_PORT = "spacedog.server.port";
	private static final String SPACEDOG_SERVER_USER_AGENT = "spacedog.server.user.agent";
	private static final String SPACEDOG_ELASTIC_DATA_PATH = "spacedog.elastic.data.path";
	private static final String SPACEDOG_ELASTIC_HTTP_ENABLED = "spacedog.elastic.http.enabled";
	private static final String SPACEDOG_ELASTIC_NETWORK_HOST = "spacedog.elastic.network.host";
	private static final String SPACEDOG_ELASTIC_INDEX_PREFIX = "spacedog.elastic.index.prefix";
	private static final String SPACEDOG_ELASTIC_SNAPSHOTS_PATH = "spacedog.elastic.snapshots.path";
	private static final String SPACEDOG_MAIL_SMTP_DEBUG = "spacedog.mail.smtp.debug";
	private static final String SPACEDOG_MAIL_DOMAIN = "spacedog.mail.domain";
	private static final String SPACEDOG_MAIL_MAILGUN_KEY = "spacedog.mail.mailgun.key";
	private static final String SPACEDOG_AWS_SUPERDOG_NOTIFICATION_TOPIC = "spacedog.aws.superdog.notification.topic";
	private static final String SPACEDOG_AWS_BUCKET_PREFIX = "spacedog.aws.bucket.prefix";
	private static final String SPACEDOG_AWS_REGION = "spacedog.aws.region";

	private SpaceEnv env;

	public ServerConfiguration() {
		this(SpaceEnv.defaultEnv());
	}

	public ServerConfiguration(SpaceEnv env) {
		this.env = env;
		this.log();

		if (isProduction()) {
			// Force Fluent HTTP to production mode
			System.setProperty("PROD_MODE", "true");
		}

	}

	public Path homePath() {
		Optional7<String> path = env.get(SPACEDOG_HOME_PATH);
		return path.isPresent() ? Paths.get(path.get()) //
				: Paths.get(System.getProperty("user.home"), "spacedog");
	}

	public boolean isProduction() {
		return env.get(SPACEDOG_PRODUCTION, false);
	}

	public boolean isOffline() {
		return env.get(SPACEDOG_OFFLINE, false);
	}

	private SpaceTarget apiBackend;

	public SpaceTarget apiBackend() {
		if (apiBackend == null)
			apiBackend = SpaceTarget.fromUrl(//
					env.getOrElseThrow(SPACEDOG_BACKEND_API_PUBLIC_URL));
		return apiBackend;
	}

	private Optional<SpaceTarget> wwwBackend;

	public Optional<SpaceTarget> wwwBackend() {
		if (wwwBackend == null) {
			Optional7<String> url = env.get(SPACEDOG_BACKEND_WWW_PUBLIC_URL);
			wwwBackend = url.isPresent() //
					? Optional.of(SpaceTarget.fromUrl(url.get(), true)) //
					: Optional.empty();
		}
		return wwwBackend;
	}

	public int serverPort() {
		return env.get(SPACEDOG_SERVER_PORT, 8443);
	}

	public Path elasticDataPath() {
		Optional7<String> path = env.get(SPACEDOG_ELASTIC_DATA_PATH);
		return path.isPresent() ? //
				Paths.get(path.get()) : homePath().resolve("data");
	}

	private String elasticIndexPrefix;

	public String elasticIndexPrefix() {
		if (elasticIndexPrefix == null)
			elasticIndexPrefix = env.get(SPACEDOG_ELASTIC_INDEX_PREFIX, "spacedog");
		return elasticIndexPrefix;
	}

	public Optional<Path> elasticSnapshotsPath() {
		Optional7<String> path = env.get(SPACEDOG_ELASTIC_SNAPSHOTS_PATH);
		return path.isPresent() ? Optional.of(Paths.get(path.get())) : Optional.empty();
	}

	public boolean elasticIsHttpEnabled() {
		return env.get(SPACEDOG_ELASTIC_HTTP_ENABLED, false);
	}

	public String elasticNetworkHost() {
		Optional7<String> ip = env.get(SPACEDOG_ELASTIC_NETWORK_HOST);
		if (ip.isPresent())
			return ip.get();

		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			throw Exceptions.illegalState(e, "no IP address found for this server");
		}
	}

	public String awsRegion() {
		return env.getOrElseThrow(SPACEDOG_AWS_REGION);
	}

	public String awsBucketPrefix() {
		return env.getOrElseThrow(SPACEDOG_AWS_BUCKET_PREFIX);
	}

	public Optional7<String> awsSuperdogNotificationTopic() {
		return env.get(SPACEDOG_AWS_SUPERDOG_NOTIFICATION_TOPIC);
	}

	public String mailGunKey() {
		return env.getOrElseThrow(SPACEDOG_MAIL_MAILGUN_KEY);
	}

	public String mailDomain() {
		return env.getOrElseThrow(SPACEDOG_MAIL_DOMAIN);
	}

	public boolean mailSmtpDebug() {
		return env.get(SPACEDOG_MAIL_SMTP_DEBUG, false);
	}

	public boolean onlySuperdogCanCreateBackend() {
		return env.get(SPACEDOG_ONLY_SUPERDOG_CAN_CREATE_BACKEND, true);
	}

	public String serverUserAgent() {
		return env.get(SPACEDOG_SERVER_USER_AGENT, "spacedog-server");
	}

	public void log() {
		Utils.info("[SpaceDog] Server configuration =");

		log("API URL", apiBackend());
		log(SPACEDOG_SERVER_PORT, serverPort());
		checkPath(SPACEDOG_HOME_PATH, homePath(), true);
		log(SPACEDOG_OFFLINE, isOffline());
		log(SPACEDOG_PRODUCTION, isProduction());

		Utils.info();
		checkPath(SPACEDOG_ELASTIC_DATA_PATH, elasticDataPath(), true);
		log(SPACEDOG_ELASTIC_HTTP_ENABLED, elasticIsHttpEnabled());
		log(SPACEDOG_ELASTIC_NETWORK_HOST, elasticNetworkHost());
		log(SPACEDOG_ELASTIC_INDEX_PREFIX, elasticIndexPrefix());
		checkPath(SPACEDOG_ELASTIC_SNAPSHOTS_PATH, elasticSnapshotsPath(), true);

		Utils.info();
		log(SPACEDOG_AWS_REGION, awsRegion());
		log(SPACEDOG_AWS_BUCKET_PREFIX, awsBucketPrefix());
		log(SPACEDOG_AWS_SUPERDOG_NOTIFICATION_TOPIC, awsSuperdogNotificationTopic());

		Utils.info();
		log(SPACEDOG_MAIL_DOMAIN, mailDomain());
		log(SPACEDOG_MAIL_SMTP_DEBUG, mailSmtpDebug());
		log(SPACEDOG_MAIL_MAILGUN_KEY, mailGunKey());
	}

	//
	// Implementation
	//

	private void log(String property, Object value) {
		Utils.info("-- %s: %s", property, value);
	}

	private void checkPath(String property, Optional<Path> path, boolean directory) {
		if (path.isPresent())
			checkPath(property, path.get(), directory);
	}

	private void checkPath(String property, Path path, boolean directory) {

		if (path == null)
			throw Exceptions.illegalArgument(//
					"SpaceDog server configuration [%s][%s] is required", property, path);

		if (directory && !Files.isDirectory(path))
			throw Exceptions.illegalArgument(//
					"SpaceDog server configuration [%s][%s] is not a directory", property, path);

		if (!directory && !Files.isReadable(path))
			throw Exceptions.illegalArgument(//
					"SpaceDog server configuration [%s][%s] is not a readable file", property, path);

		Utils.info("-- %s: %s", property, path);
	}

}
