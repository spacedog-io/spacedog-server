package io.spacedog.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.amazonaws.regions.Regions;

import io.spacedog.client.file.FileStoreType;
import io.spacedog.client.http.SpaceBackend;
import io.spacedog.client.http.SpaceEnv;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Utils;

public class ServerConfig {

	private static final String PORT = "spacedog.server.port";
	private static final String HOME_PATH = "spacedog.server.home.path";
	private static final String PRODUCTION = "spacedog.server.production";
	private static final String OFFLINE = "spacedog.server.offline";
	private static final String ELASTIC_SEARCH_HOST = "spacedog.server.elasticsearch.host";
	private static final String ELASTIC_SEARCH_SCHEME = "spacedog.server.elasticsearch.scheme";
	private static final String ELASTIC_SEARCH_PORT1 = "spacedog.server.elasticsearch.port1";
	private static final String ELASTIC_SEARCH_PORT2 = "spacedog.server.elasticsearch.port2";
	private static final String FILES_STORE_PATH = "spacedog.server.files.store.path";
	private static final String GREEN_CHECK = "spacedog.server.green.check";
	private static final String GREEN_TIMEOUT = "spacedog.server.green.timeout";
	private static final String USER_AGENT = "spacedog.server.user.agent";
	private static final String SNAPSHOTS_FILES_STORE_TYPE = "spacedog.server.snapshots.files.store.type";
	private static final String SNAPSHOTS_ELASTIC_STORE_TYPE = "spacedog.server.snapshots.elastic.store.type";
	private static final String SNAPSHOTS_S3_BUCKET = "spacedog.server.snapshots.s3.bucket";
	private static final String MAIL_SMTP_DEBUG = "spacedog.server.mail.smtp.debug";
	private static final String MAIL_DOMAIN = "spacedog.server.mail.domain";
	private static final String MAIL_MAILGUN_KEY = "spacedog.server.mail.mailgun.key";
	private static final String AWS_BUCKET_PREFIX = "spacedog.server.aws.bucket.prefix";

	public static Path homePath() {
		Optional<String> path = SpaceEnv.env().get(HOME_PATH);
		return path.isPresent() ? Paths.get(path.get()) //
				: Paths.get(System.getProperty("user.home"), "spacedog");
	}

	public static boolean isProduction() {
		return SpaceEnv.env().get(PRODUCTION, false);
	}

	public static boolean isOffline() {
		return SpaceEnv.env().get(OFFLINE, false);
	}

	public static String elasticSearchHost() {
		return SpaceEnv.env().get(ELASTIC_SEARCH_HOST, "localhost");
	}

	public static String elasticSearchScheme() {
		return SpaceEnv.env().get(ELASTIC_SEARCH_SCHEME, "http");
	}

	public static int elasticSearchPort1() {
		return SpaceEnv.env().get(ELASTIC_SEARCH_PORT1, 9200);
	}

	public static int elasticSearchPort2() {
		return SpaceEnv.env().get(ELASTIC_SEARCH_PORT2, 9201);
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

	public static Regions awsRegion() {
		return Regions.fromName(SpaceEnv.env().backendRegion());
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

	public static Path filesStorePath() {
		Optional<String> opt = SpaceEnv.env().get(FILES_STORE_PATH);
		return opt.isPresent() ? Paths.get(opt.get()) //
				: homePath().resolve("files");
	}

	public static FileStoreType snapshotsElasticStoreType() {
		return SpaceEnv.env().get(SNAPSHOTS_ELASTIC_STORE_TYPE, FileStoreType.fs);
	}

	public static FileStoreType snapshotsFilesStoreType() {
		return SpaceEnv.env().get(SNAPSHOTS_FILES_STORE_TYPE, FileStoreType.fs);
	}

	public static String snapshotsS3Bucket() {
		return SpaceEnv.env().getOrElseThrow(SNAPSHOTS_S3_BUCKET);
	}

	public static void log() {
		log("API URL", apiBackend());
		checkPath(HOME_PATH, homePath(), true);
		log(OFFLINE, isOffline());
		log(PRODUCTION, isProduction());
		log(PORT, port());
		log(GREEN_CHECK, greenCheck());
		log(GREEN_TIMEOUT, greenTimeout());
		log(SNAPSHOTS_ELASTIC_STORE_TYPE, snapshotsElasticStoreType());
		log(AWS_BUCKET_PREFIX, awsBucketPrefix());
		log(MAIL_DOMAIN, mailDomain());
		log(MAIL_SMTP_DEBUG, mailSmtpDebug());
		log(MAIL_MAILGUN_KEY, mailGunKey());
		Utils.info();
	}

	//
	// Implementation
	//

	private ServerConfig() {
	}

	private static void log(String property, Object value) {
		Utils.info("-- %s: %s", property, value);
	}

	@SuppressWarnings("unused")
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
