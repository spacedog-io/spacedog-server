package io.spacedog.watchdog;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import io.spacedog.client.SpaceRequest;
import io.spacedog.watchdog.SpaceSuite.Annotations;
import io.spacedog.watchdog.SpaceSuite.TestAlways;

@RunWith(SpaceSuite.class)
@Annotations(TestAlways.class)
public class TestOftenSuite extends RunListener {

	public String run() {
		JUnitCore junit = new JUnitCore();
		junit.addListener(this);

		SpaceRequest.setLogDebug(false);
		SpaceRequest.setTargetHostAndPorts("spacedog.io", 443, 80, true);

		Result result = junit.run(TestOftenSuite.class);
		return String.valueOf(result.wasSuccessful());
	}

	@Override
	public void testFailure(Failure failure) {

		StringBuilder logBuilder = new StringBuilder();

		if (failure.getMessage() != null)
			logBuilder.append(failure.getMessage());
		if (failure.getTrace() != null)
			logBuilder.append('\n').append(failure.getTrace());

		System.err.println(logBuilder.toString());

		System.exit(-1);
	}

	public static void main(String[] args) {
		System.out.println("Successful: " + new TestOftenSuite().run());
	}
}
