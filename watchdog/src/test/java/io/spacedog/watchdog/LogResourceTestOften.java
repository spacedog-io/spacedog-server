package io.spacedog.watchdog;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Json;
import io.spacedog.utils.Schema;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class LogResourceTestOften extends Assert {

	@Test
	public void doAFewThingsAndGetTheLogs() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		Backend test2 = SpaceClient.resetBackend("test2", "test2", "hi test2");

		// create message schema in test backend
		SpaceClient.setSchema(//
				Schema.builder("message").text("text").build(), //
				test);

		// create user in test backend
		User user = SpaceClient.signUp(test, "user", "hi user");

		// create a user in test2 backend
		SpaceClient.signUp(test2, "user2", "hi user2");

		// create message in test backend
		String id = SpaceRequest.post("/1/data/message")//
				.userAuth(user)//
				.body("text", "What's up boys?")//
				.go(201)//
				.getString("id");

		// find message by id in test backend
		SpaceRequest.get("/1/data/message/" + id).userAuth(user).go(200);

		// find all messages in test backend
		SpaceRequest.get("/1/data/message").userAuth(user).go(200);

		// get all test backend logs
		SpaceRequest.get("/1/log").size(8).adminAuth(test).go(200)//
				.assertSizeEquals(8, "results")//
				.assertEquals("GET", "results.0.method")//
				.assertEquals("/1/data/message", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/1/data/message/" + id, "results.1.path")//
				.assertEquals("POST", "results.2.method")//
				.assertEquals("/1/data/message", "results.2.path")//
				.assertEquals("GET", "results.3.method")//
				.assertEquals("/1/login", "results.3.path")//
				.assertEquals("POST", "results.4.method")//
				.assertEquals("/1/credentials", "results.4.path")//
				.assertEquals("PUT", "results.5.method")//
				.assertEquals("/1/schema/message", "results.5.path")//
				.assertEquals("POST", "results.6.method")//
				.assertEquals("/1/backend/test", "results.6.path")//
				.assertEquals("DELETE", "results.7.method")//
				.assertEquals("/1/backend", "results.7.path")//
				.assertEquals("test", "results.7.credentials.backendId");

		// get all test2 backend logs
		SpaceRequest.get("/1/log").size(4).adminAuth(test2).go(200)//
				.assertSizeEquals(4, "results")//
				.assertEquals("GET", "results.0.method")//
				.assertEquals("/1/login", "results.0.path")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/1/credentials", "results.1.path")//
				.assertEquals("******", "results.1.jsonContent.password")//
				.assertEquals("POST", "results.2.method")//
				.assertEquals("/1/backend/test2", "results.2.path")//
				.assertEquals("******", "results.2.jsonContent.password")//
				.assertEquals("DELETE", "results.3.method")//
				.assertEquals("/1/backend", "results.3.path")//
				.assertEquals("test2", "results.3.credentials.backendId");

		// after backend deletion, logs are not accessible to backend
		SpaceClient.deleteBackend(test);
		SpaceRequest.get("/1/log").adminAuth(test).go(401);

		SpaceClient.deleteBackend(test2);
		SpaceRequest.get("/1/log").adminAuth(test2).go(401);
	}

	@Test
	public void checkPasswordsAreNotLogged() {

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.signUp(test, "fred", "hi fred");

		String passwordResetCode = SpaceRequest.delete("/1/credentials/{id}/password")//
				.routeParam("id", fred.id).adminAuth(test).go(200)//
				.getString("passwordResetCode");

		SpaceRequest.post("/1/credentials/{id}/password")//
				.routeParam("id", fred.id)//
				.queryParam("passwordResetCode", passwordResetCode)//
				.backend(test).formField("password", "hi fred 2").go(200);

		SpaceRequest.put("/1/credentials/{id}/password").backend(test)//
				.routeParam("id", fred.id)//
				.basicAuth(test.backendId, "fred", "hi fred 2")//
				.formField("password", "hi fred 3").go(200);

		SpaceRequest.get("/1/log").size(7).adminAuth(test).go(200)//
				.assertSizeEquals(7, "results")//
				.assertEquals("PUT", "results.0.method")//
				.assertEquals("/1/credentials/" + fred.id + "/password", "results.0.path")//
				.assertEquals("******", "results.0.query.password")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/1/credentials/" + fred.id + "/password", "results.1.path")//
				.assertEquals("******", "results.1.query.password")//
				.assertEquals(passwordResetCode, "results.1.query.passwordResetCode")//
				.assertEquals("DELETE", "results.2.method")//
				.assertEquals("/1/credentials/" + fred.id + "/password", "results.2.path")//
				.assertEquals(passwordResetCode, "results.2.response.passwordResetCode")//
				.assertEquals("GET", "results.3.method")//
				.assertEquals("/1/login", "results.3.path")//
				.assertEquals("POST", "results.4.method")//
				.assertEquals("/1/credentials", "results.4.path")//
				.assertEquals("******", "results.4.jsonContent.password")//
				.assertEquals("POST", "results.5.method")//
				.assertEquals("/1/backend/test", "results.5.path")//
				.assertEquals("******", "results.5.jsonContent.password")//
				.assertEquals("DELETE", "results.6.method")//
				.assertEquals("/1/backend", "results.6.path")//
				.assertEquals("test", "results.6.credentials.backendId");
	}

	@Test
	public void deleteObsoleteLogs() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		SpaceClient.signUp(test, "fred", "hi fred");

		for (int i = 0; i < 5; i++)
			SpaceRequest.get("/1/data").adminAuth(test).go(200);

		// check everything is in place
		SpaceRequest.get("/1/log").size(9).adminAuth(test).go(200)//
				.assertEquals("/1/data", "results.0.path")//
				.assertEquals("/1/data", "results.1.path")//
				.assertEquals("/1/data", "results.2.path")//
				.assertEquals("/1/data", "results.3.path")//
				.assertEquals("/1/data", "results.4.path")//
				.assertEquals("/1/login", "results.5.path")//
				.assertEquals("/1/credentials", "results.6.path")//
				.assertEquals("/1/backend/test", "results.7.path")//
				.assertEquals("/1/backend", "results.8.path");

		// delete all logs but the 2 last requests
		SpaceRequest.delete("/1/log").from(2).superdogAuth(test).go(200);

		// check all test backend logs are deleted but ...
		SpaceRequest.get("/1/log").size(10).adminAuth(test).go(200)//
				.assertSizeEquals(3, "results")//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/1/log", "results.1.path")//
				.assertEquals("GET", "results.2.method")//
				.assertEquals("/1/data", "results.2.path");

	}

	@Test
	public void superdogsCanBrowseAllBackendLogs() {

		SpaceClient.prepareTest();

		// create test backends and users
		Backend test = SpaceClient.resetTestBackend();
		Backend test2 = SpaceClient.resetBackend("test2", "test2", "hi test2");

		SpaceClient.signUp(test, "vince", "hi vince");
		SpaceClient.signUp(test2, "fred", "hi fred");

		// superdog gets all backends logs
		SpaceResponse response = SpaceRequest.get("/1/log").size(20).superdogAuth().go(200);
		removeOtherThenTestLogs(response);
		response.assertEquals("/1/login", "results.0.path")//
				.assertEquals("test2", "results.0.credentials.backendId")//
				.assertEquals("/1/credentials", "results.1.path")//
				.assertEquals("test2", "results.1.credentials.backendId")//
				.assertEquals("/1/login", "results.2.path")//
				.assertEquals("test", "results.2.credentials.backendId")//
				.assertEquals("/1/credentials", "results.3.path")//
				.assertEquals("test", "results.3.credentials.backendId")//
				.assertEquals("/1/backend/test2", "results.4.path")//
				.assertEquals("/1/backend", "results.5.path")//
				.assertEquals("test2", "results.5.credentials.backendId")//
				.assertEquals("/1/backend/test", "results.6.path")//
				.assertEquals("/1/backend", "results.7.path")//
				.assertEquals("test", "results.7.credentials.backendId");

		// after backend deletion, logs are still accessible to superdogs
		SpaceClient.deleteBackend(test);
		SpaceClient.deleteBackend(test2);

		response = SpaceRequest.get("/1/log").size(15).superdogAuth().go(200);
		removeOtherThenTestLogs(response);
		response.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/backend", "results.0.path")//
				.assertEquals("test2", "results.0.credentials.backendId")//
				.assertEquals("DELETE", "results.1.method")//
				.assertEquals("/1/backend", "results.1.path")//
				.assertEquals("test", "results.1.credentials.backendId")//
				.assertEquals("GET", "results.2.method")//
				.assertEquals("/1/login", "results.2.path")//
				.assertEquals("test2", "results.2.credentials.backendId")//
				.assertEquals("POST", "results.3.method")//
				.assertEquals("/1/credentials", "results.3.path")//
				.assertEquals("test2", "results.3.credentials.backendId")//
				.assertEquals("GET", "results.4.method")//
				.assertEquals("/1/login", "results.4.path")//
				.assertEquals("test", "results.4.credentials.backendId")//
				.assertEquals("POST", "results.5.method")//
				.assertEquals("/1/credentials", "results.5.path")//
				.assertEquals("test", "results.5.credentials.backendId");
	}

	/**
	 * Removes all logs not associated with the test or test2 backend to avoid
	 * log noise from production testing.
	 */
	private void removeOtherThenTestLogs(SpaceResponse response) {
		Iterator<JsonNode> logs = response.objectNode().get("results").elements();
		while (logs.hasNext()) {
			JsonNode log = logs.next();
			JsonNode backendId = Json.get(log, "credentials.backendId");
			if (backendId == null || //
					(!backendId.asText().equals("test")//
							&& !backendId.asText().equals("test2"))) {
				logs.remove();
			}
		}
	}

	@Test
	public void filterLogs() {

		// prepare
		SpaceClient.prepareTest();

		// creates test backend and user
		Backend test = SpaceClient.resetTestBackend();
		SpaceRequest.get("/1/data").backend(test).go(200);
		SpaceRequest.get("/1/data/user").backend(test).go(403);
		User vince = SpaceClient.signUp(test, "vince", "hi vince");
		SpaceRequest.get("/1/credentials/" + vince.id).userAuth(vince).go(200);

		// superdog filters test backend log to only get status 400 and higher
		// logs
		SpaceRequest.get("/1/log?minStatus=400").size(1).superdogAuth(test).go(200)//
				.assertEquals("/1/data/user", "results.0.path")//
				.assertEquals(403, "results.0.status");

		// superdog filters test backend logs to only get SUPER_ADMIN and lower
		// logs
		SpaceRequest.get("/1/log?logType=SUPER_ADMIN").size(7).superdogAuth(test).go(200)//
				.assertEquals("/1/credentials/" + vince.id, "results.0.path")//
				.assertEquals("/1/login", "results.1.path")//
				.assertEquals("/1/credentials", "results.2.path")//
				.assertEquals("/1/data/user", "results.3.path")//
				.assertEquals("/1/data", "results.4.path")//
				.assertEquals("/1/backend/test", "results.5.path")//
				.assertEquals("/1/backend", "results.6.path");

		// superdog filters test backend log to only get USER and lower logs
		SpaceRequest.get("/1/log?logType=USER").size(7).superdogAuth(test).go(200)//
				.assertEquals("/1/credentials/" + vince.id, "results.0.path")//
				.assertEquals("/1/login", "results.1.path")//
				.assertEquals("/1/credentials", "results.2.path")//
				.assertEquals("/1/data/user", "results.3.path")//
				.assertEquals("/1/data", "results.4.path");

		// superdog filters test backend log to only get KEY and lower logs
		SpaceRequest.get("/1/log?logType=KEY").size(3).superdogAuth(test).go(200)//
				.assertEquals("/1/credentials", "results.0.path")//
				.assertEquals("/1/data/user", "results.1.path")//
				.assertEquals("/1/data", "results.2.path");

		// superdog gets all test backend logs
		SpaceRequest.get("/1/log").size(15).superdogAuth(test).go(200)//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("KEY", "results.0.query.logType")//
				.assertEquals("/1/log", "results.1.path")//
				.assertEquals("USER", "results.1.query.logType")//
				.assertEquals("/1/log", "results.2.path")//
				.assertEquals("SUPER_ADMIN", "results.2.query.logType")//
				.assertEquals("/1/log", "results.3.path")//
				.assertEquals("400", "results.3.query.minStatus")//
				.assertEquals("/1/credentials/" + vince.id, "results.4.path")//
				.assertEquals("/1/login", "results.5.path")//
				.assertEquals("/1/credentials", "results.6.path")//
				.assertEquals("/1/data/user", "results.7.path")//
				.assertEquals("/1/data", "results.8.path")//
				.assertEquals("/1/backend/test", "results.9.path")//
				.assertEquals("/1/backend", "results.10.path");
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
		SpaceRequest.post("/1/log/search").size(1).superdogAuth(test)//
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
				.assertEquals("/1/backend/test", "results.5.path")//
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
		SpaceRequest.post("/1/log/search").size(15).superdogAuth(test)//
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
				.assertEquals("/1/backend/test", "results.9.path")//
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
					&& element.get("credentials").get("backendId").asText().equals(Backends.ROOT_API))
				Assert.fail();
		}
	}

	@Test
	public void testSpecialCases() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// fails to search because invalid body
		SpaceRequest.put("/1/schema/toto").adminAuth(test).body("XXX").go(400);

		// but logs the failed request without the json content
		SpaceRequest.get("/1/log").size(1).adminAuth(test).go(200)//
				.assertEquals("PUT", "results.0.method")//
				.assertEquals("/1/schema/toto", "results.0.path")//
				.assertEquals("test", "results.0.credentials.backendId")//
				.assertEquals(400, "results.0.status")//
				.assertNotPresent("results.0.jsonContent");

		// check that log response results are not logged
		SpaceRequest.get("/1/log").adminAuth(test).go(200);
		SpaceRequest.get("/1/log").size(1).adminAuth(test).go(200)//
				.assertEquals("GET", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertNotPresent("results.0.response.results");

		// Headers are logged if not empty
		SpaceRequest.get("/1/log").adminAuth(test)//
				.header("x-empty", "")//
				.header("x-blank", " ")//
				.header("x-color", "YELLOW")//
				.header("x-color-list", "RED,BLUE,GREEN")//
				.go(200);

		SpaceRequest.get("/1/log").size(1).adminAuth(test).go(200)//
				.assertNotPresent("results.0.headers.x-empty")//
				.assertNotPresent("results.0.headers.x-blank")//
				.assertEquals("YELLOW", "results.0.headers.x-color")//
				.assertEquals("RED", "results.0.headers.x-color-list.0")//
				.assertEquals("BLUE", "results.0.headers.x-color-list.1")//
				.assertEquals("GREEN", "results.0.headers.x-color-list.2");
	}
}
