/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Backend;
import io.spacedog.client.SpaceDogHelper.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class BackendResourceTestOften extends Assert {

	@Test
	public void deleteSignUpGetLoginTestBackend() throws Exception {

		SpaceDogHelper.prepareTest();
		Backend testBackend = new Backend("test", "test", "hi test", "hello@spacedog.io");
		SpaceDogHelper.resetBackend(testBackend, true);

		// get just created test account should succeed

		SpaceRequest.get("/1/backend?refresh=true")//
				.basicAuth(testBackend).go(200)//
				.assertEquals("test", "backendId")//
				.assertEquals("test", "superAdmins.0.username")//
				.assertEquals("hello@spacedog.io", "superAdmins.0.email");

		// create new account with different backendId but same username should
		// succeed

		SpaceDogHelper.resetBackend("test1", "test", "hi test");

		// create new account with same backend id should fail

		SpaceRequest.post("/1/backend/test")
				.body(//
						Json.objectBuilder()//
								.put("backendId", "test")//
								.put("username", "anotheruser")//
								.put("password", "hi anotheruser")//
								.put("email", "hello@spacedog.io")//
								.build())
				.go(400)//
				.assertEquals("test", "invalidParameters.backendId.value");

		// admin user login should succeed

		SpaceRequest.get("/1/login").basicAuth(testBackend).go(200);

		// no header no user login should fail

		SpaceRequest.get("/1/login").go(401);

		// invalid admin username login should fail

		SpaceRequest.get("/1/login").basicAuth(testBackend, "XXX", "hi test").go(401);

		// invalid admin password login should fail

		SpaceRequest.get("/1/login").basicAuth(testBackend, "test", "hi XXX").go(401);

		// data access with key credentials should succeed

		SpaceRequest.get("/1/data?refresh=true").backend(testBackend).go(200)//
				.assertSizeEquals(1, "results")//
				.assertEquals("test", "results.0.username");

		// data access with admin user should succeed

		SpaceRequest.get("/1/data").basicAuth(testBackend).go(200)//
				.assertSizeEquals(1, "results")//
				.assertEquals("test", "results.0.username");

		// let's create a common user in 'test' backend

		User john = SpaceDogHelper.createUser(testBackend, "john", "hi john", "john@dog.io");

		// admin access with regular user and backend key should fail

		SpaceRequest.get("/1/backend").basicAuth(john).go(401);

	}

	@Test
	public void AdminCreatesGetsAndDeletesItsBackend() throws Exception {

		// prepare

		SpaceDogHelper.prepareTest();
		Backend aaaa = SpaceDogHelper.resetBackend("aaaa", "aaaa", "hi aaaa");
		Backend zzzz = SpaceDogHelper.resetBackend("zzzz", "zzzz", "hi zzzz");

		// super admin gets his backend

		ObjectNode aaaaNode = SpaceRequest.get("/1/backend").basicAuth(aaaa).go(200).objectNode();
		ObjectNode zzzzNode = SpaceRequest.get("/1/backend").basicAuth(zzzz).go(200).objectNode();

		// superdog gets all backends

		SpaceRequest.get("/1/backend").superdogAuth().go(200)//
				.assertContains(aaaaNode, "results")//
				.assertContains(zzzzNode, "results");

		// super admin fails to access a backend he does not own

		SpaceRequest.get("/1/backend").basicAuth("aaaa", zzzz.username, zzzz.password).go(401);
		SpaceRequest.get("/1/backend").basicAuth("zzzz", aaaa.username, aaaa.username).go(401);

		// super admin fails to delete a backend he does not own

		SpaceRequest.delete("/1/backend").basicAuth("aaaa", zzzz.username, zzzz.password).go(401);
		SpaceRequest.delete("/1/backend").basicAuth("zzzz", aaaa.username, aaaa.username).go(401);

		// super admin fails to delete himself
		// since backends must at least have one super admin

		// TODO does not work yet
		// SpaceRequest.delete("/1/user/aaaa").basicAuth(aaaa).go(400);
		// SpaceRequest.delete("/1/user/zzzz").basicAuth(zzzz).go(400);

		// super admin can delete his backend

		SpaceRequest.delete("/1/backend").basicAuth(aaaa).go(200);
		SpaceRequest.delete("/1/backend").basicAuth(zzzz).go(200);
	}

}
