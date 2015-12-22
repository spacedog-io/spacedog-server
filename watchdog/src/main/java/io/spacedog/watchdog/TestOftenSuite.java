package io.spacedog.watchdog;

import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;

import io.spacedog.client.SpaceRequest;
import io.spacedog.watchdog.SpaceSuite.Annotations;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@RunWith(SpaceSuite.class)
@Annotations(TestOften.class)
public class TestOftenSuite extends RunListener {

	public void lambda() {
		JUnitCore junit = new JUnitCore();
		junit.addListener(this);

		SpaceRequest.setLogDebug(false);
		SpaceRequest.setTargetHostAndPorts("spacedog.io", 443, 80, true);

		junit.run(TestOftenSuite.class);
		sendNotification(SpaceRequest.getTargetHost() + " is up and running", //
				"Everything is working properly.");
	}

	@Override
	public void testFailure(Failure failure) {

		StringBuilder logBuilder = new StringBuilder();

		if (failure.getMessage() != null)
			logBuilder.append(failure.getMessage());
		if (failure.getTrace() != null)
			logBuilder.append('\n').append(failure.getTrace());

		String msg = logBuilder.toString();
		System.err.println(msg);

		sendNotification(SpaceRequest.getTargetHost()//
				+ " is DOWN DOWN DOWN", msg);

		System.exit(-1);
	}

	private static void sendNotification(String subject, String msg) {
		AmazonSNSClient snsClient = new AmazonSNSClient();
		snsClient.setRegion(Region.getRegion(Regions.EU_WEST_1));
		snsClient.publish(new PublishRequest()//
				.withTopicArn("arn:aws:sns:eu-west-1:309725721660:watchdog")//
				.withSubject(subject)//
				.withMessage(msg));
	}

	public static void main(String[] args) {
		new TestOftenSuite().lambda();
	}
}
