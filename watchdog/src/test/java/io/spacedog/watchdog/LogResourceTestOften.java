package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
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
		SpaceRequest.get("/1/log").refresh().size(8).adminAuth(test).go(200)//
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
				.assertEquals("/1/backend", "results.6.path")//
				.assertEquals("test", "results.6.credentials.backendId")//
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
				.assertEquals("/1/backend", "results.2.path")//
				.assertEquals("test2", "results.2.credentials.backendId")//
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

		SpaceRequest.get("/1/log").refresh().size(7).adminAuth(test).go(200)//
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
				.assertEquals("/1/backend", "results.5.path")//
				.assertEquals("test", "results.5.credentials.backendId")//
				.assertEquals("******", "results.5.jsonContent.password")//
				.assertEquals("DELETE", "results.6.method")//
				.assertEquals("/1/backend", "results.6.path")//
				.assertEquals("test", "results.6.credentials.backendId");
	}

	@Test
	public void testSpecialCases() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// fails because invalid body
		SpaceRequest.put("/1/schema/toto").adminAuth(test).body("XXX").go(400);

		// but logs the failed request without the json content
		SpaceRequest.get("/1/log").refresh().size(1).adminAuth(test).go(200)//
				.assertEquals("PUT", "results.0.method")//
				.assertEquals("/1/schema/toto", "results.0.path")//
				.assertEquals("test", "results.0.credentials.backendId")//
				.assertEquals(400, "results.0.status")//
				.assertNotPresent("results.0.jsonContent");

		// check that log response results are not logged
		SpaceRequest.get("/1/log").adminAuth(test).go(200);
		SpaceRequest.get("/1/log").refresh().size(1).adminAuth(test).go(200)//
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

		SpaceRequest.get("/1/log").refresh().size(1).adminAuth(test).go(200)//
				.assertNotPresent("results.0.headers.x-empty")//
				.assertNotPresent("results.0.headers.x-blank")//
				.assertEquals("YELLOW", "results.0.headers.x-color")//
				.assertEquals("RED", "results.0.headers.x-color-list.0")//
				.assertEquals("BLUE", "results.0.headers.x-color-list.1")//
				.assertEquals("GREEN", "results.0.headers.x-color-list.2");
	}

}
