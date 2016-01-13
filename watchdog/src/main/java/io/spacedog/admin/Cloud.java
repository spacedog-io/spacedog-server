package io.spacedog.admin;

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

import io.spacedog.client.SpaceTarget;

public class Cloud {

	private AmazonSNSClient snsClient;
	private AmazonS3Client s3Client;
	private SuperdogConfiguration configuration;

	public void sendNotification(String title, String message) {
		snsClient.publish(new PublishRequest()//
				.withTopicArn("arn:aws:sns:eu-west-1:309725721660:watchdog")//
				.withSubject(title)//
				.withMessage(message));
	}

	public SuperdogConfiguration getSuperdogConfiguration() {
		try {
			if (configuration == null) {

				String userHome = System.getProperty("user.home");

				if (!Strings.isNullOrEmpty(userHome)) {
					Path path = Paths.get(userHome, "superdog.properties");
					if (Files.exists(path))
						configuration = new SuperdogConfiguration(Files.newInputStream(path));
				} else
					configuration = new SuperdogConfiguration(//
							s3Client.getObject("spacedog-artefact", "superdog.properties")//
									.getObjectContent());
			}
			return configuration;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class SuperdogConfiguration {

		private Properties properties;

		private SuperdogConfiguration(InputStream stream) throws IOException {
			properties = new Properties();
			properties.load(stream);
		}

		public String username() {
			return properties.getProperty("superdog.username");
		}

		public String password() {
			return properties.getProperty("superdog.password");
		}

		public boolean debug() {
			return Boolean.valueOf(properties.getProperty("superdog.debug", "false"));
		}

		public SpaceTarget target() {
			return SpaceTarget.valueOf(properties.getProperty("superdog.target", "local"));
		}
	}

	//
	// Singleton
	//

	private static Cloud singleton = new Cloud();

	static Cloud get() {
		return singleton;
	}

	private Cloud() {
		snsClient = new AmazonSNSClient();
		snsClient.setRegion(Region.getRegion(Regions.EU_WEST_1));
		s3Client = new AmazonS3Client();
		s3Client.setRegion(Region.getRegion(Regions.EU_WEST_1));
	}
}
