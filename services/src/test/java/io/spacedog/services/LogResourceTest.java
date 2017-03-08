package io.spacedog.services;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Json;

public class LogResourceTest extends SpaceTest {

	@Test
	public void purgeBackendLogs() throws InterruptedException {

		// prepare
		prepareTest();
		SpaceDog test2 = resetTest2Backend();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = signUp(test, "fred", "hi fred");

		SpaceRequest.get("/1/data").userAuth(fred).go(200);
		SpaceRequest.get("/1/data").userAuth(fred).go(200);

		// superadmin gets test2 backend total log count to check
		// later that they aren't affected by test backend log purge
		int test2TotalLogs = SpaceRequest.get("/1/log").refresh().size(0)//
				.adminAuth(test2).go(200).get("total").asInt();

		// superadmin checks everything is in place
		String before = SpaceRequest.get("/1/log").refresh().adminAuth(test).go(200)//
				.assertEquals("/1/data", "results.0.path")//
				.assertEquals("/1/data", "results.1.path")//
				.assertEquals("/1/login", "results.2.path")//
				.assertEquals("/1/credentials", "results.3.path")//
				.assertEquals("/1/backend", "results.4.path")//
				.assertEquals("/1/backend", "results.5.path")//
				.getString("results.1.receivedAt");

		// superadmin deletes all logs before GET /data requests
		SpaceRequest.delete("/1/log").queryParam("before", before).adminAuth(test).go(200);

		// superadmin checks all test backend logs are deleted but ...
		before = SpaceRequest.get("/1/log").refresh().adminAuth(test).go(200)//
				.assertEquals(4, "total")//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/1/log", "results.1.path")//
				.assertEquals("GET", "results.2.method")//
				.assertEquals("/1/data", "results.2.path")//
				.assertEquals("GET", "results.3.method")//
				.assertEquals("/1/data", "results.3.path")//
				.getString("results.1.receivedAt");

		// superdog deletes all logs before GET /log requests
		SpaceRequest.delete("/1/log").queryParam("before", before).superdogAuth(test).go(200);

		// superadmin checks all test backend logs are deleted but ...
		SpaceRequest.get("/1/log").refresh().superdogAuth(test).go(200)//
				.assertEquals(4, "total")//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/1/log", "results.1.path")//
				.assertEquals("DELETE", "results.2.method")//
				.assertEquals("/1/log", "results.2.path")//
				.assertEquals("GET", "results.3.method")//
				.assertEquals("/1/log", "results.3.path");

		// superadmin checks test2 backend total log count is stable.
		// count = last time checked + 1, because of the first check.
		// It demonstrates purge of specific backend doesn't affect other
		// backends
		SpaceRequest.get("/1/log").refresh().size(0).adminAuth(test2).go(200)//
				.assertEquals(test2TotalLogs + 1, "total");

	}

	@Test
	public void purgeAllBackendLogs() throws InterruptedException {

		// prepare
		prepareTest();

		// superdog creates purge user in root backend
		superdogDeletesCredentials("api", "purgealltest");
		SpaceDog purgeUser = createTempUser("api", "purgealltest");

		// purge user fails to delete any logs
		// since it doesn't have the 'purgeall' role
		SpaceRequest.delete("/1/log").userAuth(purgeUser).go(403);

		// superdog assigns 'purgeall' role to purge user
		SpaceRequest.put("/1/credentials/" + purgeUser.id() + "/roles/purgeall")//
				.superdogAuth().go(200);

		// create test and test2 backends and users
		SpaceDog test = resetTestBackend();
		SpaceDog test2 = resetTest2Backend();
		SpaceDog fred = signUp(test, "fred", "hi fred");
		SpaceDog vince = signUp(test2, "vince", "hi vince");

		// data requests for logs
		SpaceRequest.get("/1/data").userAuth(fred).go(200);
		SpaceRequest.get("/1/data").userAuth(vince).go(200);
		SpaceRequest.get("/1/data").userAuth(fred).go(200);
		SpaceRequest.get("/1/data").userAuth(vince).go(200);

		// check everything is in place
		String before = SpaceRequest.get("/1/log").refresh().superdogAuth().go(200)//
				.assertEquals("/1/data", "results.0.path")//
				.assertEquals("test2", "results.0.credentials.backendId")//
				.assertEquals("/1/data", "results.1.path")//
				.assertEquals("test", "results.1.credentials.backendId")//
				.assertEquals("/1/data", "results.2.path")//
				.assertEquals("test2", "results.2.credentials.backendId")//
				.assertEquals("/1/data", "results.3.path")//
				.assertEquals("test", "results.3.credentials.backendId")//
				.assertEquals("/1/login", "results.4.path")//
				.assertEquals("test2", "results.4.credentials.backendId")//
				.assertEquals("/1/credentials", "results.5.path")//
				.assertEquals("test2", "results.5.credentials.backendId")//
				.assertEquals("/1/login", "results.6.path")//
				.assertEquals("test", "results.6.credentials.backendId")//
				.assertEquals("/1/credentials", "results.7.path")//
				.assertEquals("test", "results.7.credentials.backendId")//
				.getString("results.0.receivedAt");

		// purge user fails to get logs
		SpaceRequest.get("/1/log").userAuth(purgeUser).go(403);

		// purge user deletes all backend logs before the last data request
		SpaceRequest.delete("/1/log").queryParam("before", before).userAuth(purgeUser).go(200);

		// superdog checks all backend logs are deleted but ...
		before = SpaceRequest.get("/1/log").refresh().superdogAuth().go(200)//
				.assertEquals(4, "total")//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/1/log", "results.1.path")//
				.assertEquals("GET", "results.2.method")//
				.assertEquals("/1/log", "results.2.path")//
				.assertEquals("GET", "results.3.method")//
				.assertEquals("/1/data", "results.3.path")//
				.getString("results.0.receivedAt");

		// superdog deletes all backend logs before the delete request
		SpaceRequest.delete("/1/log").queryParam("before", before).superdogAuth().go(200);

		// superdog checks all backend logs are deleted but ...
		SpaceRequest.get("/1/log").refresh().superdogAuth().go(200)//
				.assertEquals(3, "total")//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/1/log", "results.1.path")//
				.assertEquals("DELETE", "results.2.method")//
				.assertEquals("/1/log", "results.2.path");
	}

