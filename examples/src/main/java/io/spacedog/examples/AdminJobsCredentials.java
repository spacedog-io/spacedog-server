package io.spacedog.examples;

import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceBackend;
import io.spacedog.http.SpaceEnv;
import io.spacedog.http.SpaceTest;
import io.spacedog.server.LogService;
import io.spacedog.server.SnapshotService;

public class AdminJobsCredentials extends SpaceTest {

	@Test
	public void initPurgeAllCredentials() {

		SpaceEnv.defaultEnv().target(SpaceBackend.production);
		String password = SpaceEnv.defaultEnv().getOrElseThrow("spacedog_jobs_purgeall_password");
		initCredentials(LogService.PURGE_ALL, password, LogService.PURGE_ALL);
	}

	@Test
	public void initSnapshotAllCredentials() {

		SpaceEnv.defaultEnv().target(SpaceBackend.production);
		String password = SpaceEnv.defaultEnv().getOrElseThrow("spacedog_jobs_snapshotall_password");
		initCredentials(SnapshotService.SNAPSHOT_ALL, password, SnapshotService.SNAPSHOT_ALL);
	}

	private void initCredentials(String username, String password, String role) {

		superdog().credentials().deleteByUsername(username);
		SpaceDog newDog = superdog().credentials()//
				.create(username, password, "platform@spacedog.io");
		superdog().credentials().setRole(newDog.id(), role);
	}

}
