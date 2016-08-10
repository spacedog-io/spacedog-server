/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class BackendResourceTestOften extends Assert {

	@Test
	public void deleteSignUpGetLoginTestBackend() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// gets backend info

		SpaceRequest.get("/1/backend")//
				.adminAuth(test).go(200)//
				.assertEquals("test", "backendId")//
				.assertEquals("test", "superAdmins.0.username")//
				.assertEquals("hello@spacedog.io", "superAdmins.0.email");

		// empty backend returns no data

		SpaceRequest.get("/1/data").refresh().backend(test).go(200)//
				.assertSizeEquals(0, "results");

		// fails to return users before user schema is set

		SpaceRequest.get("/1/user").adminAuth(test).go(404);

		// creates new backend with different id but same username

		SpaceClient.resetBackend("test1", "test", "hi test");

		// fails to create new backend with non available id

		SpaceRequest.post("/1/backend/test")
				.body("backendId", "test", "username", "anotheruser", //
						"password", "hi anotheruser", "email", "hello@spacedog.io")//
				.go(400)//
				.assertEquals("test", "invalidParameters.backendId.value");

		// admin successfully logins

		SpaceRequest.get("/1/login").adminAuth(test).go(200);

		// login fails if no backend nor user set

		SpaceRequest.get("/1/login").go(400);

		// invalid admin username login fails

		SpaceRequest.get("/1/login").basicAuth(test, "XXX", "hi test").go(401);

		// invalid admin password login fails

		SpaceRequest.get("/1/login").basicAuth(test, "test", "hi XXX").go(401);

		// data access with without credentials succeeds

		SpaceRequest.get("/1/data").refresh().backend(test).go(200)//
				.assertSizeEquals(0, "results");

		// data access with admin user succeeds

		SpaceRequest.get("/1/data").adminAuth(test).go(200)//
				.assertSizeEquals(0, "results");

		// let's create a common user

		User john = SpaceClient.newCredentials(test, "john", "hi john", "john@dog.io");

		// user fails to get backend data since it is restricted to admins

		SpaceRequest.get("/1/backend").userAuth(john).go(403);
	}

	@Test
	public void AdminCreatesGetsAndDeletesItsBackend() throws Exception {

		// prepare

		SpaceClient.prepareTest();
		Backend aaaa = SpaceClient.resetBackend("aaaa", "aaaa", "hi aaaa");
		Backend zzzz = SpaceClient.resetBackend("zzzz", "zzzz", "hi zzzz");

		// super admin gets his backend

		ObjectNode aaaaNode = SpaceRequest.get("/1/backend").adminAuth(aaaa).go(200).objectNode();
		ObjectNode zzzzNode = SpaceRequest.get("/1/backend").adminAuth(zzzz).go(200).objectNode();

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

		SpaceRequest.delete("/1/backend").adminAuth(aaaa).go(200);
		SpaceRequest.delete("/1/backend").adminAuth(zzzz).go(200);
	}

	@Test
	public void invalidBackendIdentifiers() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		SpaceClient.deleteTestBackend();
		ObjectNode body = Json.object("username", "test", //
				"password", "hi test", "email", "hello@spacedog.io");

		// fails to create a backend whom id contains invalid characters
		SpaceRequest.post("/1/backend/xxx-xxx").body(body).go(400);

		// fails to create a backend whom id is not lowercase
		SpaceRequest.post("/1/backend/XXXX").body(body).go(400);

		// fails to create backend whom id is not at least 4 characters long
		SpaceRequest.post("/1/backend/xxx").body(body).go(400);

		// user fails to create a backend whom id contains spacedog
		SpaceRequest.post("/1/backend/xxxspacedogxxx").body(body).go(400);

		// user fails to create a backend whom id starts with api
		SpaceRequest.post("/1/backend/apixxx").body(body).go(400);
	}

	@Test
	public void pingServerToCheckItIsUpAndRunning() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		SpaceTarget target = SpaceRequest.configuration().target();

		// successfully pings server to check it is up and running
		SpaceRequest.get("").go(200);
		SpaceRequest.get("/").go(200);

		// successfully pings localhost without any backend subdomain
		// this can and must be tested on local server only
		if (target.equals(SpaceTarget.local))
			SpaceRequest.get("http://localhost:8443").go(200);

		// successfully pings server with an ip address
		// this can and must be tested on local server only
		if (target.equals(SpaceTarget.local))
			SpaceRequest.get("http://127.0.0.1:8443").go(200);
	}
}
