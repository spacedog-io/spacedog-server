package io.spacedog.admin;

import com.amazonaws.services.lambda.runtime.Context;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceEnv;
import io.spacedog.jobs.Job;

public class Snapshot extends Job {

	public String run(Context context) {
		firstname(context.getFunctionName());
		return run();
	}

	public String run() {

		try {
			SpaceEnv env = SpaceEnv.defaultEnv();
			lastname(env.target().host());

			// set high timeout to wait for server
			// since snapshot service is slow
			env.httpTimeoutMillis(120000);

			SpaceDog snapshotDog = SpaceDog.backendId("api").username("snapshotall")//
					.password(env.getOrElseThrow("spacedog_jobs_snapshotall_password"));

			snapshotDog.post("/1/snapshot").go(202);

			snapshotDog.get("/1/snapshot").go(200)//
					// current snapshot is sometimes so fast
					// I don't get the IN_PROGESS status for results.0
					// results.1 should always be a SUCCESS
					.assertEquals("SUCCESS", "results.1.state");

		} catch (Throwable t) {
			return error(t);
		}

		return okOncePerDay();
	}

	public static void main(String[] args) {
		Snapshot snapshot = new Snapshot();
		snapshot.firstname("snapshot");
		snapshot.run();
	}
}
