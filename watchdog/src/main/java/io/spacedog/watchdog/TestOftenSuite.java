package io.spacedog.watchdog;

import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import io.spacedog.client.Space;
import io.spacedog.client.SpaceRequest;
import io.spacedog.watchdog.SpaceSuite.Annotations;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@RunWith(SpaceSuite.class)
@Annotations(TestOften.class)
public class TestOftenSuite extends RunListener {

	public void lambda() {

		JUnitCore junit = new JUnitCore();
		junit.addListener(this);
		junit.run(TestOftenSuite.class);

		Space.get().sendNotification(//
				SpaceRequest.getTarget().host() + " is up and running", //
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

		Space.get().sendNotification(SpaceRequest.getTarget().host()//
				+ " is DOWN DOWN DOWN", msg);

		System.exit(-1);
	}

	public static void main(String[] args) {
		new TestOftenSuite().lambda();
	}
}
