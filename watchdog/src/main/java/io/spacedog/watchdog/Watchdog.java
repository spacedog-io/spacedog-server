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

	public void addToDescription(String string) {
		job.addToDescription(string);
	}

	public String run(Context context) {
		job.addToDescription(context.getFunctionName());
		return run();
	}

	public String run() {

		try {
			job.addToDescription(SpaceEnv.defaultEnv().target().host());

			JUnitCore junit = new JUnitCore();
			junit.addListener(this);

			junit.run(BackendResourceTestOften.class);
			junit.run(CredentialsResourceTestOften.class);
			junit.run(CrossOriginFilterTestOften.class);
			junit.run(DataAccessControlTestOften.class);
			junit.run(DataResourceTestOften.class);
			junit.run(DataResource2TestOften.class);
			junit.run(LogResourceTestOften.class);
			junit.run(PushResourceTestOften.class);
			junit.run(SchemaResourceTestOften.class);
			junit.run(SearchResourceTestOften.class);
			junit.run(ServiceErrorFilterTestOften.class);

			int hourOfDay = DateTime.now().hourOfDay().get();
			if (4 <= hourOfDay && hourOfDay < 5) {

				junit.run(BackendResourceTestOncePerDay.class);
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
		watchdog.addToDescription("watchdog");
		watchdog.run();
	}
}
