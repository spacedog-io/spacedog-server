package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import io.spacedog.admin.AdminJobs;

public class Watchdog extends RunListener {

	public String run() {
		try {
			JUnitCore junit = new JUnitCore();
			junit.addListener(this);
			junit.run(TestOftenSuite.class);

			int hourOfDay = DateTime.now().hourOfDay().get();
			if (6 <= hourOfDay && hourOfDay < 7) {

				junit.run(TestOncePerDaySuite.class);
				AdminJobs.ok(this);
			}

			return "OK";

		} catch (Throwable t) {
			return AdminJobs.error(this, t);
		}
	}

	@Override
	public void testFailure(Failure failure) {
		AdminJobs.error(this,
				new StringBuilder()//
						.append(failure.getMessage()).append('\n')//
						.append(failure.getTrace()).toString());
	}

	public static void main(String[] args) {
		new Watchdog().run();
	}
}
