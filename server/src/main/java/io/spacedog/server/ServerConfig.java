package io.spacedog.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.amazonaws.regions.Regions;

import io.spacedog.client.http.SpaceBackend;
import io.spacedog.client.http.SpaceEnv;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Utils;

public class ServerConfig {

	private static final String PORT = "spacedog.server.port";
	private static final String HOME_PATH = "spacedog.server.home.path";
	private static final String PRODUCTION = "spacedog.server.production";
	private static final String OFFLINE = "spacedog.server.offline";
	private static final String FILE_STORE = "spacedog.server.file.store";
	private static final String FILE_STORE_PATH = "spacedog.server.file.store.path";
	private static final String GREEN_CHECK = "spacedog.server.green.check";
	private static final String GREEN_TIMEOUT = "spacedog.server.green.timeout";
	private static final String USER_AGENT = "spacedog.server.user.agent";
	private static final String ELASTIC_CONFIG_PATH = "spacedog.server.elastic.config.path";
	private static final String ELASTIC_NETWORK_HOST = "spacedog.server.elastic.network.host";
	private static final String ELASTIC_SNAPSHOTS_PATH = "spacedog.server.elastic.snapshots.path";
	private static final String MAIL_SMTP_DEBUG = "spacedog.server.mail.smtp.debug";
	private static final String MAIL_DOMAIN = "spacedog.server.mail.domain";
	private static final String MAIL_MAILGUN_KEY = "spacedog.server.mail.mailgun.key";
	private static final String AWS_BUCKET_PREFIX = "spacedog.server.aws.bucket.prefix";
	private static final String AWS_REGION = "spacedog.server.aws.region";

	public static Path homePath() {
		Optional7<String> path = SpaceEnv.env().get(HOME_PATH);
		return path.isPresent() ? Paths.get(path.get()) //
				: Paths.get(System.getProperty("user.home"), "spacedog");
	}

	public static boolean isProduction() {
		return SpaceEnv.env().get(PRODUCTION, false);
	}

	public static boolean isOffline() {
		return SpaceEnv.env().get(OFFLINE, false);
	}

	public static SpaceBackend apiBackend() {
		return SpaceEnv.env().apiBackend();
	}

	public static SpaceBackend wwwBackend() {
		return SpaceEnv.env().wwwBackend();
	}

	public static int port() {
		return SpaceEnv.env().get(PORT, 8443);
	}

	public static boolean greenCheck() {
		return SpaceEnv.env().get(GREEN_CHECK, true);
	}

	public static int greenTimeout() {
		return SpaceEnv.env().get(GREEN_TIMEOUT, 60);
	}

	public static Path elasticConfigPath() {
		Optional7<String> path = SpaceEnv.env().get(ELASTIC_CONFIG_PATH);
		return path.isPresent() ? Paths.get(path.get()) : null;
	}

	public static Optional<Path> elasticSnapshotsPath() {
		Optional7<String> path = SpaceEnv.env().get(ELASTIC_SNAPSHOTS_PATH);
		return path.isPresent() ? Optional.of(Paths.get(path.get())) : Optional.empty();
	}

	public static String elasticNetworkHost() {
		Optional7<String> ip = SpaceEnv.env().get(ELASTIC_NETWORK_HOST);
		if (ip.isPresent())
			return ip.get();

		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			throw Exceptions.illegalState(e, "no IP address found for this server");
		}
	}

	public static Optional7<Regions> awsRegion() {
		Optional7<String> opt = SpaceEnv.env().get(AWS_REGION);
		return opt.isPresent() //
				? Optional7.of(Regions.fromName(opt.get())) //
				: Optional7.empty();

	}

	public static Regions awsRegionOrDefault() {
		return awsRegion().orElse(Regions.EU_WEST_1);
	}

	public static String awsBucketPrefix() {
		return SpaceEnv.env().getOrElseThrow(AWS_BUCKET_PREFIX);
	}

	public static String mailGunKey() {
		return SpaceEnv.env().getOrElseThrow(MAIL_MAILGUN_KEY);
	}

	public static String mailDomain() {
		return SpaceEnv.env().getOrElseThrow(MAIL_DOMAIN);
	}

	public static boolean mailSmtpDebug() {
		return SpaceEnv.env().get(MAIL_SMTP_DEBUG, false);
	}

	public static String userAgent() {
		return SpaceEnv.env().get(USER_AGENT, "spacedog");
	}

	public static String superdogPassword() {
		return SpaceEnv.env().superdogPassword();
	}

	public static Optional7<String> fileStore() {
		return SpaceEnv.env().get(FILE_STORE);
	}

	public static Path fileStorePath() {
		return Paths.get(SpaceEnv.env().getOrElseThrow(FILE_STORE_PATH));
	}

	public static void log() {
		Utils.info("[SpaceDog] Server configuration =");

		log("API URL", apiBackend());
		checkPath(HOME_PATH, homePath(), true);
		log(OFFLINE, isOffline());
		log(PRODUCTION, isProduction());

		Utils.info();
		log(PORT, port());
		log(GREEN_CHECK, greenCheck());
		log(GREEN_TIMEOUT, greenTimeout());

		Utils.info();
		log(ELASTIC_NETWORK_HOST, elasticNetworkHost());
		checkPath(ELASTIC_SNAPSHOTS_PATH, elasticSnapshotsPath(), true);

		Utils.info();
		log(AWS_REGION, awsRegion());
		log(AWS_BUCKET_PREFIX, awsBucketPrefix());

		Utils.info();
		log(MAIL_DOMAIN, mailDomain());
		log(MAIL_SMTP_DEBUG, mailSmtpDebug());
		log(MAIL_MAILGUN_KEY, mailGunKey());
	}

	//
	// Implementation
	//

	private ServerConfig() {
	}

	private static void log(String property, Object value) {
		Utils.info("-- %s: %s", property, value);
	}

	private static void checkPath(String property, Optional<Path> path, boolean directory) {
		if (path.isPresent())
			checkPath(property, path.get(), directory);
	}

	private static void checkPath(String property, Path path, boolean directory) {

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
