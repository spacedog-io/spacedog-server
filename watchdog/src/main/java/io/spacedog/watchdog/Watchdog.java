package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.google.common.base.Throwables;

import io.spacedog.client.SpaceRequestConfiguration;
import io.spacedog.utils.Internals;

public class Watchdog extends RunListener {

	public void run() {
		try {
			JUnitCore junit = new JUnitCore();
			junit.addListener(this);

			int hourOfDay = DateTime.now().hourOfDay().get();

			if (6 <= hourOfDay && hourOfDay < 7) {
				junit.run(TestAllSuite.class);

				Internals.get().notify(//
						SpaceRequestConfiguration.get().superdogNotificationTopic(), //
						SpaceRequestConfiguration.get().target().host() + " is up and running", //
						"Everything is under control.");
			} else
				junit.run(TestOftenSuite.class);

		} catch (Exception e) {
			e.printStackTrace();
			error(Throwables.getStackTraceAsString(e));
		}
	}

	public void run1() {
		try {
			JUnitCore junit = new JUnitCore();
			junit.addListener(this);

			junit.run(AccountResourceTestOncePerDay.class);

			Internals.get().notify(//
					SpaceRequestConfiguration.get().superdogNotificationTopic(), //
					SpaceRequestConfiguration.get().target().host() + " is up and running", //
					"Everything is under control.");

		} catch (Exception e) {
			e.printStackTrace();
			error(Throwables.getStackTraceAsString(e));
		}
	}

	public void runMinimal() {
		try {
			JUnitCore junit = new JUnitCore();
			junit.addListener(this);

			junit.run(DataResourceTest2.class);

			Internals.get().notify(//
					SpaceRequestConfiguration.get().superdogNotificationTopic(), //
					SpaceRequestConfiguration.get().target().host() + " is seems OK", //
					"Everything is under control.");

		} catch (Exception e) {
			e.printStackTrace();
			error(Throwables.getStackTraceAsString(e));
		}
	}

	@Override
	public void testFailure(Failure failure) {
		StringBuilder builder = new StringBuilder().append(failure.getMessage())//
				.append('\n').append(failure.getTrace());
		error(builder.toString());
	}

	public void error(String message) {
		System.err.println(message);

		Internals.get().notify(//
				SpaceRequestConfiguration.get().superdogNotificationTopic(), //
				SpaceRequestConfiguration.get().target().host() + " is DOWN DOWN DOWN", //
				message);

		System.exit(-1);
	}

	public static void main(String[] args) {
		new Watchdog().run();
	}
}
