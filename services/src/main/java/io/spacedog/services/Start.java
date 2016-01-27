/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfilesConfigFile;

import net.codestory.http.AbstractWebServer;
import net.codestory.http.Request;
import net.codestory.http.Response;
import net.codestory.http.internal.Handler;
import net.codestory.http.internal.HttpServerWrapper;
import net.codestory.http.internal.SimpleServerWrapper;
import net.codestory.http.payload.Payload;
import net.codestory.http.routes.Routes;
import net.codestory.http.websockets.WebSocketHandler;

public class Start {

	private Node elasticNode;
	private Client elasticClient;
	private MyFluentServer fluent;
	private Configuration config;

	public Node getElasticNode() {
		return elasticNode;
	}

	public Client getElasticClient() {
		return elasticClient;
	}

	public Configuration configuration() {
		return config;
	}

	public static void main(String[] args) {
		try {
			configAwsCredentials();
			singleton = new Start();
			singleton.startLocalElastic();
			singleton.startFluent();

		} catch (Throwable t) {
			t.printStackTrace();
			if (singleton != null) {
				if (singleton.fluent != null)
					singleton.elasticClient.close();
				if (singleton.elasticClient != null)
					singleton.elasticClient.close();
				if (singleton.elasticNode != null)
					singleton.elasticClient.close();
			}
			System.exit(-1);
		}
	}

	private static void configAwsCredentials() {
		// get AWS credentials from system user ~/.aws/credentials
		// set them in system properties for ElasticSearch to be able
		// to retreive them since ES does not use the ProfileCredentialProvider
		// class in its credentials provider chain
		AWSCredentials credentials = new ProfilesConfigFile().getCredentials("default");
		System.setProperty(SDKGlobalConfiguration.ACCESS_KEY_SYSTEM_PROPERTY, //
				credentials.getAWSAccessKeyId());
		System.setProperty(SDKGlobalConfiguration.SECRET_KEY_SYSTEM_PROPERTY, //
				credentials.getAWSSecretKey());
	}

	private void startLocalElastic() throws InterruptedException, ExecutionException, IOException {

		elasticNode = NodeBuilder.nodeBuilder()//
				.local(true)//
				.data(true)//
				.clusterName("spacedog-elastic-cluster")//
				.settings(ImmutableSettings.builder()//
						.put("path.data", //
								config.getElasticDataPath().toAbsolutePath().toString())
						.build())//
				.node();

		elasticClient = elasticNode.client();
		AccountResource.get().initSpacedogIndex();
	}

	private void startFluent() throws IOException {

		fluent = new MyFluentServer();
		fluent.configure(Start::configure);

		if (config.isSsl()) {
			fluent.startSSL(config.getMainPort(), Arrays.asList(config.getCrtFilePath(), //
					config.getPemFilePath()), config.getDerFilePath());
		} else
			fluent.start(config.getMainPort());

		HttpPermanentRedirect.start(config.getRedirectPort(), config.getUrl());
	}

	private static void configure(Routes routes) {
		routes.add(DataResource.get())//
				.add(SchemaResource.get())//
				.add(UserResource.get())//
				.add(AccountResource.get())//
				.add(BatchResource.get())//
				.add(MailResource.get())//
				.add(SnapshotResource.get())//
				.add(LogResource.get())//
				.add(PushResource.get())//
				.add(FileResource.get())//
				.add(ShareResource.get())//
				.add(SearchResource.get());

		routes.filter(new CrossOriginFilter())//
				.filter(SpaceContext.filter())//
				.filter(LogResource.filter())//
				.filter(new ServiceErrorFilter());
	}

	private static class MyFluentServer extends AbstractWebServer<MyFluentServer> {

		@Override
		protected HttpServerWrapper createHttpServer(Handler httpHandler, WebSocketHandler webSocketHandler) {
			return new SimpleServerWrapper(httpHandler, webSocketHandler);
		}

		public Payload executeRequest(Request request, Response response) throws Exception {
			return routesProvider.get().apply(request, response);
		}
	}

	public Payload executeRequest(Request request, Response response) throws Exception {
		return fluent.executeRequest(request, response);
	}

	//
	// Singleton
	//

	private static Start singleton;

	static Start get() {
		return singleton;
	}

	private Start() throws IOException {
		this.config = new Configuration();
	}

	//
	// Configuration
	//

	public static class Configuration {

		private Properties configuration = new Properties();

