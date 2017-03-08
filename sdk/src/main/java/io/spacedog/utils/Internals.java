package io.spacedog.utils;

import java.util.Optional;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;

public class Internals {

	private AmazonSNSClient snsClient;

	public void notify(Optional<String> topicId, String title, String message) {

		try {
			if (topicId.isPresent())
				snsClient.publish(new PublishRequest()//
						.withTopicArn(topicId.get())//
						.withSubject(title)//
						.withMessage(message));
			else
				Utils.warn("Unable to send internal notification [%s][%s]: no SNS topic id.", //
						title, message);

		} catch (Throwable ignore) {
			ignore.printStackTrace();
		}
	}

	//
	// Singleton
	//

	private static Internals singleton = new Internals();

	public static Internals get() {
		return singleton;
	}

	private Internals() {
		snsClient = new AmazonSNSClient();
		snsClient.setRegion(Region.getRegion(Regions.EU_WEST_1));
	}
}
