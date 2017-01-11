package io.spacedog.examples;

import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;
import io.spacedog.services.LogResource;
import io.spacedog.services.SnapshotResource;

public class AdminJobsCredentials extends SpaceClient {

	@Test
	public void initPurgeAllCredentials() {

		// SpaceRequest.env().target(SpaceTarget.production);

		String password = SpaceEnv.defaultEnv().get("spacedog_jobs_purgeall_password");

		SpaceClient.deleteCredentialsBySuperdog("api", LogResource.PURGE_ALL);

		String id = SpaceRequest.post("/1/credentials").superdogAuth()//
				.body(USERNAME, LogResource.PURGE_ALL, PASSWORD, password, //
						EMAIL, "platform@spacedog.io")
				.go(201).getString(ID);

		SpaceRequest.put("/1/credentials/{id}/roles/" + LogResource.PURGE_ALL)//
				.routeParam(ID, id).superdogAuth().go(200);
	}

	@Test
	public void initSnapshotAllCredentials() {

		// SpaceRequest.env().target(SpaceTarget.production);

		String password = SpaceEnv.defaultEnv().get("spacedog_jobs_snapshotall_password");

		SpaceClient.deleteCredentialsBySuperdog("api", SnapshotResource.SNAPSHOT_ALL);

		String id = SpaceRequest.post("/1/credentials").superdogAuth()//
				.body(USERNAME, SnapshotResource.SNAPSHOT_ALL, //
						PASSWORD, password, EMAIL, "platform@spacedog.io")
				.go(201).getString(ID);

		SpaceRequest.put("/1/credentials/{id}/roles/" + SnapshotResource.SNAPSHOT_ALL)//
				.routeParam(ID, id).superdogAuth().go(200);
	}

}
