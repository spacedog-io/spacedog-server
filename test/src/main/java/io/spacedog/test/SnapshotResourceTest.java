package io.spacedog.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceResponse;
import io.spacedog.http.SpaceTest;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Optional7;

public class SnapshotResourceTest extends SpaceTest {

	@Test
	public void snapshotAndRestoreMultipleTimes() throws InterruptedException, UnknownHostException {

		// prepare
		prepareTest();
		deleteBackend("test1");
		deleteBackend("test2");
		deleteBackend("test3");

		// superdog creates snapshotall user in root backend
		SpaceDog superdog = superdog();
		Optional7<Credentials> optional = superdog.credentials().getByUsername("snaptest");
		if (optional.isPresent())
			superdog.credentials().delete(optional.get().id());

		SpaceDog snapDog = createTempDog(superdog, "snaptest");
		superdog.credentials().setRole(snapDog.id(), "snapshotall");

		// creates backend and credentials
		SpaceDog test1 = createBackend("test1");
		SpaceDog vince = createTempDog(test1, "vince");

		// deletes the current repository to force repo creation by this test
		// use full url to avoid delete by mistake any prod repo
		String repository = DateTime.now().withZone(DateTimeZone.UTC).toString("yyyy-ww");
		String ip = InetAddress.getLocalHost().getHostAddress();
		SpaceRequest.delete("/_snapshot/{repoId}")//
				.backend("http://" + ip + ":9200")//
				.routeParam("repoId", repository)//
				.go(200, 404);

		// first snapshot
		// returns 202 since wait for completion false
		// snapshot authorized because superdog credentials and root backend
		String firstSnapId = superdog.post("/1/snapshot")//
				.go(202)//
				.getString("id");

		// fails since snapshot is not yet completed
		// returns 400 if not yet restorable or if restoreInfo is null
		SpaceResponse response = superdog.post("/1/snapshot/latest/restore").go(400);

		// poll and wait for snapshot to complete
		do {

			response = superdog.get("/1/snapshot/latest").go(200)//
					.assertEquals(firstSnapId, "id");

			// let server work a bit
			Thread.sleep(100);

		} while (!response.asJson().get("state").asText().equalsIgnoreCase("SUCCESS"));

		ObjectNode firstSnap = response.asJsonObject();

		// gets snapshot by id
		superdog.get("/1/snapshot/" + firstSnapId).go(200)//
				.assertEquals(firstSnap);

		// creates another backend and credentials
		SpaceDog test2 = createBackend("test2");
		SpaceDog fred = createTempDog(test2, "fred");

		// second snapshot
		// returns 201 since wait for completion true (202 otherwise)
		// Authorized since superdog even with non root backend
		response = superdog("test").post("/1/snapshot")//
				.queryParam("waitForCompletion", "true")//
				.go(201)//
				.assertEquals(repository, "snapshot.repository")//
				.assertEquals("SUCCESS", "snapshot.state")//
				.assertEquals("all", "snapshot.type");

		ObjectNode secondSnap = (ObjectNode) response.get("snapshot");
		String secondSnapId = response.getString("id");

		superdog.get("/1/snapshot").go(200)//
				.assertEquals(secondSnap, "results.0")//
				.assertEquals(firstSnap, "results.1");

		// create another account and add a credentials
		SpaceDog test3 = createBackend("test3");
		SpaceDog nath = createTempDog(test3, "nath");

		// third snapshot
		// returns 201 since wait for completion true (202 otherwise)
		// Authorized since snapshotUser is a root backend user
		// and has the snapshotall role
		response = snapDog.post("/1/snapshot")//
				.queryParam("waitForCompletion", "true")//
				.go(201)//
				.assertEquals(repository, "snapshot.repository")//
				.assertEquals("SUCCESS", "snapshot.state")//
				.assertEquals("all", "snapshot.type");

		ObjectNode thirdSnap = (ObjectNode) response.get("snapshot");
		String thirdSnapId = response.getString("id");

		// snapshotAll gets first and second latest snapshots info
		snapDog.get("/1/snapshot").from(0).size(2).go(200)//
				.assertSizeEquals(2, "results")//
				.assertEquals(thirdSnap, "results.0")//
				.assertEquals(secondSnap, "results.1");

		// snapshotAll gets third latest snapshot info
		snapDog.get("/1/snapshot").from(2).size(1).go(200)//
				.assertSizeEquals(1, "results")//
				.assertEquals(firstSnap, "results.0");

		// restore to oldest snapshot
		superdog.post("/1/snapshot/{id}/restore")//
				.routeParam("id", firstSnapId)//
				.queryParam("waitForCompletion", "true")//
				.go(200);

		// check only account aaaa and credentials vince are present
		vince.login();
		fred.get("/1/login").go(401);
		nath.get("/1/login").go(401);

		// restore to second (middle) snapshot
		superdog.post("/1/snapshot/{id}/restore")//
				.routeParam("id", secondSnapId)//
				.queryParam("waitForCompletion", "true")//
				.go(200);

		// check only aaaa and bbbb accounts are present
		// check only vince and fred credentials are present
		vince.login();
		fred.login();
		nath.get("/1/login").go(401);

		// restore to latest (third) snapshot
		superdog.post("/1/snapshot/{id}/restore")//
				.routeParam("id", thirdSnapId)//
				.queryParam("waitForCompletion", "true")//
				.go(200);

		// check all accounts and credentials are present
		vince.login();
		fred.login();
		nath.login();

		// delete all accounts and internal indices
		deleteBackend("test1");
		deleteBackend("test2");
		deleteBackend("test3");

		// check all accounts are deleted
		vince.get("/1/login").go(401);
		fred.get("/1/login").go(401);
		nath.get("/1/login").go(401);

		// restore to latest (third) snapshot
		superdog.post("/1/snapshot/latest/restore")//
				.queryParam("waitForCompletion", "true")//
				.go(200);

		// check all accounts and credentials are back
		vince.login();
		fred.login();
		nath.login();

		// fails to restore snapshot if invalid id format
		superdog.post("/1/snapshot/xxxx/restore")//
				.queryParam("waitForCompletion", "true")//
				.go(400);

		// fails to get snapshot if id not found
		superdog.get("/1/snapshot/all-utc-2011-01-01-00-00-00-000").go(404);

		// check account administrator can not restore the platform
		test1.post("/1/snapshot/latest/restore").go(403);

		// clean up
		deleteBackend("test1");
		deleteBackend("test2");
		deleteBackend("test3");

		// check snapshot list did not change since last snapshot
		superdog.get("/1/snapshot").go(200)//
				.assertEquals(thirdSnap, "results.0")//
				.assertEquals(secondSnap, "results.1")//
				.assertEquals(firstSnap, "results.2");
	}
}
