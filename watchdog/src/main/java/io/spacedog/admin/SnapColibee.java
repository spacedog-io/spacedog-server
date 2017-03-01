package io.spacedog.admin;

import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.utils.AdminJobs;

public class SnapColibee {

	public String run() {

		try {
			SpaceEnv env = SpaceEnv.defaultEnv();
			env.target(SpaceTarget.colibee);

			// set high timeout to wait for server
			// since snapshot service is slow
			env.httpTimeoutMillis(60000);

			String password = env.get("colibee_superdog_password");

			SpaceRequest.post("/1/snapshot")//
					.basicAuth("connectapi", "superdog-david", password)//
					.go(202);

			SpaceRequest.get("/1/snapshot")//
					.basicAuth("connectapi", "superdog-david", password)//
					.go(200)//
					.assertEquals("IN_PROGRESS", "results.0.state")//
					.assertEquals("SUCCESS", "results.1.state");

		} catch (Throwable t) {
			return AdminJobs.error(this, t);
		}

		return "OK";
	}

	public static void main(String[] args) throws InterruptedException {
		SnapColibee snapshot = new SnapColibee();
		snapshot.run();
	}
}
