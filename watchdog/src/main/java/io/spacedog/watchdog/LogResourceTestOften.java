package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.model.Schema;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.DataObject;
import io.spacedog.sdk.SpaceDog;

public class LogResourceTestOften extends SpaceTest {

	@Test
	public void doAFewThingsAndGetTheLogs() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog test2 = resetTest2Backend();

		// create message schema in test backend
		test.schema().set(Schema.builder("message").text("text").build());

		// create user in test backend
		SpaceDog user = SpaceDog.backend("test").username("user").signUp("hi user");

		// create a user in test2 backend
		SpaceDog.backend("test2").username("user2").signUp("hi user2");

		// create message in test backend
		DataObject message = user.dataEndpoint().object("message")//
				.node("text", "What's up boys?").save();

		// find message by id in test backend
		message.fetch();

		// find all messages in test backend
		user.dataEndpoint().getAllRequest().type("message").load(DataObject.class);

		// get all test backend logs
		SpaceRequest.get("/1/log").refresh().size(8).auth(test).go(200)//
				.assertSizeEquals(8, "results")//
				.assertEquals("GET", "results.0.method")//
				.assertEquals("/1/data/message", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/1/data/message/" + message.id(), "results.1.path")//
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
		SpaceRequest.get("/1/log").size(4).auth(test2).go(200)//
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
		test.backend().delete();
		SpaceRequest.get("/1/log").auth(test).go(401);

		test2.backend().delete();
		SpaceRequest.get("/1/log").auth(test2).go(401);
	}

	@Test
	public void checkPasswordsAreNotLogged() {

		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = SpaceDog.backend("test").username("fred").signUp("hi fred");

		String passwordResetCode = SpaceRequest.delete("/1/credentials/{id}/password")//
				.routeParam("id", fred.id()).adminAuth(test).go(200)//
				.getString("passwordResetCode");

		SpaceRequest.post("/1/credentials/{id}/password")//
				.routeParam("id", fred.id())//
				.queryParam("passwordResetCode", passwordResetCode)//
				.backend(test).formField("password", "hi fred 2").go(200);

		SpaceRequest.put("/1/credentials/{id}/password").backend(test)//
				.routeParam("id", fred.id())//
				.basicAuth(test.backendId(), "fred", "hi fred 2")//
				.formField("password", "hi fred 3").go(200);

		SpaceRequest.get("/1/log").refresh().size(7).adminAuth(test).go(200)//
				.assertSizeEquals(7, "results")//
				.assertEquals("PUT", "results.0.method")//
				.assertEquals("/1/credentials/" + fred.id() + "/password", "results.0.path")//
				.assertEquals("******", "results.0.query.password")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/1/credentials/" + fred.id() + "/password", "results.1.path")//
				.assertEquals("******", "results.1.query.password")//
				.assertEquals(passwordResetCode, "results.1.query.passwordResetCode")//
				.assertEquals("DELETE", "results.2.method")//
				.assertEquals("/1/credentials/" + fred.id() + "/password", "results.2.path")//
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
		prepareTest();
		SpaceDog test = resetTestBackend();

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
