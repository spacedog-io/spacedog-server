package io.spacedog.examples;

import org.junit.Test;

import io.spacedog.rest.SpaceBackend;
import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceTest;
import io.spacedog.server.LogResource;
import io.spacedog.server.SnapshotResource;

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

		superdog().credentials().deleteByUsername(username);
		String id = superdog().credentials()//
				.create(username, password, "platform@spacedog.io");
		superdog().credentials().setRole(id, role);
	}

}
