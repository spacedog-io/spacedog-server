package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import io.spacedog.client.Space;
import io.spacedog.client.SpaceRequest;

public class Watchdog extends RunListener {

	public void lambda() {

		JUnitCore junit = new JUnitCore();
		junit.addListener(this);

		int hourOfDay = DateTime.now().hourOfDay().get();

		if (6 <= hourOfDay && hourOfDay < 7) {
			junit.run(TestAllSuite.class);

			Space.get().sendNotification(//
					SpaceRequest.getTarget().host() + " is up and running", //
					"Everything is working properly.");
		} else
			junit.run(TestOftenSuite.class);

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
		new Watchdog().lambda();
	}
}
