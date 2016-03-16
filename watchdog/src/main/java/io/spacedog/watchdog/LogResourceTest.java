package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Backend;
import io.spacedog.client.SpaceDogHelper.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder2;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class LogResourceTest extends Assert {

	@Test
	public void doAFewThingsAndGetTheLogs() throws Exception {

		SpaceDogHelper.prepareTest();

		// create a test account
		Backend testBackend = SpaceDogHelper.resetTestBackend();

		// create test2 account
		Backend test2Backend = SpaceDogHelper.resetBackend("test2", "test2", "hi test2");

		// create message schema in test backend
		SpaceDogHelper.setSchema(//
				SchemaBuilder2.builder("message")//
						.textProperty("text", "english", true).build(),
				testBackend);

		// create a user in test2 backend
		SpaceDogHelper.initUserDefaultSchema(test2Backend);
		SpaceDogHelper.createUser(test2Backend, "fred", "hi fred", "fred@dog.com");

		// create message in test backend
		String id = SpaceRequest.post("/1/data/message")//
				.backend(testBackend)//
				.body(Json.objectBuilder().put("text", "What's up boys?").toString())//
				.go(201)//
				.getFromJson("id")//
				.asText();

		// find message by id in test backend
		SpaceRequest.get("/1/data/message/" + id).backend(testBackend).go(200);

		// create user in test backend
		SpaceDogHelper.initUserDefaultSchema(testBackend);
		User vince = SpaceDogHelper.createUser(testBackend, "vince", "hi vince", "vince@dog.com");

		// find all messages in test backend
		SpaceRequest.get("/1/data/message").backend(testBackend).userAuth(vince).go(200);

		// get all test account logs
		SpaceRequest.get("/1/log?size=7").adminAuth(testBackend).go(200)//
				.assertSizeEquals(7, "results")//
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
				.assertEquals("/1/backend/test", "results.6.path");
				// don't check the delete account request before post account
				// because it sometimes fails normally with 401 (not authorized)
				// and 401 requests are not associated with any backend

		// get all test2 account logs
		SpaceRequest.get("/1/log?size=3").adminAuth(test2Backend).go(200)//
				.assertSizeEquals(3, "results")//
				.assertEquals("POST", "results.0.method")//
				.assertEquals("/1/user", "results.0.path")//
				.assertEquals("******", "results.0.jsonContent.password")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/1/schema/user", "results.1.path")//
				.assertEquals("POST", "results.2.method")//
				.assertEquals("/1/backend/test2", "results.2.path")//
				.assertEquals("******", "results.2.jsonContent.password");
				// don't check the delete account request before post account
				// because it sometimes fails normally with 401 (not authorized)
				// and 401 requests are not associated with any backend

		// after account deletion, logs are not accessible to account
		SpaceDogHelper.deleteBackend(testBackend);
		SpaceRequest.get("/1/log").adminAuth(testBackend).go(401);

		SpaceDogHelper.deleteBackend(test2Backend);
		SpaceRequest.get("/1/log").adminAuth(test2Backend).go(401);
	}

	@Test
	public void checkPasswordsAreNotLogged() throws Exception {

		SpaceDogHelper.prepareTest();
		Backend testBackend = SpaceDogHelper.resetTestBackend();
		SpaceDogHelper.initUserDefaultSchema(testBackend);
		SpaceDogHelper.createUser(testBackend, "fred", "hi fred", "fred@dog.com");

		String passwordResetCode = SpaceRequest.delete("/1/user/fred/password")//
				.adminAuth(testBackend).go(200).getFromJson("passwordResetCode").asText();

		SpaceRequest.post("/1/user/fred/password?passwordResetCode=" + passwordResetCode)//
				.backend(testBackend).field("password", "hi fred 2").go(200);

		SpaceRequest.put("/1/user/fred/password").backend(testBackend)//
				.basicAuth(testBackend.backendId, "fred", "hi fred 2")//
				.field("password", "hi fred 3").go(200);

		SpaceRequest.get("/1/log?size=6").adminAuth(testBackend).go(200)//
				.assertSizeEquals(6, "results")//
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
				.assertEquals("******", "results.5.jsonContent.password");
	}

	@Test
	public void deleteObsoleteLogs() throws Exception {

		// prepare
		SpaceDogHelper.prepareTest();
		Backend testBackend = SpaceDogHelper.resetTestBackend();
		SpaceDogHelper.initUserDefaultSchema(testBackend);
		SpaceDogHelper.createUser(testBackend, "fred", "hi fred", "fred@dog.com");
		for (int i = 0; i < 5; i++)
			SpaceRequest.get("/1/data").adminAuth(testBackend).go(200);

		// check everything is in place
		SpaceRequest.get("/1/log?size=8").adminAuth(testBackend).go(200)//
				.assertEquals("/1/data", "results.0.path")//
				.assertEquals("/1/data", "results.1.path")//
				.assertEquals("/1/data", "results.2.path")//
				.assertEquals("/1/data", "results.3.path")//
				.assertEquals("/1/data", "results.4.path")//
				.assertEquals("/1/user", "results.5.path")//
				.assertEquals("/1/schema/user", "results.6.path")//
				.assertEquals("/1/backend/test", "results.7.path");

		// delete all logs but the 2 last requests
		SpaceRequest.delete("/1/log?from=2").superdogAuth(testBackend).go(200);

		// check all test account logs are deleted but ...
		SpaceRequest.get("/1/log?size=10").adminAuth(testBackend).go(200)//
				.assertSizeEquals(3, "results")//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/1/log", "results.1.path")//
				.assertEquals("GET", "results.2.method")//
				.assertEquals("/1/data", "results.2.path");

	}

	@Test
	public void superdogsCanBrowseAllAccountLogs() throws Exception {

		SpaceDogHelper.prepareTest();

		// create test accounts and users
		Backend testBackend = SpaceDogHelper.resetTestBackend();
		SpaceDogHelper.initUserDefaultSchema(testBackend);
		SpaceDogHelper.createUser(testBackend, "vince", "hi vince", "vince@dog.com");
		Backend test2Backend = SpaceDogHelper.resetBackend("test2", "test2", "hi test2", "test2@dog.com", true);
		SpaceDogHelper.initUserDefaultSchema(test2Backend);
		SpaceDogHelper.createUser(test2Backend, "fred", "hi fred", "fred@dog.com");

		// get all test account logs
		SpaceRequest.get("/1/log?size=3")//
				.superdogAuth(testBackend)//
				.go(200)//
				.assertSizeEquals(3, "results")//
				.assertEquals("POST", "results.0.method")//
				.assertEquals("/1/user", "results.0.path")//
				.assertEquals("vince", "results.0.response.id")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/1/schema/user", "results.1.path")//
				.assertEquals("POST", "results.2.method")//
				.assertEquals("/1/backend/test", "results.2.path")//
				.assertEquals("test", "results.2.response.id");

		// get all test2 account logs
		SpaceRequest.get("/1/log?size=3")//
				.superdogAuth("test2")//
				.go(200)//
				.assertSizeEquals(3, "results")//
				.assertEquals("POST", "results.0.method")//
				.assertEquals("/1/user", "results.0.path")//
				.assertEquals("fred", "results.0.response.id")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/1/schema/user", "results.1.path")//
				.assertEquals("POST", "results.2.method")//
				.assertEquals("/1/backend/test2", "results.2.path")//
				.assertEquals("test2", "results.2.response.id");

		// get all accounts logs
		SpaceRequest.get("/1/log?size=10")//
				.superdogAuth()//
				.go(200)//
				.assertSizeEquals(10, "results")//
				.assertEquals("GET", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/1/log", "results.1.path")//
				.assertEquals("POST", "results.2.method")//
				.assertEquals("/1/user", "results.2.path")//
				.assertEquals("fred", "results.2.response.id")//
				.assertEquals("POST", "results.3.method")//
				.assertEquals("/1/schema/user", "results.3.path")//
				.assertEquals("POST", "results.4.method")//
				.assertEquals("/1/backend/test2", "results.4.path")//
				.assertEquals("test2", "results.4.response.id")//
				.assertEquals("DELETE", "results.5.method")//
				.assertEquals("/1/backend", "results.5.path")//
				.assertEquals("POST", "results.6.method")//
				.assertEquals("/1/user", "results.6.path")//
				.assertEquals("vince", "results.6.response.id")//
				.assertEquals("POST", "results.7.method")//
				.assertEquals("/1/schema/user", "results.7.path")//
				.assertEquals("POST", "results.8.method")//
				.assertEquals("/1/backend/test", "results.8.path")//
				.assertEquals("test", "results.8.response.id")//
				.assertEquals("DELETE", "results.9.method")//
				.assertEquals("/1/backend", "results.9.path");

		// after account deletion, logs are still accessible to superdogs
		SpaceDogHelper.deleteBackend(testBackend);
		SpaceDogHelper.deleteBackend(test2Backend);
		SpaceRequest.get("/1/log?size=3")//
				.superdogAuth()//
				.go(200)//
				.assertSizeEquals(3, "results")//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/backend", "results.0.path")//
				.assertEquals("DELETE", "results.1.method")//
				.assertEquals("/1/backend", "results.1.path")//
				.assertEquals("GET", "results.2.method")//
				.assertEquals("/1/log", "results.2.path")//
				.assertEquals("10", "results.2.query.size");

		// use logType=ADMIN to filter and only get admin and lower logs
		SpaceRequest.get("/1/log?size=3&logType=SUPER_ADMIN")//
				.superdogAuth()//
				.go(200)//
				.assertSizeEquals(3, "results")//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/backend", "results.0.path")//
				.assertEquals("DELETE", "results.1.method")//
				.assertEquals("/1/backend", "results.1.path")//
				.assertEquals("POST", "results.2.method")//
				.assertEquals("/1/user", "results.2.path")//
				.assertEquals("fred", "results.2.response.id");

		// use logType=USER to filter and only get user and lower logs
		SpaceRequest.get("/1/log?size=2&logType=USER")//
				.superdogAuth()//
				.go(200)//
				.assertSizeEquals(2, "results")//
				.assertEquals("POST", "results.0.method")//
				.assertEquals("/1/user", "results.0.path")//
				.assertEquals("fred", "results.0.response.id")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/1/user", "results.1.path")//
				.assertEquals("vince", "results.1.response.id");

		// use logType=KEY to filter and only get backend key logs
		SpaceRequest.get("/1/log?size=2&logType=KEY")//
				.superdogAuth()//
				.go(200)//
				.assertSizeEquals(2, "results")//
				.assertEquals("POST", "results.0.method")//
				.assertEquals("/1/user", "results.0.path")//
				.assertEquals("fred", "results.0.response.id")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/1/user", "results.1.path")//
				.assertEquals("vince", "results.1.response.id");
	}
}
