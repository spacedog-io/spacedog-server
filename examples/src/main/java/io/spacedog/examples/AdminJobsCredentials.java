package io.spacedog.examples;

import org.junit.Test;

import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceBackend;
import io.spacedog.rest.SpaceTest;
import io.spacedog.services.LogResource;
import io.spacedog.services.SnapshotResource;

public class AdminJobsCredentials extends SpaceTest {

	@Test
	public void initPurgeAllCredentials() {
		SpaceEnv.defaultEnv().target(SpaceBackend.production);
		String password = SpaceEnv.defaultEnv().getOrElseThrow("spacedog_jobs_purgeall_password");
		initCredentials(LogResource.PURGE_ALL, password, LogResource.PURGE_ALL);
	}

	@Test
	public void initSnapshotAllCredentials() {

		SpaceEnv.defaultEnv().target(SpaceBackend.production);
		String password = SpaceEnv.defaultEnv().getOrElseThrow("spacedog_jobs_snapshotall_password");
		initCredentials(SnapshotResource.SNAPSHOT_ALL, password, SnapshotResource.SNAPSHOT_ALL);
	}

	private void initCredentials(String username, String password, String role) {

		superdogDeletesCredentials("api", username);

		String id = superdog().post("/1/credentials")//
				.bodyJson(USERNAME_FIELD, username, PASSWORD_FIELD, password, //
						EMAIL_FIELD, "platform@spacedog.io")
				.go(201).getString(ID_FIELD);

		superdog().put("/1/credentials/{id}/roles/{role}")//
				.routeParam(ID_FIELD, id).routeParam("role", role).go(200);
	}

}