	@Test
	public void superdogsCanBrowseAllBackendLogs() {

		prepareTest();

		// create test backends and users
		SpaceDog test = resetTestBackend();
		SpaceDog test2 = resetTest2Backend();

		signUp(test, "vince", "hi vince");
		signUp(test2, "fred", "hi fred");

		// superdog gets all backends logs
		SpaceRequest.get("/1/log").refresh().superdogAuth().go(200)//
				.assertEquals("/1/login", "results.0.path")//
				.assertEquals("test2", "results.0.credentials.backendId")//
				.assertEquals("/1/credentials", "results.1.path")//
				.assertEquals("test2", "results.1.credentials.backendId")//
				.assertEquals("/1/login", "results.2.path")//
				.assertEquals("test", "results.2.credentials.backendId")//
				.assertEquals("/1/credentials", "results.3.path")//
				.assertEquals("test", "results.3.credentials.backendId")//
				.assertEquals("/1/backend", "results.4.path")//
				.assertEquals("test2", "results.4.credentials.backendId")//
				.assertEquals("/1/backend", "results.5.path")//
				.assertEquals("test2", "results.5.credentials.backendId")//
				.assertEquals("/1/backend", "results.6.path")//
				.assertEquals("test", "results.6.credentials.backendId")//
				.assertEquals("/1/backend", "results.7.path")//
				.assertEquals("test", "results.7.credentials.backendId");

		// after backend deletion, logs are still accessible to superdogs
		test.backend().delete();
		test2.backend().delete();

		SpaceRequest.get("/1/log").refresh().superdogAuth().go(200)//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/backend", "results.0.path")//
				.assertEquals("test2", "results.0.credentials.backendId")//
				.assertEquals("DELETE", "results.1.method")//
				.assertEquals("/1/backend", "results.1.path")//
				.assertEquals("test", "results.1.credentials.backendId")//
				.assertEquals("GET", "results.2.method")//
				.assertEquals("/1/log", "results.2.path")//
				.assertEquals("api", "results.2.credentials.backendId")//
				.assertEquals("GET", "results.3.method")//
				.assertEquals("/1/login", "results.3.path")//
				.assertEquals("test2", "results.3.credentials.backendId")//
				.assertEquals("POST", "results.4.method")//
				.assertEquals("/1/credentials", "results.4.path")//
				.assertEquals("test2", "results.4.credentials.backendId")//
				.assertEquals("GET", "results.5.method")//
				.assertEquals("/1/login", "results.5.path")//
				.assertEquals("test", "results.5.credentials.backendId")//
				.assertEquals("POST", "results.6.method")//
				.assertEquals("/1/credentials", "results.6.path")//
				.assertEquals("test", "results.6.credentials.backendId");

	}

