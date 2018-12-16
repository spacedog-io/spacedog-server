package io.spacedog.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.amazonaws.regions.Regions;

import io.spacedog.client.file.FileBucket;
import io.spacedog.client.http.SpaceBackend;
import io.spacedog.client.http.SpaceEnv;
import io.spacedog.client.snapshot.SpaceRepository;
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
	private static final String FILE_SNAPSHOTS_STORE_TYPE = "spacedog.server.file.snapshots.store.type";
	private static final String FILE_SNAPSHOTS_SYSTEM_PATH = "spacedog.server.file.snapshots.system.path";
	private static final String FILE_SNAPSHOTS_S3_SUFFIX = "spacedog.server.file.snapshots.s3.suffix";
	private static final String GREEN_CHECK = "spacedog.server.green.check";
	private static final String GREEN_TIMEOUT = "spacedog.server.green.timeout";
	private static final String USER_AGENT = "spacedog.server.user.agent";
	private static final String ELASTIC_CONFIG_PATH = "spacedog.server.elastic.config.path";
	private static final String SNAPSHOTS_REPO_TYPE = "spacedog.server.snapshots.repo.type";
	private static final String MAIL_SMTP_DEBUG = "spacedog.server.mail.smtp.debug";
	private static final String MAIL_DOMAIN = "spacedog.server.mail.domain";
	private static final String MAIL_MAILGUN_KEY = "spacedog.server.mail.mailgun.key";
	private static final String AWS_BUCKET_PREFIX = "spacedog.server.aws.bucket.prefix";

	public static Path homePath() {
		Optional7<String> path = SpaceEnv.env().get(HOME_PATH);
		return path.isPresent() ? Paths.get(path.get()) //
				: Paths.get(System.getProperty("user.home"), "spacedog");
	}

	public static Path elasticConfigPath() {
		Optional7<String> path = SpaceEnv.env().get(ELASTIC_CONFIG_PATH);
		return path.isPresent() ? Paths.get(path.get()) : null;
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

	public static String snapshotsRepoType() {
		return SpaceEnv.env().get(SNAPSHOTS_REPO_TYPE, SpaceRepository.TYPE_FS);
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

	public static String fileStore() {
		return SpaceEnv.env().get(FILE_STORE, "system");
	}

	public static Path fileStorePath() {
		Optional7<String> opt = SpaceEnv.env().get(FILE_STORE_PATH);
		return opt.isPresent() ? Paths.get(opt.get()) //
				: homePath().resolve("files");
	}

	public static FileBucket.StoreType fileSnapshotsStoreType() {
		return SpaceEnv.env().get(FILE_SNAPSHOTS_STORE_TYPE, FileBucket.StoreType.system);
	}

	public static Path fileSnapshotsSystemPath() {
		Optional7<String> opt = SpaceEnv.env().get(FILE_SNAPSHOTS_SYSTEM_PATH);
		return opt.isPresent() ? Paths.get(opt.get()) //
				: homePath().resolve("snapshots-files");
	}

	public static String fileSnapshotsS3Suffix() {
		return SpaceEnv.env().get(FILE_SNAPSHOTS_S3_SUFFIX, "snapshots-files");
	}

	public static void log() {
		log("API URL", apiBackend());
		checkPath(HOME_PATH, homePath(), true);
		log(OFFLINE, isOffline());
		log(PRODUCTION, isProduction());
		log(PORT, port());
		log(GREEN_CHECK, greenCheck());
		log(GREEN_TIMEOUT, greenTimeout());
		log(SNAPSHOTS_REPO_TYPE, snapshotsRepoType());
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
