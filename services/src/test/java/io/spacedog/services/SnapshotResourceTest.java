package io.spacedog.services;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;

public class SnapshotResourceTest extends Assert {

	@Test
	public void snapshotAndRestoreMultipleTimes() throws Exception {

		// create an account and add a user
		Account aaaaAccount = SpaceDogHelper.resetAccount("aaaa", "aaaa", "hi aaaa", "aaaa@dog.com");
		SpaceDogHelper.createUser(aaaaAccount, "vince", "hi vince", "vince@dog.com");
		SpaceRequest.get("/v1/user/vince").backendKey(aaaaAccount).go(200);

		// delete the current repository to force repo creation by this test
		// use full url to avoid delete by mistake any prod repo
		String repository = DateTime.now().withZone(DateTimeZone.UTC).toString("yyyy-ww");
		SpaceRequest.delete("http://localhost:9200/_snapshot/{repoId}")//
				.routeParam("repoId", repository)//
				.go(200, 404);

		// first snapshot (returns 202 since wait for completion false)
		String firstSnapId = SpaceRequest.post("/v1/dog/snapshot")//
				.promptAuth()//
				.go(202)//
				.getFromJson("id")//
				.asText();

		// fails since snapshot is not yet completed
		// returns 400 if not yet restorable or if restoreInfo is null
		SpaceResponse response = SpaceRequest.post("/v1/dog/snapshot/latest/restore")//
				.promptAuth()//
				.go(400);

		// poll and wait for snapshot to complete
		do {

			response = SpaceRequest.get("/v1/dog/snapshot/latest")//
					.promptAuth()//
					.go(200)//
					.assertEquals(firstSnapId, "id");

			// let server work a bit
			Thread.sleep(100);

		} while (!response.jsonNode().get("state").asText().equalsIgnoreCase("SUCCESS"));

		ObjectNode firstSnap = response.objectNode();

		// check get snapshot by id is correct
		SpaceRequest.get("/v1/dog/snapshot/" + firstSnapId)//
				.promptAuth()//
				.go(200)//
				.assertEquals(firstSnap);

		// create another account and add a user
		Account bbbbAccount = SpaceDogHelper.resetAccount("bbbb", "bbbb", "hi bbbb", "bbbb@dog.com");
		SpaceDogHelper.createUser(bbbbAccount, "fred", "hi fred", "fred@dog.com");
		SpaceRequest.get("/v1/user/fred").backendKey(bbbbAccount).go(200);

		// second snapshot (returns 201 since wait for completion true, 202
		// otherwise)
		response = SpaceRequest.post("/v1/dog/snapshot")//
				.promptAuth()//
				.queryString("waitForCompletion", "true")//
				.go(201)//
				.assertEquals(repository, "snapshot.repository")//
				.assertEquals("SUCCESS", "snapshot.state")//
				.assertEquals("all", "snapshot.type");

		ObjectNode secondSnap = (ObjectNode) response.getFromJson("snapshot");
		String secondSnapId = response.getFromJson("id").asText();

		SpaceRequest.get("/v1/dog/snapshot")//
				.promptAuth()//
				.go(200)//
				.assertEquals(secondSnap, "results.0")//
				.assertEquals(firstSnap, "results.1");

		// create another account and add a user
		Account ccccAccount = SpaceDogHelper.resetAccount("cccc", "cccc", "hi cccc", "cccc@dog.com");
		SpaceDogHelper.createUser(ccccAccount, "nath", "hi nath", "nath@dog.com");
		SpaceRequest.get("/v1/user/nath").backendKey(ccccAccount).go(200);

		// third snapshot (returns 200 since wait for completion true, 202
		// otherwise)
		response = SpaceRequest.post("/v1/dog/snapshot")//
				.promptAuth()//
				.queryString("waitForCompletion", "true")//
				.go(201)//
				.assertEquals(repository, "snapshot.repository")//
				.assertEquals("SUCCESS", "snapshot.state")//
				.assertEquals("all", "snapshot.type");

		ObjectNode thirdSnap = (ObjectNode) response.getFromJson("snapshot");
		String thirdSnapId = response.getFromJson("id").asText();

		SpaceRequest.get("/v1/dog/snapshot")//
				.promptAuth()//
				.go(200)//
				.assertEquals(thirdSnap, "results.0")//
				.assertEquals(secondSnap, "results.1")//
				.assertEquals(firstSnap, "results.2");

		// restore to oldest snapshot
		SpaceRequest.post("/v1/dog/snapshot/{id}/restore")//
				.routeParam("id", firstSnapId)//
				.queryString("waitForCompletion", "true")//
				.promptAuth()//
				.go(200);

		// check only account aaaa and user vince are present
		SpaceRequest.get("/v1/user/vince").backendKey(aaaaAccount).go(200);
		SpaceRequest.get("/v1/user/fred").backendKey(bbbbAccount).go(401);
		SpaceRequest.get("/v1/user/nath").backendKey(ccccAccount).go(401);

		// restore to second (middle) snapshot
		SpaceRequest.post("/v1/dog/snapshot/{id}/restore")//
				.routeParam("id", secondSnapId)//
				.queryString("waitForCompletion", "true")//
				.promptAuth()//
				.go(200);

		// check only aaaa and bbbb accounts are present
		// check only vince and fred users are present
		SpaceRequest.get("/v1/user/vince").backendKey(aaaaAccount).go(200);
		SpaceRequest.get("/v1/user/fred").backendKey(bbbbAccount).go(200);
		SpaceRequest.get("/v1/user/nath").backendKey(ccccAccount).go(401);

		// restore to latest (third) snapshot
		SpaceRequest.post("/v1/dog/snapshot/{id}/restore")//
				.routeParam("id", thirdSnapId)//
				.queryString("waitForCompletion", "true")//
				.promptAuth()//
				.go(200);

		// check all accounts and users are present
		SpaceRequest.get("/v1/user/vince").backendKey(aaaaAccount).go(200);
		SpaceRequest.get("/v1/user/fred").backendKey(bbbbAccount).go(200);
		SpaceRequest.get("/v1/user/nath").backendKey(ccccAccount).go(200);

		// delete all accounts and internal indices
		// TODO delete all spacedog internal indices
		// TODO authentication with spacedog admin account to be able to delete
		// aaaa
		// SpaceRequest.delete("/v1/admin/account/aaaa").basicAuth(aaaaAccount).go(200);
		SpaceRequest.delete("/v1/admin/account/bbbb").basicAuth(bbbbAccount).go(200);
		SpaceRequest.delete("/v1/admin/account/cccc").basicAuth(ccccAccount).go(200);

		// check all accounts are deleted
		// TODO authentication with spacedog admin account to be able to delete
		// aaaa
		// SpaceRequest.get("/v1/user/vince").backendKey(aaaaAccount).go(401);
		SpaceRequest.get("/v1/user/fred").backendKey(bbbbAccount).go(401);
		SpaceRequest.get("/v1/user/nath").backendKey(ccccAccount).go(401);

		// restore to latest (third) snapshot
		SpaceRequest.post("/v1/dog/snapshot/latest/restore")//
				.queryString("waitForCompletion", "true")//
				.promptAuth()//
				.go(200);

		// check all accounts and users are back
		SpaceRequest.get("/v1/user/vince").backendKey(aaaaAccount).go(200);
		SpaceRequest.get("/v1/user/fred").backendKey(bbbbAccount).go(200);
		SpaceRequest.get("/v1/user/nath").backendKey(ccccAccount).go(200);

		// check that restore to an invalid snapshot id fails
		SpaceRequest.post("/v1/dog/snapshot/xxxx/restore")//
				.queryString("waitForCompletion", "true")//
				.promptAuth()//
				.go(404);

		// check account administrator can not restore the platform
		SpaceRequest.post("/v1/dog/snapshot/latest/restore")//
				.basicAuth(aaaaAccount)//
				.go(401);

		// clean up
		SpaceRequest.delete("/v1/admin/account/aaaa").basicAuth(aaaaAccount).go(200);
		SpaceRequest.delete("/v1/admin/account/bbbb").basicAuth(bbbbAccount).go(200);
		SpaceRequest.delete("/v1/admin/account/cccc").basicAuth(ccccAccount).go(200);

		// check snapshot list did not change since last snapshot
		SpaceRequest.get("/v1/dog/snapshot")//
				.promptAuth()//
				.go(200)//
				.assertEquals(thirdSnap, "results.0")//
				.assertEquals(secondSnap, "results.1")//
				.assertEquals(firstSnap, "results.2");
	}
}
