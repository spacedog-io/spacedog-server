package io.spacedog.watchdog;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder2;
import io.spacedog.utils.SchemaBuilder3;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class LogResourceTestOften extends Assert {

	@Test
	public void doAFewThingsAndGetTheLogs() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		Backend test2 = SpaceClient.resetBackend("test2", "test2", "hi test2");

		// create message schema in test backend
		SpaceClient.setSchema(//
				SchemaBuilder2.builder("message")//
						.textProperty("text", "english", true).build(),
				test);

		// create a user in test2 backend
		SpaceClient.initUserDefaultSchema(test2);
		SpaceClient.createUser(test2, "fred", "hi fred");

		// create message in test backend
		String id = SpaceRequest.post("/1/data/message")//
				.backend(test)//
				.body("text", "What's up boys?")//
				.go(201)//
				.getFromJson("id")//
				.asText();

		// find message by id in test backend
		SpaceRequest.get("/1/data/message/" + id).backend(test).go(200);

		// create user in test backend
		SpaceClient.initUserDefaultSchema(test);
		User vince = SpaceClient.createUser(test, "vince", "hi vince");

		// find all messages in test backend
		SpaceRequest.get("/1/data/message").userAuth(vince).go(200);

		// get all test backend logs
		SpaceRequest.get("/1/log").size(8).adminAuth(test).go(200)//
				.assertSizeEquals(8, "results")//
				.assertEquals("GET", "results.0.method")//
				.assertEquals("/1/data/message", "results.0.path")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/1/user", "results.1.path")//
				.assertEquals("POST", "results.2.method")//
				.assertEquals("/1/schema/user", "results.2.path")//
				.assertEquals("GET", "results.3.method")//
				.assertEquals("/1/data/message/" + id, "results.3.path")//
				.assertEquals("POST", "results.4.method")//
				.assertEquals("/1/data/message", "results.4.path")//
				.assertEquals("POST", "results.5.method")//
				.assertEquals("/1/schema/message", "results.5.path")//
				.assertEquals("POST", "results.6.method")//
				.assertEquals("/1/backend/test", "results.6.path")//
				.assertEquals("DELETE", "results.7.method")//
				.assertEquals("/1/backend", "results.7.path")//
				.assertEquals("test", "results.7.credentials.backendId");

		// get all test2 backend logs
		SpaceRequest.get("/1/log").size(4).adminAuth(test2).go(200)//
				.assertSizeEquals(4, "results")//
				.assertEquals("POST", "results.0.method")//
				.assertEquals("/1/user", "results.0.path")//
				.assertEquals("******", "results.0.jsonContent.password")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/1/schema/user", "results.1.path")//
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
	public void checkPasswordsAreNotLogged() throws Exception {

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		SpaceClient.initUserDefaultSchema(test);
		SpaceClient.createUser(test, "fred", "hi fred");

		String passwordResetCode = SpaceRequest.delete("/1/user/fred/password")//
				.adminAuth(test).go(200).getFromJson("passwordResetCode").asText();

		SpaceRequest.post("/1/user/fred/password?passwordResetCode=" + passwordResetCode)//
				.backend(test).field("password", "hi fred 2").go(200);

		SpaceRequest.put("/1/user/fred/password").backend(test)//
				.basicAuth(test.backendId, "fred", "hi fred 2")//
				.field("password", "hi fred 3").go(200);

		SpaceRequest.get("/1/log").size(7).adminAuth(test).go(200)//
				.assertSizeEquals(7, "results")//
				.assertEquals("PUT", "results.0.method")//
				.assertEquals("/1/user/fred/password", "results.0.path")//
				.assertEquals("******", "results.0.query.password")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/1/user/fred/password", "results.1.path")//
				.assertEquals("******", "results.1.query.password")//
				.assertEquals(passwordResetCode, "results.1.query.passwordResetCode")//
				.assertEquals("DELETE", "results.2.method")//
				.assertEquals("/1/user/fred/password", "results.2.path")//
				.assertEquals(passwordResetCode, "results.2.response.passwordResetCode")//
				.assertEquals("POST", "results.3.method")//
				.assertEquals("/1/user", "results.3.path")//
				.assertEquals("******", "results.3.jsonContent.password")//
				.assertEquals("POST", "results.4.method")//
				.assertEquals("/1/schema/user", "results.4.path")//
				.assertEquals("POST", "results.5.method")//
				.assertEquals("/1/backend/test", "results.5.path")//
				.assertEquals("******", "results.5.jsonContent.password")//
				.assertEquals("DELETE", "results.6.method")//
				.assertEquals("/1/backend", "results.6.path")//
				.assertEquals("test", "results.6.credentials.backendId");
	}

	@Test
	public void checkLogFilterDoesNotRemoveAnyPasswordSchemaProperty() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// custom schema with password property is valid
		ObjectNode schema = SchemaBuilder3.builder("credentials").string("password").build();
		SpaceClient.setSchema(schema, test);

		// schema password properties are not scrambled
		SpaceRequest.get("/1/schema/credentials").backend(test).go(200)//
				.assertEquals("string", "credentials.password._type");

		// log password filter only scramble text fields
		SpaceRequest.get("/1/log").size(2).adminAuth(test).go(200)//
				.assertEquals("string", "results.0.response.credentials.password._type")//
				.assertEquals("string", "results.1.jsonContent.credentials.password._type");
	}

	@Test
	public void deleteObsoleteLogs() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		SpaceClient.initUserDefaultSchema(test);
		SpaceClient.createUser(test, "fred", "hi fred");

		for (int i = 0; i < 5; i++)
			SpaceRequest.get("/1/data").adminAuth(test).go(200);

		// check everything is in place
		SpaceRequest.get("/1/log").size(9).adminAuth(test).go(200)//
				.assertEquals("/1/data", "results.0.path")//
				.assertEquals("/1/data", "results.1.path")//
				.assertEquals("/1/data", "results.2.path")//
				.assertEquals("/1/data", "results.3.path")//
				.assertEquals("/1/data", "results.4.path")//
				.assertEquals("/1/user", "results.5.path")//
				.assertEquals("/1/schema/user", "results.6.path")//
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
	public void superdogsCanBrowseAllBackendLogs() throws Exception {

		SpaceClient.prepareTest();

		// create test backends and users
		Backend test = SpaceClient.resetTestBackend();
		Backend test2 = SpaceClient.resetBackend("test2", "test2", "hi test2");

		SpaceClient.initUserDefaultSchema(test);
		SpaceClient.initUserDefaultSchema(test2);

		SpaceClient.createUser(test, "vince", "hi vince");
		SpaceClient.createUser(test2, "fred", "hi fred");

		// superdog gets all backends logs
		SpaceResponse response = SpaceRequest.get("/1/log").size(20).superdogAuth().go(200);
		removeOtherThenTestLogs(response);
		response.assertEquals("/1/user", "results.0.path")//
				.assertEquals("test2", "results.0.credentials.backendId")//
				.assertEquals("/1/user", "results.1.path")//
				.assertEquals("test", "results.1.credentials.backendId")//
				.assertEquals("/1/schema/user", "results.2.path")//
				.assertEquals("test2", "results.2.credentials.backendId")//
				.assertEquals("/1/schema/user", "results.3.path")//
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

		response = SpaceRequest.get("/1/log").size(10).superdogAuth().go(200);
		removeOtherThenTestLogs(response);
		response.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/backend", "results.0.path")//
				.assertEquals("test2", "results.0.credentials.backendId")//
				.assertEquals("DELETE", "results.1.method")//
				.assertEquals("/1/backend", "results.1.path")//
				.assertEquals("test", "results.1.credentials.backendId")//
				.assertEquals("POST", "results.2.method")//
				.assertEquals("/1/user", "results.2.path")//
				.assertEquals("test2", "results.2.credentials.backendId")//
				.assertEquals("POST", "results.3.method")//
				.assertEquals("/1/user", "results.3.path")//
				.assertEquals("test", "results.3.credentials.backendId");
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
	public void filterLogs() throws Exception {

		SpaceClient.prepareTest();

		// creates test backend and user
		Backend test = SpaceClient.resetTestBackend();
		SpaceRequest.get("/1/data").backend(test).go(200);
		SpaceRequest.get("/1/data/user").backend(test).go(401);
		SpaceClient.initUserDefaultSchema(test);
		User vince = SpaceClient.createUser(test, "vince", "hi vince");
		SpaceRequest.get("/1/user").userAuth(vince).go(200);
		SpaceRequest.get("/1/user/vince").userAuth(vince).go(200);

		// superdog filters test backend log to only get status 400 and higher
		// logs
		SpaceRequest.get("/1/log?minStatus=400").size(1).superdogAuth(test).go(200)//
				.assertEquals("/1/data/user", "results.0.path")//
				.assertEquals(401, "results.0.status");

		// superdog filters test backend logs to only get SUPER_ADMIN and lower
		// logs
		SpaceRequest.get("/1/log?logType=SUPER_ADMIN").size(8).superdogAuth(test).go(200)//
				.assertEquals("/1/user/vince", "results.0.path")//
				.assertEquals("/1/user", "results.1.path")//
				.assertEquals("/1/user", "results.2.path")//
				.assertEquals("/1/schema/user", "results.3.path")//
				.assertEquals("/1/data/user", "results.4.path")//
				.assertEquals("/1/data", "results.5.path")//
				.assertEquals("/1/backend/test", "results.6.path")//
				.assertEquals("/1/backend", "results.7.path");

		// superdog filters test backend log to only get USER and lower logs
		SpaceRequest.get("/1/log?logType=USER").size(5).superdogAuth(test).go(200)//
				.assertEquals("/1/user/vince", "results.0.path")//
				.assertEquals("/1/user", "results.1.path")//
				.assertEquals("/1/user", "results.2.path")//
				.assertEquals("/1/data/user", "results.3.path")//
				.assertEquals("/1/data", "results.4.path");

		// superdog filters test backend log to only get KEY and lower logs
		SpaceRequest.get("/1/log?logType=KEY").size(3).superdogAuth(test).go(200)//
				.assertEquals("/1/user", "results.0.path")//
				.assertEquals("/1/data/user", "results.1.path")//
				.assertEquals("/1/data", "results.2.path");

		// superdog gets all test backend logs
		SpaceRequest.get("/1/log").size(12).superdogAuth(test).go(200)//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("KEY", "results.0.query.logType")//
				.assertEquals("/1/log", "results.1.path")//
				.assertEquals("USER", "results.1.query.logType")//
				.assertEquals("/1/log", "results.2.path")//
				.assertEquals("SUPER_ADMIN", "results.2.query.logType")//
				.assertEquals("/1/log", "results.3.path")//
				.assertEquals("400", "results.3.query.minStatus")//
				.assertEquals("/1/user/vince", "results.4.path")//
				.assertEquals("/1/user", "results.5.path")//
				.assertEquals("/1/user", "results.6.path")//
				.assertEquals("/1/schema/user", "results.7.path")//
				.assertEquals("/1/data/user", "results.8.path")//
				.assertEquals("/1/data", "results.9.path")//
				.assertEquals("/1/backend/test", "results.10.path")//
				.assertEquals("/1/backend", "results.11.path");
	}

	@Test
	public void pingRequestAreNotLogged() throws Exception {

		SpaceClient.prepareTest();

		// load balancer pings a SpaceDog instance

		SpaceRequest.get("").go(200);

		// this ping should not be present in logs

		JsonNode results = SpaceRequest.get("/1/log")//
				.size(5).superdogAuth().go(200).getFromJson("results");

		Iterator<JsonNode> elements = results.elements();
		while (elements.hasNext()) {
			JsonNode element = elements.next();
			if (element.get("path").asText().equals("/")
					&& element.get("credentials").get("backendId").asText().equals(Backends.ROOT_API))
				Assert.fail();
		}
	}

	@Test
	public void testSpecialCases() throws Exception {

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

		// check that log responses are not logged
		SpaceRequest.get("/1/log").adminAuth(test).go(200);
		SpaceRequest.get("/1/log").size(1).adminAuth(test).go(200)//
				.assertEquals("GET", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertNotPresent("results.0.response");

		// Headers are logged
		SpaceRequest.get("/1/log").adminAuth(test)//
				.header("x-empty", "")//
				.header("x-color", "YELLOW")//
				.header("x-color-list", "RED,BLUE,GREEN")//
				.go(200);

		SpaceRequest.get("/1/log").size(1).adminAuth(test).go(200)//
				.assertNotPresent("results.0.headers.x-empty")//
				.assertEquals("YELLOW", "results.0.headers.x-color")//
				.assertEquals("RED", "results.0.headers.x-color-list.0")//
				.assertEquals("BLUE", "results.0.headers.x-color-list.1")//
				.assertEquals("GREEN", "results.0.headers.x-color-list.2");
	}
}
