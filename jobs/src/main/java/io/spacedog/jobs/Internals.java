package io.spacedog.jobs;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.google.common.base.Throwables;

import io.spacedog.utils.Utils;

public class Internals {

	private final AmazonSNS sns;

	public void notify(String topicId, String title, Throwable t) {
		notify(toString(), title, Throwables.getStackTraceAsString(t));
	}

	public void notify(String topicId, String title, String message) {

		try {
			if (topicId == null)
				Utils.warn("Unable to send internal notification [%s][%s]: no SNS topic id.", //
						title, message);
			else
				sns.publish(new PublishRequest()//
						.withTopicArn(topicId)//
						.withSubject(title)//
						.withMessage(message));

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
		sns = AmazonSNSClientBuilder.standard()//
				.withRegion(Regions.EU_WEST_1)//
				.build();
	}
}
