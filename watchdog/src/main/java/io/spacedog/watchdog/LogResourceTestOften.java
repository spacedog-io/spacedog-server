package io.spacedog.watchdog;

import java.util.List;

import org.junit.Test;

import io.spacedog.client.DataObject;
import io.spacedog.client.SpaceDog;
import io.spacedog.client.LogEndpoint.LogItem;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceTest;
import io.spacedog.model.Schema;
import io.spacedog.utils.SpaceHeaders;

public class LogResourceTestOften extends SpaceTest {

	@Test
	public void doAFewThingsAndGetTheLogs() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog superadmin2 = resetTest2Backend();

		// create message schema in test backend
		superadmin.schema().set(Schema.builder("message").text("text").build());

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
		// the delete request is not part of the logs
		// since log starts with the backend creation
		List<LogItem> results = superadmin.log().get(10, true).results;
		assertEquals(7, results.size());
		assertEquals("GET", results.get(0).method);
		assertEquals("/1/data/message", results.get(0).path);
		assertEquals("GET", results.get(1).method);
		assertEquals("/1/data/message/" + message.id(), results.get(1).path);
		assertEquals("POST", results.get(2).method);
		assertEquals("/1/data/message", results.get(2).path);
		assertEquals("GET", results.get(3).method);
		assertEquals("/1/login", results.get(3).path);
		assertEquals("POST", results.get(4).method);
		assertEquals("/1/credentials", results.get(4).path);
		assertEquals("PUT", results.get(5).method);
		assertEquals("/1/schema/message", results.get(5).path);
		assertEquals("POST", results.get(6).method);
		assertEquals("/1/backend", results.get(6).path);

		// get all test2 backend logs
		results = superadmin2.log().get(4, true).results;
		assertEquals(3, results.size());
		assertEquals("GET", results.get(0).method);
		assertEquals("/1/login", results.get(0).path);
		assertEquals("POST", results.get(1).method);
		assertEquals("/1/credentials", results.get(1).path);
		assertEquals("********", results.get(1).payload.get(PASSWORD_FIELD).asText());
		assertEquals("POST", results.get(2).method);
		assertEquals("/1/backend", results.get(2).path);
		assertEquals("********", results.get(2).payload.get(PASSWORD_FIELD).asText());

		// after backend deletion, logs are not accessible to backend
		superadmin.admin().deleteBackend(superadmin.backendId());
		superadmin.get("/1/log").go(401);

		superadmin2.admin().deleteBackend(superadmin2.backendId());
		superadmin2.get("/1/log").go(401);
	}

	@Test
	public void checkPasswordsAreNotLogged() {

		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog fred = signUp("test", "fred", "hi fred");

		String passwordResetCode = superadmin.delete("/1/credentials/{id}/password")//
				.routeParam("id", fred.id()).go(200)//
				.getString("passwordResetCode");

		SpaceRequest.post("/1/credentials/{id}/password")//
				.routeParam("id", fred.id())//
				.queryParam("passwordResetCode", passwordResetCode)//
				.backend(superadmin).formField("password", "hi fred 2").go(200);

		SpaceRequest.put("/1/credentials/{id}/password").backend(superadmin)//
				.routeParam("id", fred.id()).basicAuth("fred", "hi fred 2")//
				.formField("password", "hi fred 3").go(200);

		List<LogItem> results = superadmin.log().get(7, true).results;
		assertEquals(6, results.size());
		assertEquals("PUT", results.get(0).method);
		assertEquals("/1/credentials/" + fred.id() + "/password", results.get(0).path);
		assertEquals("********", results.get(0).getParameter(PASSWORD_FIELD));
		assertEquals("POST", results.get(1).method);
		assertEquals("/1/credentials/" + fred.id() + "/password", results.get(1).path);
		assertEquals("********", results.get(1).getParameter(PASSWORD_FIELD));
		assertEquals(passwordResetCode, //
				results.get(1).getParameter(PASSWORD_RESET_CODE_FIELD));
		assertEquals("DELETE", results.get(2).method);
		assertEquals("/1/credentials/" + fred.id() + "/password", results.get(2).path);
		assertEquals(passwordResetCode, //
				results.get(2).response.get(PASSWORD_RESET_CODE_FIELD).asText());
		assertEquals("GET", results.get(3).method);
		assertEquals("/1/login", results.get(3).path);
		assertEquals("POST", results.get(4).method);
		assertEquals("/1/credentials", results.get(4).path);
		assertEquals("fred", results.get(4).payload.get(USERNAME_FIELD).asText());
		assertEquals("********", results.get(4).payload.get(PASSWORD_FIELD).asText());
		assertEquals("POST", results.get(5).method);
		assertEquals("/1/backend", results.get(5).path);
		assertEquals("test", results.get(5).payload.get(USERNAME_FIELD).asText());
		assertEquals("********", results.get(5).payload.get(PASSWORD_FIELD).asText());
	}

	@Test
	public void testSpecialCases() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// fails because invalid body
		superadmin.put("/1/schema/toto").bodyString("XXX").go(400);

		// but logs the failed request without the json content
		LogItem logItem = superadmin.log().get(1, true).results.get(0);
		assertEquals("PUT", logItem.method);
		assertEquals("/1/schema/toto", logItem.path);
		assertEquals(400, logItem.status);
		assertNull(logItem.payload);
		assertNull(logItem.parameters);

		// check that log response results are not logged
		superadmin.log().get(10);
		logItem = superadmin.log().get(1, true).results.get(0);
		assertEquals("GET", logItem.method);
		assertEquals("/1/log", logItem.path);
		assertNull(logItem.response.get("results"));

		// Headers are logged if not empty
		// and 'Authorization' header is not logged
		superadmin.get("/1/log")//
				.setHeader("x-empty", "")//
				.setHeader("x-blank", " ")//
				.setHeader("x-color", "YELLOW")//
				.addHeader("x-color-list", "RED")//
				.addHeader("x-color-list", "BLUE")//
				.addHeader("x-color-list", "GREEN")//
				.go(200);

		logItem = superadmin.log().get(1, true).results.get(0);
		assertTrue(logItem.getHeader(SpaceHeaders.AUTHORIZATION).isEmpty());
		assertTrue(logItem.getHeader("x-empty").isEmpty());
		assertTrue(logItem.getHeader("x-blank").isEmpty());
		assertTrue(logItem.getHeader("x-color").contains("YELLOW"));
		assertTrue(logItem.getHeader("x-color-list").contains("RED"));
		assertTrue(logItem.getHeader("x-color-list").contains("BLUE"));
		assertTrue(logItem.getHeader("x-color-list").contains("GREEN"));
	}

}
