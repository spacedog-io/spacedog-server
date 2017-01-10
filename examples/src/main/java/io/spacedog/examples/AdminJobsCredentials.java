package io.spacedog.examples;

import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;

public class AdminJobsCredentials extends SpaceClient {

	@Test
	public void initPurgeAllCredentials() {

		// SpaceRequest.env().target(SpaceTarget.production);

		String password = SpaceEnv.defaultEnv().get("spacedog_jobs_purgeall_password");

		String id = SpaceRequest.post("/1/credentials").superdogAuth()//
				.body("username", "purgeall", "password", password, //
						"email", "platform@spacedog.io")
				.go(201).getString("id");

		SpaceRequest.put("/1/credentials/" + id + "/roles/purgeall").superdogAuth().go(200);
	}

	@Test
	public void initSnapshotAllCredentials() {

		// SpaceRequest.env().target(SpaceTarget.production);

		String password = SpaceEnv.defaultEnv().get("spacedog_jobs_snapshotall_password");

		String id = SpaceRequest.post("/1/credentials").superdogAuth()//
				.body("username", "snapshotall", "password", password, //
						"email", "platform@spacedog.io")
				.go(201).getString("id");

		SpaceRequest.put("/1/credentials/" + id + "/roles/snapshoteall").superdogAuth().go(200);
	}

}
