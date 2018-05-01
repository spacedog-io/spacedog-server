package io.spacedog.watchdog;

import org.joda.time.DateTime;

import com.amazonaws.services.lambda.runtime.Context;

import io.spacedog.jobs.Job;
import io.spacedog.rest.SpaceEnv;

public class Watchdog {

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
			job.lastname(//
					SpaceEnv.defaultEnv().target()//
							.host(WatchdogTest.TEST_V0_BACKEND_ID));

			WatchdogTest test = new WatchdogTest();

			test.shouldCreatAndDeleteCredentials();
			test.shouldCreateReadSearchAndDeleteDataMessages();
			test.shouldShareFiles();

			int hourOfDay = DateTime.now().getHourOfDay();
			if (4 <= hourOfDay && hourOfDay < 5)
				job.ok();

			return Job.OK;

		} catch (Throwable t) {
			return job.error(t);
		}
	}

	public static void main(String[] args) {
		Watchdog watchdog = new Watchdog();
		watchdog.name("watchdog");
		watchdog.run();
	}
}