	@Test
	public void onlySuperdogsCanBrowseAllBackends() {

		// prepare
		prepareTest();

		// superdog creates superadmin named nath in root 'api' backend
		SpaceDog nath = SpaceDog.backend("api").username("nath")//
				.password("hi nath").email("nath@dog.com");
		String id = SpaceRequest.post("/1/credentials").superdogAuth()//
				.body("username", nath.username(), "password", nath.password().get(), //
						"email", nath.email().get(), "level", "SUPER_ADMIN")
				.go(201).getString("id");

		nath.id(id);

		// anonymous gets data from test backend
		SpaceRequest.get("/1/data").backendId("test").go();

		// superdog gets all backend logs and is able to review
		// previous test and root 'api' backend requests
		SpaceRequest.get("/1/log").refresh().superdogAuth().size(2).go(200)//
				.assertEquals("/1/data", "results.0.path")//
				.assertEquals("/1/credentials", "results.1.path");

		// but nath only gets logs from her root 'api' backend
		// since she's only superadmin and not superdog
		SpaceRequest.get("/1/log").refresh().userAuth(nath).size(2).go(200)//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("/1/credentials", "results.1.path");

		// clean nath credentials
		SpaceRequest.delete("/1/credentials/" + nath.id()).userAuth(nath).go(200);
	}

	@Test
	public void searchInLogs() {

		// prepare
		prepareTest();

		// creates test backend and user
		SpaceDog test = resetTestBackend();
		SpaceRequest.get("/1/data").backend(test).go(200);
		SpaceRequest.get("/1/data/user").backend(test).go(403);
		SpaceDog vince = signUp(test, "vince", "hi vince");
		SpaceRequest.get("/1/credentials/" + vince.id()).userAuth(vince).go(200);

		// superdog search for test backend logs with status 400 and higher
		SpaceRequest.post("/1/log/search").refresh().size(1).superdogAuth(test)//
				.body("range", Json.object("status", Json.object("gte", "400")))//
				.go(200)//
				.assertEquals("/1/data/user", "results.0.path")//
				.assertEquals(403, "results.0.status");

		// superdog search for test backend logs
		// with credentials level equal to SUPER_ADMIN and lower
		SpaceRequest.post("/1/log/search").size(7).superdogAuth(test)//
				.body("terms", Json.object("credentials.type", Json.array("SUPER_ADMIN", "USER", "KEY")))//
				.go(200)//
				.assertEquals("/1/credentials/" + vince.id(), "results.0.path")//
				.assertEquals("/1/login", "results.1.path")//
				.assertEquals("/1/credentials", "results.2.path")//
				.assertEquals("/1/data/user", "results.3.path")//
				.assertEquals("/1/data", "results.4.path")//
				.assertEquals("/1/backend", "results.5.path")//
				.assertEquals("/1/backend", "results.6.path");

		// superdog search for test backend log to only get USER and lower logs
		SpaceRequest.post("/1/log/search").size(7).superdogAuth(test)//
				.body("terms", Json.object("credentials.type", Json.array("USER", "KEY")))//
				.go(200)//
				.assertEquals("/1/credentials/" + vince.id(), "results.0.path")//
				.assertEquals("/1/login", "results.1.path")//
				.assertEquals("/1/credentials", "results.2.path")//
				.assertEquals("/1/data/user", "results.3.path")//
				.assertEquals("/1/data", "results.4.path");

		// superdog search for test backend log to only get KEY logs
		SpaceRequest.post("/1/log/search").size(3).superdogAuth(test)//
				.body("term", Json.object("credentials.type", "KEY"))//
				.go(200)//
				.assertEquals("/1/credentials", "results.0.path")//
				.assertEquals("/1/data/user", "results.1.path")//
				.assertEquals("/1/data", "results.2.path");

		// superdog gets all test backend logs
		SpaceRequest.post("/1/log/search").refresh().size(15).superdogAuth(test)//
				.body("match_all", Json.object())//
				.go(200)//
				.assertEquals("/1/log/search", "results.0.path")//
				.assertEquals("/1/log/search", "results.1.path")//
				.assertEquals("/1/log/search", "results.2.path")//
				.assertEquals("/1/log/search", "results.3.path")//
				.assertEquals("/1/credentials/" + vince.id(), "results.4.path")//
				.assertEquals("/1/login", "results.5.path")//
				.assertEquals("/1/credentials", "results.6.path")//
				.assertEquals("/1/data/user", "results.7.path")//
				.assertEquals("/1/data", "results.8.path")//
				.assertEquals("/1/backend", "results.9.path")//
				.assertEquals("/1/backend", "results.10.path");
	}

	@Test
	public void pingRequestAreNotLogged() {

		prepareTest();

		// load balancer pings a SpaceDog instance

		SpaceRequest.get("").go(200);

		// this ping should not be present in logs

		JsonNode results = SpaceRequest.get("/1/log")//
				.size(5).superdogAuth().go(200).get("results");

		Iterator<JsonNode> elements = results.elements();
		while (elements.hasNext()) {
			JsonNode element = elements.next();
			if (element.get("path").asText().equals("/")
					&& element.get("credentials").get("backendId").asText().equals(Backends.rootApi()))
				Assert.fail();
		}
	}
}
