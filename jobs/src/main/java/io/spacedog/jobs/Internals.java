package io.spacedog.jobs;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.google.common.base.Throwables;

import io.spacedog.client.http.SpaceEnv;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Utils;

public class Internals {

	private final AmazonSNS sns;

	public void notify(String title, Throwable t) {
		notify(title, Throwables.getStackTraceAsString(t));
	}

	public void notify(String title, String message) {

		try {
			Optional7<String> topic = SpaceEnv.env().superdogNotificationTopic();
			if (!topic.isPresent())
				Utils.warn("unable to send platform alert [%s][%s]: no SNS topic in env", //
						title, message);
			else
				sns.publish(new PublishRequest()//
						.withTopicArn(topic.get())//
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
