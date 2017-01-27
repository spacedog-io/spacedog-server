package io.spacedog.services;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.client.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class SnapshotResourceTest extends SpaceTest {

	@Test
	public void snapshotAndRestoreMultipleTimes() throws InterruptedException, UnknownHostException {

		// prepare
		prepareTest();
		SpaceDog aaaa = SpaceDog.backend("aaaa").username("aaaa").password("hi aaaa");
		SpaceDog bbbb = SpaceDog.backend("bbbb").username("bbbb").password("hi bbbb");
		SpaceDog cccc = SpaceDog.backend("cccc").username("cccc").password("hi cccc");

		aaaa.backend().delete();
		bbbb.backend().delete();
		cccc.backend().delete();

		// creates backend and credentials
		aaaa.signUpBackend();
		SpaceDog vince = signUp(aaaa, "vince", "hi vince");
		SpaceRequest.get("/1/login").userAuth(vince).go(200);

		// deletes the current repository to force repo creation by this test
		// use full url to avoid delete by mistake any prod repo
		String repository = DateTime.now().withZone(DateTimeZone.UTC).toString("yyyy-ww");
		String ip = InetAddress.getLocalHost().getHostAddress();
		SpaceRequest.delete("http://" + ip + ":9200/_snapshot/{repoId}")//
				.routeParam("repoId", repository)//
				.go(200, 404);

		// first snapshot (returns 202 since wait for completion false)
		String firstSnapId = SpaceRequest.post("/1/snapshot")//
				.superdogAuth()//
				.go(202)//
				.getString("id");

		// fails since snapshot is not yet completed
		// returns 400 if not yet restorable or if restoreInfo is null
		SpaceResponse response = SpaceRequest.post("/1/snapshot/latest/restore")//
				.superdogAuth()//
				.go(400);

		// poll and wait for snapshot to complete
		do {

			response = SpaceRequest.get("/1/snapshot/latest")//
					.superdogAuth()//
					.go(200)//
					.assertEquals(firstSnapId, "id");

			// let server work a bit
			Thread.sleep(100);

		} while (!response.jsonNode().get("state").asText().equalsIgnoreCase("SUCCESS"));

		ObjectNode firstSnap = response.objectNode();

		// gets snapshot by id
		SpaceRequest.get("/1/snapshot/" + firstSnapId)//
				.superdogAuth()//
				.go(200)//
				.assertEquals(firstSnap);

		// creates another backend and credentials
		bbbb.signUpBackend();
		SpaceDog fred = signUp(bbbb, "fred", "hi fred");
		SpaceRequest.get("/1/login").userAuth(fred).go(200);

		// second snapshot (returns 201 since wait for completion true, 202
		// otherwise)
		response = SpaceRequest.post("/1/snapshot")//
				.superdogAuth()//
				.queryParam("waitForCompletion", "true")//
				.go(201)//
				.assertEquals(repository, "snapshot.repository")//
				.assertEquals("SUCCESS", "snapshot.state")//
				.assertEquals("all", "snapshot.type");

		ObjectNode secondSnap = (ObjectNode) response.get("snapshot");
		String secondSnapId = response.getString("id");

		SpaceRequest.get("/1/snapshot")//
				.superdogAuth()//
				.go(200)//
				.assertEquals(secondSnap, "results.0")//
				.assertEquals(firstSnap, "results.1");

		// create another account and add a credentials
		cccc.signUpBackend();
		SpaceDog nath = signUp(cccc, "nath", "hi nath");
		SpaceRequest.get("/1/login").userAuth(nath).go(200);

		// third snapshot (returns 200 since wait for completion true, 202
		// otherwise)
		response = SpaceRequest.post("/1/snapshot")//
				.superdogAuth()//
				.queryParam("waitForCompletion", "true")//
				.go(201)//
				.assertEquals(repository, "snapshot.repository")//
				.assertEquals("SUCCESS", "snapshot.state")//
				.assertEquals("all", "snapshot.type");

		ObjectNode thirdSnap = (ObjectNode) response.get("snapshot");
		String thirdSnapId = response.getString("id");

		SpaceRequest.get("/1/snapshot")//
				.superdogAuth()//
				.go(200)//
				.assertEquals(thirdSnap, "results.0")//
				.assertEquals(secondSnap, "results.1")//
				.assertEquals(firstSnap, "results.2");

		// restore to oldest snapshot
		SpaceRequest.post("/1/snapshot/{id}/restore")//
				.routeParam("id", firstSnapId)//
				.queryParam("waitForCompletion", "true")//
				.superdogAuth()//
				.go(200);

		// check only account aaaa and credentials vince are present
		SpaceRequest.get("/1/login").userAuth(vince).go(200);
		SpaceRequest.get("/1/login").userAuth(fred).go(401);
		SpaceRequest.get("/1/login").userAuth(nath).go(401);

		// restore to second (middle) snapshot
		SpaceRequest.post("/1/snapshot/{id}/restore")//
				.routeParam("id", secondSnapId)//
				.queryParam("waitForCompletion", "true")//
				.superdogAuth()//
				.go(200);

		// check only aaaa and bbbb accounts are present
		// check only vince and fred credentials are present
		SpaceRequest.get("/1/login").userAuth(vince).go(200);
		SpaceRequest.get("/1/login").userAuth(fred).go(200);
		SpaceRequest.get("/1/login").userAuth(nath).go(401);

		// restore to latest (third) snapshot
		SpaceRequest.post("/1/snapshot/{id}/restore")//
				.routeParam("id", thirdSnapId)//
				.queryParam("waitForCompletion", "true")//
				.superdogAuth()//
				.go(200);

		// check all accounts and credentials are present
		SpaceRequest.get("/1/login").userAuth(vince).go(200);
		SpaceRequest.get("/1/login").userAuth(fred).go(200);
		SpaceRequest.get("/1/login").userAuth(nath).go(200);

		// delete all accounts and internal indices
		SpaceRequest.delete("/1/backend").adminAuth(aaaa).go(200);
		SpaceRequest.delete("/1/backend").adminAuth(bbbb).go(200);
		SpaceRequest.delete("/1/backend").adminAuth(cccc).go(200);

		// check all accounts are deleted
		SpaceRequest.get("/1/login").userAuth(vince).go(401);
		SpaceRequest.get("/1/login").userAuth(fred).go(401);
		SpaceRequest.get("/1/login").userAuth(nath).go(401);

		// restore to latest (third) snapshot
		SpaceRequest.post("/1/snapshot/latest/restore")//
				.queryParam("waitForCompletion", "true")//
				.superdogAuth()//
				.go(200);

		// check all accounts and credentials are back
		SpaceRequest.get("/1/login").userAuth(vince).go(200);
		SpaceRequest.get("/1/login").userAuth(fred).go(200);
		SpaceRequest.get("/1/login").userAuth(nath).go(200);

		// check that restore to an invalid snapshot id fails
		SpaceRequest.post("/1/snapshot/xxxx/restore")//
				.queryParam("waitForCompletion", "true")//
				.superdogAuth()//
				.go(404);

		// check account administrator can not restore the platform
		SpaceRequest.post("/1/snapshot/latest/restore")//
				.adminAuth(aaaa)//
				.go(403);

		// clean up
		SpaceRequest.delete("/1/backend").adminAuth(aaaa).go(200);
		SpaceRequest.delete("/1/backend").adminAuth(bbbb).go(200);
		SpaceRequest.delete("/1/backend").adminAuth(cccc).go(200);

		// check snapshot list did not change since last snapshot
		SpaceRequest.get("/1/snapshot")//
				.superdogAuth()//
				.go(200)//
				.assertEquals(thirdSnap, "results.0")//
				.assertEquals(secondSnap, "results.1")//
				.assertEquals(firstSnap, "results.2");
	}
}
