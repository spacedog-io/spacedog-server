package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.amazonaws.services.lambda.runtime.Context;

import io.spacedog.client.SpaceEnv;
import io.spacedog.utils.Job;

public class Watchdog extends RunListener {

	private Job job = new Job();

	public String run(String input, Context context) {
		job.addToDescription(context);
		return run();
	}

	public String run() {

		try {
			job.addToDescription(SpaceEnv.defaultEnv().target().host());

			JUnitCore junit = new JUnitCore();
			junit.addListener(this);
			junit.run(TestOftenSuite.class);

			int hourOfDay = DateTime.now().hourOfDay().get();
			if (4 <= hourOfDay && hourOfDay < 5) {

				junit.run(TestOncePerDaySuite.class);
				job.ok();
			}

			return "OK";

		} catch (Throwable t) {
			return job.error(t);
		}
	}

	@Override
	public void testFailure(Failure failure) {
		job.error(new StringBuilder()//
				.append(failure.getMessage()).append('\n')//
				.append(failure.getTrace()).toString());
	}

	public static void main(String[] args) {
		new Watchdog().run();
	}
}
