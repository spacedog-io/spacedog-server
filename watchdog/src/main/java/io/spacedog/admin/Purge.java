package io.spacedog.admin;

import org.joda.time.DateTime;

import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Job;

public class Purge extends Job {

	public String run() {

		try {
			// set high timeout to wait for purge response from server
			// since delete of thousands of logs might take long
			SpaceRequest.env().httpTimeoutMillis(120000);
			String password = SpaceEnv.defaultEnv().get("spacedog_jobs_purgeall_password");
			int keepInDays = SpaceEnv.defaultEnv().get("keep_in_days", 7);
			String before = DateTime.now().minusDays(keepInDays).toString();

			SpaceRequest.delete("/1/log")//
					.queryParam("before", before)//
					.basicAuth("api", "purgeall", password)//
					.go(200);

			return ok();

		} catch (Throwable t) {
			return error(t);
		}
	}

	public static void main(String[] args) {
		new Purge().run();
	}
}
