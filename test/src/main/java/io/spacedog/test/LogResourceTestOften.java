package io.spacedog.test;

import org.junit.Test;

import io.spacedog.model.Schema;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.DataObject;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.SpaceHeaders;

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
		SpaceDog user = signUp("test", "user", "hi user");

		// create a user in test2 backend
		signUp("test2", "user2", "hi user2");

		// create message in test backend
		DataObject<?> message = user.data().object("message")//
				.node("text", "What's up boys?").save();

		// find message by id in test backend
		message.fetch();

		// find all messages in test backend
		user.data().getAll().type("message").get(DataObject.class);

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
		test.admin().deleteBackend(test.backendId());
		SpaceRequest.get("/1/log").auth(test).go(401);

		test2.admin().deleteBackend(test2.backendId());
		SpaceRequest.get("/1/log").auth(test2).go(401);
	}

	@Test
	public void checkPasswordsAreNotLogged() {

		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = signUp("test", "fred", "hi fred");

		String passwordResetCode = test.delete("/1/credentials/{id}/password")//
				.routeParam("id", fred.id()).go(200)//
				.getString("passwordResetCode");

		SpaceRequest.post("/1/credentials/{id}/password")//
				.routeParam("id", fred.id())//
				.queryParam("passwordResetCode", passwordResetCode)//
				.backend(test).formField("password", "hi fred 2").go(200);

		SpaceRequest.put("/1/credentials/{id}/password").backend(test)//
				.routeParam("id", fred.id()).basicAuth("fred", "hi fred 2")//
				.formField("password", "hi fred 3").go(200);

		test.get("/1/log").refresh().size(7).go(200)//
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
		SpaceRequest.put("/1/schema/toto").auth(test).bodyString("XXX").go(400);

		// but logs the failed request without the json content
		SpaceRequest.get("/1/log").refresh().size(1).auth(test).go(200)//
				.assertEquals("PUT", "results.0.method")//
				.assertEquals("/1/schema/toto", "results.0.path")//
				.assertEquals("test", "results.0.credentials.backendId")//
				.assertEquals(400, "results.0.status")//
				.assertNotPresent("results.0.jsonContent");

		// check that log response results are not logged
		SpaceRequest.get("/1/log").auth(test).go(200);
		SpaceRequest.get("/1/log").refresh().size(1).auth(test).go(200)//
				.assertEquals("GET", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertNotPresent("results.0.response.results");

		// Headers are logged if not empty
		// and 'Authorization' header is not logged
		SpaceRequest.get("/1/log").auth(test)//
				.setHeader("x-empty", "")//
				.setHeader("x-blank", " ")//
				.setHeader("x-color", "YELLOW")//
				.addHeader("x-color-list", "RED")//
				.addHeader("x-color-list", "BLUE")//
				.addHeader("x-color-list", "GREEN")//
				.go(200);

		SpaceRequest.get("/1/log").refresh().size(1).auth(test).go(200)//
				.assertNotPresent("results.0.headers." + SpaceHeaders.AUTHORIZATION)//
				.assertNotPresent("results.0.headers.x-empty")//
				.assertNotPresent("results.0.headers.x-blank")//
				.assertEquals("YELLOW", "results.0.headers.x-color")//
				.assertEquals("RED", "results.0.headers.x-color-list.0")//
				.assertEquals("BLUE", "results.0.headers.x-color-list.1")//
				.assertEquals("GREEN", "results.0.headers.x-color-list.2");
	}

}