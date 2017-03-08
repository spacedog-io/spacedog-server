package io.spacedog.examples;

import org.junit.Test;

import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTarget;
import io.spacedog.rest.SpaceTest;
import io.spacedog.services.LogResource;
import io.spacedog.services.SnapshotResource;

public class AdminJobsCredentials extends SpaceTest {

	@Test
	public void initPurgeAllCredentials() {
		SpaceRequest.env().target(SpaceTarget.production);
		String password = SpaceEnv.defaultEnv().get("spacedog_jobs_purgeall_password");
		initCredentials(LogResource.PURGE_ALL, password, LogResource.PURGE_ALL);
	}

	@Test
	public void initSnapshotAllCredentials() {

		SpaceRequest.env().target(SpaceTarget.production);
		String password = SpaceEnv.defaultEnv().get("spacedog_jobs_snapshotall_password");
		initCredentials(SnapshotResource.SNAPSHOT_ALL, password, SnapshotResource.SNAPSHOT_ALL);
	}

	private void initCredentials(String username, String password, String role) {

		superdogDeletesCredentials("api", username);

		String id = SpaceRequest.post("/1/credentials").superdogAuth()//
				.body(FIELD_USERNAME, username, FIELD_PASSWORD, password, //
						FIELD_EMAIL, "platform@spacedog.io")
				.go(201).getString(FIELD_ID);

		SpaceRequest.put("/1/credentials/{id}/roles/{role}")//
				.routeParam(FIELD_ID, id).routeParam("role", role).superdogAuth().go(200);
	}

}
