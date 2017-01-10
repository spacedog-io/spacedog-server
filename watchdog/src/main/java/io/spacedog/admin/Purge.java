package io.spacedog.admin;

import org.joda.time.DateTime;

import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;

public class Purge {

	public String run() {

		try {
			// set high timeout to wait for purge response from server
			// since delete of thousands of logs might take long
			SpaceRequest.env().httpTimeoutMillis(120000);
			String password = SpaceEnv.defaultEnv().get("spacedog_jobs_purgeall_password");
			String before = DateTime.now().minusDays(7).toString();

			SpaceRequest.delete("/1/log")//
					.queryParam("before", before)//
					.basicAuth("api", "purgeall", password)//
					.go(200);

			return AdminJobs.ok(this);

		} catch (Throwable t) {
			return AdminJobs.error(this, t);
		}
	}

	public static void main(String[] args) {
		new Purge().run();
	}
}
