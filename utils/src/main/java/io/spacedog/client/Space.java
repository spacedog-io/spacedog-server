package io.spacedog.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.google.common.base.Strings;

public class Space {

	private AmazonSNSClient snsClient;
	private AmazonS3Client s3Client;
	private Configuration configuration;

	public void sendNotification(String title, String message) {
		snsClient.publish(new PublishRequest()//
				.withTopicArn("arn:aws:sns:eu-west-1:309725721660:watchdog")//
				.withSubject(title)//
				.withMessage(message));
	}

	public Configuration configuration() {
		try {
			if (configuration == null) {

				String userHome = System.getProperty("user.home");

				if (!Strings.isNullOrEmpty(userHome)) {
					Path path = Paths.get(userHome, ".superdog.properties");
					if (Files.exists(path))
						configuration = new Configuration(Files.newInputStream(path));
				}

				if (configuration == null)
					configuration = new Configuration(//
							get().s3Client.getObject("spacedog-artefact", "superdog.proerties")//
									.getObjectContent());
			}
			return configuration;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Configuration {

		private Properties properties;

		private Configuration(InputStream stream) throws IOException {
			properties = new Properties();
			properties.load(stream);
		}

		public String superdogName() {
			return getProperty("spacedog.superdog.username");
		}

		public String superdogPassword() {
			return getProperty("spacedog.superdog.password");
		}

		public boolean debug() {
			return Boolean.valueOf(getProperty("spacedog.debug", "false"));
		}

		public SpaceTarget target() {
			return SpaceTarget.valueOf(getProperty("spacedog.target", "local"));
		}

		private String getProperty(String key, String defaultValue) {
			String value = System.getProperty(key);
			if (Strings.isNullOrEmpty(value))
				value = properties.getProperty(key, defaultValue);
			return value;
		}

		private String getProperty(String key) {
			String value = System.getProperty(key);
			if (Strings.isNullOrEmpty(value))
				value = properties.getProperty(key);
			if (Strings.isNullOrEmpty(value))
				throw new IllegalStateException(String.format("configuration property [%s] not set", key));
			return value;
		}
	}

	//
	// Singleton
	//

	private static Space singleton = new Space();

	public static Space get() {
		return singleton;
	}

	private Space() {
		snsClient = new AmazonSNSClient();
		snsClient.setRegion(Region.getRegion(Regions.EU_WEST_1));
		s3Client = new AmazonS3Client();
		s3Client.setRegion(Region.getRegion(Regions.EU_WEST_1));
	}
}
