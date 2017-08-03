package io.spacedog.admin;

import org.joda.time.DateTime;

import com.amazonaws.services.lambda.runtime.Context;

import io.spacedog.jobs.Job;
import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceRequest;

public class Purge extends Job {

	public String run(Context context) {
		firstname(context.getFunctionName());
		return run();
	}

	public String run() {

		try {
			SpaceEnv env = SpaceEnv.defaultEnv();
			lastname(env.target().host("api"));

			// set high timeout to wait for purge response from server
			// since delete of thousands of logs might take long
			env.httpTimeoutMillis(120000);
			String password = env.getOrElseThrow("spacedog_jobs_purgeall_password");
			int keepInDays = env.get("keep_in_days", 7);
			String before = DateTime.now().minusDays(keepInDays).toString();

			SpaceRequest.delete("/1/log").backend("api")//
					.basicAuth("purgeall", password)//
					.queryParam("before", before).go(200);

			return ok();

		} catch (Throwable t) {
			return error(t);
		}
	}

	public static void main(String[] args) {
		Purge purge = new Purge();
		purge.firstname("purgelogs");
		purge.run();
	}
}
