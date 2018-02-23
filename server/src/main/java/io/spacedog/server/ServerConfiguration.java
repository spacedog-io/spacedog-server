package io.spacedog.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.amazonaws.regions.Regions;

import io.spacedog.http.SpaceBackend;
import io.spacedog.http.SpaceEnv;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Utils;

public class ServerConfiguration {

	private static final String HOME_PATH = "spacedog.home.path";
	private static final String PRODUCTION = "spacedog.production";
	private static final String OFFLINE = "spacedog.offline";
	private static final String BACKEND_CREATE_RESTRICTED = "spacedog.backend.create.restricted";
	private static final String SERVER_PORT = "spacedog.server.port";
	private static final String SERVER_GREEN_CHECK = "spacedog.server.green.check";
	private static final String SERVER_GREEN_TIMEOUT = "spacedog.server.green.timeout";
	private static final String SERVER_USER_AGENT = "spacedog.server.user.agent";
	private static final String ELASTIC_DATA_PATH = "spacedog.elastic.data.path";
	private static final String ELASTIC_HTTP_ENABLED = "spacedog.elastic.http.enabled";
	private static final String ELASTIC_NETWORK_HOST = "spacedog.elastic.network.host";
	private static final String ELASTIC_SNAPSHOTS_PATH = "spacedog.elastic.snapshots.path";
	private static final String MAIL_SMTP_DEBUG = "spacedog.mail.smtp.debug";
	private static final String MAIL_DOMAIN = "spacedog.mail.domain";
	private static final String MAIL_MAILGUN_KEY = "spacedog.mail.mailgun.key";
	private static final String AWS_SUPERDOG_NOTIFICATION_TOPIC = "spacedog.aws.superdog.notification.topic";
	private static final String AWS_BUCKET_PREFIX = "spacedog.aws.bucket.prefix";
	private static final String AWS_REGION = "spacedog.aws.region";

	private SpaceEnv env;

	public ServerConfiguration() {
		this(SpaceEnv.env());
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
		Optional7<String> path = env.get(HOME_PATH);
		return path.isPresent() ? Paths.get(path.get()) //
				: Paths.get(System.getProperty("user.home"), "spacedog");
	}

	public boolean isProduction() {
		return env.get(PRODUCTION, false);
	}

	public boolean isOffline() {
		return env.get(OFFLINE, false);
	}

	public SpaceBackend apiBackend() {
		return env.apiBackend();
	}

	public SpaceBackend wwwBackend() {
		return env.wwwBackend();
	}

	public int serverPort() {
		return env.get(SERVER_PORT, 8443);
	}

	public boolean serverGreenCheck() {
		return env.get(SERVER_GREEN_CHECK, true);
	}

	public int serverGreenTimeout() {
		return env.get(SERVER_GREEN_TIMEOUT, 60);
	}

	public Path elasticDataPath() {
		Optional7<String> path = env.get(ELASTIC_DATA_PATH);
		return path.isPresent() ? //
				Paths.get(path.get()) : homePath().resolve("data");
	}

	public Optional<Path> elasticSnapshotsPath() {
		Optional7<String> path = env.get(ELASTIC_SNAPSHOTS_PATH);
		return path.isPresent() ? Optional.of(Paths.get(path.get())) : Optional.empty();
	}

	public boolean elasticIsHttpEnabled() {
		return env.get(ELASTIC_HTTP_ENABLED, false);
	}

	public String elasticNetworkHost() {
		Optional7<String> ip = env.get(ELASTIC_NETWORK_HOST);
		if (ip.isPresent())
			return ip.get();

		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			throw Exceptions.illegalState(e, "no IP address found for this server");
		}
	}

	public Optional7<Regions> awsRegion() {
		Optional7<String> opt = env.get(AWS_REGION);
		return opt.isPresent() //
				? Optional7.of(Regions.fromName(opt.get())) //
				: Optional7.empty();

	}

	public Regions awsRegionOrDefault() {
		return awsRegion().orElse(Regions.EU_WEST_1);
	}

	public String awsBucketPrefix() {
		return env.getOrElseThrow(AWS_BUCKET_PREFIX);
	}

	public Optional7<String> awsSuperdogNotificationTopic() {
		return env.get(AWS_SUPERDOG_NOTIFICATION_TOPIC);
	}

	public String mailGunKey() {
		return env.getOrElseThrow(MAIL_MAILGUN_KEY);
	}

	public String mailDomain() {
		return env.getOrElseThrow(MAIL_DOMAIN);
	}

	public boolean mailSmtpDebug() {
		return env.get(MAIL_SMTP_DEBUG, false);
	}

	public boolean backendCreateRestricted() {
		return env.get(BACKEND_CREATE_RESTRICTED, true);
	}

	public String serverUserAgent() {
		return env.get(SERVER_USER_AGENT, "spacedog-server");
	}

	public String superdogPassword() {
		return env.superdogPassword();
	}

	public void log() {
		Utils.info("[SpaceDog] Server configuration =");

		log("API URL", apiBackend());
		checkPath(HOME_PATH, homePath(), true);
		log(OFFLINE, isOffline());
		log(PRODUCTION, isProduction());

		Utils.info();
		log(SERVER_PORT, serverPort());
		log(SERVER_GREEN_CHECK, serverGreenCheck());
		log(SERVER_GREEN_TIMEOUT, serverGreenTimeout());

		Utils.info();
		checkPath(ELASTIC_DATA_PATH, elasticDataPath(), true);
		log(ELASTIC_HTTP_ENABLED, elasticIsHttpEnabled());
		log(ELASTIC_NETWORK_HOST, elasticNetworkHost());
		checkPath(ELASTIC_SNAPSHOTS_PATH, elasticSnapshotsPath(), true);

		Utils.info();
		log(AWS_REGION, awsRegion());
		log(AWS_BUCKET_PREFIX, awsBucketPrefix());
		log(AWS_SUPERDOG_NOTIFICATION_TOPIC, awsSuperdogNotificationTopic());

		Utils.info();
		log(MAIL_DOMAIN, mailDomain());
		log(MAIL_SMTP_DEBUG, mailSmtpDebug());
		log(MAIL_MAILGUN_KEY, mailGunKey());
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
