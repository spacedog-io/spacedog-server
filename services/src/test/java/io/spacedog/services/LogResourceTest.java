package io.spacedog.services;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Json;

public class LogResourceTest extends Assert {

	@Test
	public void purgeBackendLogs() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		SpaceClient.signUp(test, "fred", "hi fred");

		for (int i = 0; i < 5; i++)
			SpaceRequest.get("/1/data").adminAuth(test).go(200);

		// check everything is in place
		SpaceRequest.get("/1/log").refresh().size(9).adminAuth(test).go(200)//
				.assertEquals("/1/data", "results.0.path")//
				.assertEquals("/1/data", "results.1.path")//
				.assertEquals("/1/data", "results.2.path")//
				.assertEquals("/1/data", "results.3.path")//
				.assertEquals("/1/data", "results.4.path")//
				.assertEquals("/1/login", "results.5.path")//
				.assertEquals("/1/credentials", "results.6.path")//
				.assertEquals("/1/backend", "results.7.path")//
				.assertEquals("/1/backend", "results.8.path");

		// delete all logs but the 2 last requests
		SpaceRequest.delete("/1/log").from(2).superdogAuth(test).go(200);

		// check all test backend logs are deleted but ...
		int total = SpaceRequest.get("/1/log").refresh().size(10).adminAuth(test).go(200)//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/1/log", "results.1.path")//
				.assertEquals("GET", "results.2.method")//
				.assertEquals("/1/data", "results.2.path")//
				.get("total").asInt();

		// size should be between 3 and 4 depending on index refresh
		// delete request might be executed before GET /1/log is indexed
		assertTrue(total <= 4);
	}

	@Test
	public void purgeAllBackendLogs() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		Backend test2 = SpaceClient.resetTest2Backend();

		SpaceClient.signUp(test, "fred", "hi fred");
		SpaceClient.signUp(test2, "vince", "hi vince");

		for (int i = 0; i < 5; i++) {
			SpaceRequest.get("/1/data").adminAuth(test).go(200);
			SpaceRequest.get("/1/data").adminAuth(test2).go(200);
		}

		// check everything is in place
		SpaceRequest.get("/1/log").refresh().size(18).superdogAuth().go(200)//
				.assertEquals("/1/data", "results.0.path")//
				.assertEquals("test2", "results.0.credentials.backendId")//
				.assertEquals("/1/data", "results.1.path")//
				.assertEquals("test", "results.1.credentials.backendId")//
				.assertEquals("/1/data", "results.2.path")//
				.assertEquals("test2", "results.2.credentials.backendId")//
				.assertEquals("/1/data", "results.3.path")//
				.assertEquals("test", "results.3.credentials.backendId")//
				.assertEquals("/1/data", "results.4.path")//
				.assertEquals("test2", "results.4.credentials.backendId")//
				.assertEquals("/1/data", "results.5.path")//
				.assertEquals("test", "results.5.credentials.backendId")//
				.assertEquals("/1/data", "results.6.path")//
				.assertEquals("test2", "results.6.credentials.backendId")//
				.assertEquals("/1/data", "results.7.path")//
				.assertEquals("test", "results.7.credentials.backendId")//
				.assertEquals("/1/data", "results.8.path")//
				.assertEquals("test2", "results.8.credentials.backendId")//
				.assertEquals("/1/data", "results.9.path")//
				.assertEquals("test", "results.9.credentials.backendId")//
				.assertEquals("/1/login", "results.10.path")//
				.assertEquals("test2", "results.10.credentials.backendId")//
				.assertEquals("/1/credentials", "results.11.path")//
				.assertEquals("test2", "results.11.credentials.backendId")//
				.assertEquals("/1/login", "results.12.path")//
				.assertEquals("test", "results.12.credentials.backendId")//
				.assertEquals("/1/credentials", "results.13.path")//
				.assertEquals("test", "results.13.credentials.backendId")//
				.assertEquals("/1/backend", "results.14.path")//
				.assertEquals("test2", "results.14.credentials.backendId")//
				.assertEquals("/1/backend", "results.15.path")//
				.assertEquals("test2", "results.15.credentials.backendId")//
				.assertEquals("/1/backend", "results.16.path")//
				.assertEquals("test", "results.16.credentials.backendId")//
				.assertEquals("/1/backend", "results.17.path")//
				.assertEquals("test", "results.17.credentials.backendId");

		// delete all logs but the 2 last requests
		SpaceRequest.delete("/1/log").from(2).superdogAuth().go(200);

		// check all test backend logs are deleted but ...
		int total = SpaceRequest.get("/1/log").refresh().superdogAuth().go(200)//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/1/log", "results.1.path")//
				.assertEquals("GET", "results.2.method")//
				.assertEquals("/1/data", "results.2.path")//
				.get("total").asInt();

		// size should be between 3 and 4 depending on index refresh
		// delete request might be executed before GET /1/log is indexed
		assertTrue(total <= 4);
	}

	@Test
	public void superdogsCanBrowseAllBackendLogs() {

		SpaceClient.prepareTest();

		// create test backends and users
		Backend test = SpaceClient.resetTestBackend();
		Backend test2 = SpaceClient.resetTest2Backend();

		SpaceClient.signUp(test, "vince", "hi vince");
		SpaceClient.signUp(test2, "fred", "hi fred");

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
		SpaceClient.deleteBackend(test);
		SpaceClient.deleteBackend(test2);

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
		SpaceClient.prepareTest();

		// superdog creates superadmin named nath in root 'api' backend
		User nath = new User("api", "nath", "hi nath", "platform@spacedog.io");
		nath.id = SpaceRequest.post("/1/credentials").superdogAuth()//
				.body("username", nath.username, "password", nath.password, //
						"email", nath.email, "level", "SUPER_ADMIN")
				.go(201).getString("id");

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
		SpaceRequest.delete("/1/credentials/" + nath.id).userAuth(nath).go(200);
	}

	@Test
	public void searchInLogs() {

		// prepare
		SpaceClient.prepareTest();

		// creates test backend and user
		Backend test = SpaceClient.resetTestBackend();
		SpaceRequest.get("/1/data").backend(test).go(200);
		SpaceRequest.get("/1/data/user").backend(test).go(403);
		User vince = SpaceClient.signUp(test, "vince", "hi vince");
		SpaceRequest.get("/1/credentials/" + vince.id).userAuth(vince).go(200);

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
				.assertEquals("/1/credentials/" + vince.id, "results.0.path")//
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
				.assertEquals("/1/credentials/" + vince.id, "results.0.path")//
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
				.assertEquals("/1/credentials/" + vince.id, "results.4.path")//
				.assertEquals("/1/login", "results.5.path")//
				.assertEquals("/1/credentials", "results.6.path")//
				.assertEquals("/1/data/user", "results.7.path")//
				.assertEquals("/1/data", "results.8.path")//
				.assertEquals("/1/backend", "results.9.path")//
				.assertEquals("/1/backend", "results.10.path");
	}

	@Test
	public void pingRequestAreNotLogged() {

		SpaceClient.prepareTest();

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
