package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.amazonaws.services.lambda.runtime.Context;

import io.spacedog.jobs.Job;
import io.spacedog.rest.SpaceEnv;

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
			job.lastname(//
					SpaceEnv.defaultEnv().target().host("test"));

			JUnitCore junit = new JUnitCore();
			junit.addListener(this);

			junit.run(DataResourceTestOften.class);

			int hourOfDay = DateTime.now().getHourOfDay();
			if (4 <= hourOfDay && hourOfDay < 5) {

				junit.run(CredentialsResourceTestOften.class);
				junit.run(CrossOriginFilterTestOften.class);
				junit.run(DataAccessControlTestOften.class);
				junit.run(DataResource2TestOften.class);
				junit.run(LogResourceTestOften.class);
				junit.run(PushResourceTestOften.class);
				junit.run(SchemaResourceTestOften.class);
				junit.run(SearchResourceTestOften.class);
				junit.run(ServiceErrorFilterTestOften.class);
				junit.run(ServiceErrorFilterTestOncePerDay.class);
				junit.run(FileResourceTestOncePerDay.class);
				junit.run(ShareResourceTestOncePerDay.class);
				junit.run(WebResourceTestOncePerDay.class);

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
