/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.client.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceParams;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class BackendResourceTestOften extends SpaceTest {

	@Test
	public void deleteSignUpGetLoginTestBackend() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// gets backend super admin info
		SpaceRequest.get("/1/backend")//
				.adminAuth(test).go(200)//
				.assertEquals("test", "results.0.backendId")//
				.assertEquals("test", "results.0.username")//
				.assertEquals("platform@spacedog.io", "results.0.email");

		// empty backend returns no data
		SpaceRequest.get("/1/data").refresh().backend(test).go(200)//
				.assertSizeEquals(0, "results");

		// creates new backend with different id but same username
		resetBackend("test1", "test", "hi test");

		// fails to create new backend with non available id
		SpaceRequest.post("/1/backend/test")
				.body("backendId", "test", "username", "anotheruser", //
						"password", "hi anotheruser", "email", "hello@spacedog.io")//
				.go(400)//
				.assertEquals("test", "invalidParameters.backendId.value");

		// admin successfully logins
		test.login();

		// login is not possible for anonymous users
		SpaceRequest.get("/1/login").go(403);
		SpaceRequest.get("/1/login").backend(test).go(403);

		// invalid admin username login fails
		SpaceRequest.get("/1/login").basicAuth("test", "XXX", "hi test").go(401);

		// invalid admin password login fails
		SpaceRequest.get("/1/login").basicAuth("test", "test", "hi XXX").go(401);

		// data access with without credentials succeeds
		SpaceRequest.get("/1/data").refresh().backend(test).go(200)//
				.assertSizeEquals(0, "results");

		// data access with admin user succeeds
		SpaceRequest.get("/1/data").adminAuth(test).go(200)//
				.assertSizeEquals(0, "results");

		// let's create a common user
		SpaceDog john = SpaceDog.backend("test").username("john").signUp("hi john");

		// user fails to get backend data since it is restricted to admins
		SpaceRequest.get("/1/backend").userAuth(john).go(403);
	}

	@Test
	public void invalidBackendIdentifiers() {

		// prepare
		prepareTest();
		SpaceDog.backend("test").username("test").password("hi test").backend().delete();
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
		prepareTest();
		SpaceTarget target = SpaceRequest.env().target();

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

	@Test
	public void iCanCreateABackendWithANonWildCardUrl() {

		// prepare
		prepareTest();

		// super admin deletes the test backend
		SpaceDog.backend("test").username("test").password("hi test").backend().delete();

		// it is also possible to create a backend with the same request host as
		// other backend requests. Example https://cel.suez.fr
		// This is very useful for mono backend servers with a non wildcard dns
		// record
		SpaceRequest.post("/1/backend").backendId("test")//
				.queryParam(SpaceParams.PARAM_NOTIF, "false")//
				.body("username", "test", "password", "hi test", "email", "test@test.fr")//
				.go(201);

		// super admin gets its backend info
		SpaceRequest.get("/1/backend").basicAuth("test", "test", "hi test").go(200)//
				.assertEquals(1, "total")//
				.assertEquals("test", "results.0.backendId")//
				.assertEquals("test", "results.0.username")//
				.assertEquals("test@test.fr", "results.0.email");
	}
}