		public Configuration() throws IOException {

			Path confPath = getConfigurationFilePath();
			if (Files.isReadable(confPath)) {
				System.out.println("[SpaceDog] configuration file = " + confPath);
				configuration.load(Files.newInputStream(confPath));
			}

			if (!Files.isDirectory(getHomePath()))
				throw new RuntimeException(String.format("SpaceDog home path not a directory [%s]", //
						getHomePath()));

			if (!Files.isDirectory(getElasticDataPath()))
				throw new RuntimeException(String.format("SpaceDog data path not a directory [%s]", //
						getElasticDataPath()));

			if (!hasSuperDogs())
				throw new RuntimeException("No superdog administrator defined in configuration");

			System.out.println("[SpaceDog] url = " + getUrl());
			System.out.println("[SpaceDog] main port = " + getMainPort());
			System.out.println("[SpaceDog] redirect port = " + getRedirectPort());
			System.out.println("[SpaceDog] home path = " + getHomePath());
			System.out.println("[SpaceDog] data path = " + getElasticDataPath());
			System.out.println("[SpaceDog] mail domain = " + getMailDomain());
			System.out.println("[SpaceDog] mailgun key = " + getMailGunKey());
			System.out.println("[SpaceDog] snapshots path = " + getSnapshotsPath());
			System.out.println("[SpaceDog] snapshots bucket name = " + getSnapshotsBucketName());
			System.out.println("[SpaceDog] snapshots bucket region = " + getSnapshotsBucketRegion());
			System.out.println("[SpaceDog] david superdog password = " + getSuperDogHashedPassword("david"));
			System.out.println("[SpaceDog] david superdog email = " + getSuperDogEmail("david"));
			System.out.println("[SpaceDog] production = " + isProduction());

			if (isProduction()) {
				// Force Fluent HTTP to production mode
				System.setProperty("PROD_MODE", "true");
			}

			if (isSsl()) {
				System.out.println("[SpaceDog] .crt file = " + getCrtFilePath());
				System.out.println("[SpaceDog] .pem file = " + getPemFilePath());
				System.out.println("[SpaceDog] .der file = " + getDerFilePath());
			}
		}

		public Path getHomePath() {
			String path = configuration.getProperty("spacedog.home.path");
			return path == null //
					? Paths.get(System.getProperty("user.home"), "spacedog")//
					: Paths.get(path);
		}

		public Path getConfigurationFilePath() {
			String path = System.getProperty("spacedog.configuration.file");
			return path == null //
					? getHomePath().resolve("spacedog.server.properties")//
					: Paths.get(path);
		}

		public String getUrl() {
			return configuration.getProperty("spacedog.url", "https://spacedog.io");
		}

		public int getMainPort() {
			return Integer.valueOf(configuration.getProperty("spacedog.port.main", "4444"));
		}

		public int getRedirectPort() {
			return Integer.valueOf(configuration.getProperty("spacedog.port.redirect", "8888"));
		}

		public Path getElasticDataPath() {
			String path = configuration.getProperty("spacedog.data.path");
			return path == null ? //
					getHomePath().resolve("data") : Paths.get(path);
		}

		public Path getDerFilePath() {
			String path = configuration.getProperty("spacedog.ssl.der.file");
			return path == null ? //
					getHomePath().resolve("ssl/spacedog.io.pkcs8.der") : Paths.get(path);
		}

		public Path getPemFilePath() {
			String path = configuration.getProperty("spacedog.ssl.pem.file");
			return path == null ? //
					getHomePath().resolve("ssl/GandiStandardSSLCA.pem") : Paths.get(path);
		}

		public Path getCrtFilePath() {
			String path = configuration.getProperty("spacedog.ssl.crt.file");
			return path == null ? //
					getHomePath().resolve("ssl/spacedog.io.certificate.crt") : Paths.get(path);
		}

		public boolean isProduction() {
			return Boolean.valueOf(configuration.getProperty("spacedog.production", "false"));
		}

		public boolean isSsl() {
			return Files.isReadable(getCrtFilePath());
		}

		public Optional<String> getMailGunKey() {
			return Optional.ofNullable(configuration.getProperty("spacedog.mailgun.key"));
		}

		public Optional<String> getMailDomain() {
			return Optional.ofNullable(configuration.getProperty("spacedog.mail.domain"));
		}

		public Optional<Path> getSnapshotsPath() {
			String path = configuration.getProperty("spacedog.snapshots.path");
			return path == null ? Optional.empty() : Optional.of(Paths.get(path));
		}

		public Optional<String> getSnapshotsBucketName() {
			return Optional.ofNullable(configuration.getProperty("spacedog.snapshots.bucket.name"));
		}

		public Optional<String> getSnapshotsBucketRegion() {
			return Optional.ofNullable(configuration.getProperty("spacedog.snapshots.bucket.region"));
		}

		public Optional<String> getSuperDogHashedPassword(String username) {
			return Optional.ofNullable(configuration.getProperty("spacedog.superdog." + username + ".password"));
		}

		public boolean hasSuperDogs() {
			Enumeration<Object> keys = configuration.keys();
			while (keys.hasMoreElements())
				if (keys.nextElement().toString().startsWith("spacedog.superdog."))
					return true;
			return false;
		}

		public boolean isSuperDog(String username) {
			return getSuperDogHashedPassword(username).isPresent();
		}

		public Optional<String> getSuperDogEmail(String username) {
			return Optional.ofNullable(configuration.getProperty("spacedog.superdog." + username + ".email"));
		}

		public String superdogNotificationTopic() {
			return configuration.getProperty("spacedog.superdog.notification.topic");
		}
	}
}