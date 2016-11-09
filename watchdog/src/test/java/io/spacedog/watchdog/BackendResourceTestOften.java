/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.client.SpaceTarget;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class BackendResourceTestOften extends Assert {

	@Test
	public void rootBackendShallNotBeDeleted() {
		// prepare
		SpaceClient.prepareTest();

		// root backend can not be deleted
		SpaceRequest.delete("/1/backend").superdogAuth().go(400);
	}

	@Test
	public void deleteSignUpGetLoginTestBackend() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// gets backend super admin info

		SpaceRequest.get("/1/backend")//
				.adminAuth(test).go(200)//
				.assertEquals("test", "results.0.backendId")//
				.assertEquals("test", "results.0.username")//
				.assertEquals("hello@spacedog.io", "results.0.email");

		// empty backend returns no data

		SpaceRequest.get("/1/data").refresh().backend(test).go(200)//
				.assertSizeEquals(0, "results");

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

		// login is not possible for anonymous users
		SpaceRequest.get("/1/login").go(403);
		SpaceRequest.get("/1/login").backend(test).go(403);

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

		User john = SpaceClient.signUp(test, "john", "hi john", "john@dog.io");

		// user fails to get backend data since it is restricted to admins

		SpaceRequest.get("/1/backend").userAuth(john).go(403);
	}

	@Test
	public void AdminCreatesGetsAndDeletesItsBackend() {

		// prepare

		SpaceClient.prepareTest();
		Backend aaaa = SpaceClient.resetBackend("aaaa", "aaaa", "hi aaaa");
		Backend zzzz = SpaceClient.resetBackend("zzzz", "zzzz", "hi zzzz");

		// super admin gets his backend

		JsonNode aaaaSuperAdmin = SpaceRequest.get("/1/backend").adminAuth(aaaa).go(200)//
				.objectNode().get("results").get(0);
		JsonNode zzzzSuperAdmin = SpaceRequest.get("/1/backend").adminAuth(zzzz).go(200)//
				.objectNode().get("results").get(0);

		// superdog browse all backends and finds aaaa and zzzz

		boolean aaaaFound = false, zzzzFound = false;
		int from = 0, size = 100, total = 0;

		do {
			SpaceResponse response = SpaceRequest.get("/1/backend").from(from).size(size)//
					.superdogAuth().go(200);

			aaaaFound = Iterators.contains(response.get("results").elements(), aaaaSuperAdmin);
			zzzzFound = Iterators.contains(response.get("results").elements(), zzzzSuperAdmin);

			total = response.get("total").asInt();
			from = from + size;

		} while (aaaaFound && zzzzFound && from < total);

		// super admin fails to access a backend he does not own

		SpaceRequest.get("/1/backend")//
				.basicAuth("aaaa", zzzz.adminUser.username, zzzz.adminUser.password).go(401);
		SpaceRequest.get("/1/backend")//
				.basicAuth("zzzz", aaaa.adminUser.username, aaaa.adminUser.username).go(401);

		// super admin fails to delete a backend he does not own

		SpaceRequest.delete("/1/backend")//
				.basicAuth("aaaa", zzzz.adminUser.username, zzzz.adminUser.password).go(401);
		SpaceRequest.delete("/1/backend")//
				.basicAuth("zzzz", aaaa.adminUser.username, aaaa.adminUser.username).go(401);

		// super admin can delete his backend

		SpaceRequest.delete("/1/backend").adminAuth(aaaa).go(200);
		SpaceRequest.delete("/1/backend").adminAuth(zzzz).go(200);
	}

	@Test
	public void invalidBackendIdentifiers() {

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
	public void pingServerToCheckItIsUpAndRunning() {

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
