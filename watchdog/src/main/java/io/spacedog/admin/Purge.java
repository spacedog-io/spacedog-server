package io.spacedog.admin;

import io.spacedog.client.SpaceRequest;

public class Purge {

	public String run() {

		try {
			// set high timeout to wait for purge response from server
			// since delete of thousands of logs might take long
			SpaceRequest.configuration().httpTimeoutMillis(120000);

			SpaceRequest.delete("/1/log?from=100000")//
					.superdogAuth()//
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
