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

			int hourOfDay = DateTime.now().hourOfDay().get();

			if (6 <= hourOfDay && hourOfDay < 7) {
				junit.run(TestAllSuite.class);

				AdminJobs.ok(this);
			} else
				junit.run(TestOftenSuite.class);

			return "OK";

		} catch (Exception e) {
			return AdminJobs.error(this, e);
		}
	}

	public String runMinimal() {
		try {
			JUnitCore junit = new JUnitCore();
			junit.addListener(this);
			junit.run(DataResourceTest2.class);
			return AdminJobs.ok(this);

		} catch (Exception e) {
			return AdminJobs.error(this, e);
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
