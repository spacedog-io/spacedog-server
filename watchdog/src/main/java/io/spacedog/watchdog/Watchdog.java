package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.amazonaws.services.lambda.runtime.Context;

import io.spacedog.http.SpaceEnv;
import io.spacedog.jobs.Job;

public class Watchdog extends RunListener {

	private Job job = new Job();

	public void name(String name) {
		job.firstname(name);
	}

	public String run(Context context) {
		job.firstname(context.getFunctionName());
		return run();
	}

	public String run() {

		try {
			job.lastname(SpaceEnv.defaultEnv().target()//
					.instanciate("test").host());

			JUnitCore junit = new JUnitCore();
			junit.addListener(this);

			junit.run(WatchdogTest.class);

			int hourOfDay = DateTime.now().getHourOfDay();
			if (4 <= hourOfDay && hourOfDay < 5) {

				junit.run(WatchdogTest.class);

				job.ok();
			}

			return Job.OK;

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
		Watchdog watchdog = new Watchdog();
		watchdog.name("watchdog");
		watchdog.run();
	}
}
