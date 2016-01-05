package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceDogHelper.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder2;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class LogResourceTest extends Assert {

	@Test
	public void doAFewThingsAndGetTheLogs() throws Exception {

		SpaceDogHelper.printTestHeader();

		// create a test account
		Account testAccount = SpaceDogHelper.resetTestAccount();

		// create test2 account
		Account test2Account = SpaceDogHelper.resetAccount("test2", "test2", "hi test2", "test2@dog.com");

		// create message schema in test backend
		SpaceDogHelper.setSchema(//
				SchemaBuilder2.builder("message")//
						.textProperty("text", "english", true).build(),
				testAccount);

		// create a user in test2 backend
		SpaceDogHelper.createUser(test2Account, "fred", "hi fred", "fred@dog.com");

		// create message in test backend
		String id = SpaceRequest.post("/v1/data/message")//
				.backendKey(testAccount)//
				.body(Json.objectBuilder().put("text", "What's up boys?").toString())//
				.go(201)//
				.getFromJson("id")//
				.asText();

		// find message by id in test backend
		SpaceRequest.get("/v1/data/message/" + id).backendKey(testAccount).go(200);

		// create user in test backend
		User vince = SpaceDogHelper.createUser(testAccount, "vince", "hi vince", "vince@dog.com");

		// find all messages in test backend
		SpaceRequest.get("/v1/data/message").backendKey(testAccount).basicAuth(vince).go(200);

		// get all test account logs
		SpaceRequest.get("/v1/admin/log?size=7").basicAuth(testAccount).go(200)//
				.assertSizeEquals(7, "results")//
				.assertEquals("GET", "results.0.method")//
				.assertEquals("/v1/data/message", "results.0.path")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/v1/user", "results.1.path")//
				.assertEquals("GET", "results.2.method")//
				.assertEquals("/v1/data/message/" + id, "results.2.path")//
				.assertEquals("POST", "results.3.method")//
				.assertEquals("/v1/data/message", "results.3.path")//
				.assertEquals("POST", "results.4.method")//
				.assertEquals("/v1/schema/message", "results.4.path")//
				.assertEquals("POST", "results.5.method")//
				.assertEquals("/v1/admin/account", "results.5.path")//
				.assertEquals("DELETE", "results.6.method")//
				.assertEquals("/v1/admin/account/test", "results.6.path");

		// get all test2 account logs
		SpaceRequest.get("/v1/admin/log?size=3").basicAuth(test2Account).go(200)//
				.assertSizeEquals(3, "results")//
				.assertEquals("POST", "results.0.method")//
				.assertEquals("/v1/user", "results.0.path")//
				.assertNotPresent("results.0.content.password")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/v1/admin/account", "results.1.path")//
				.assertNotPresent("results.1.content.password")//
				.assertEquals("DELETE", "results.2.method")//
				.assertEquals("/v1/admin/account/test2", "results.2.path");

		// after account deletion, logs are not accessible to account
		SpaceDogHelper.deleteAccount(testAccount);
		SpaceRequest.get("/v1/admin/log").basicAuth(testAccount).go(401);

		SpaceDogHelper.deleteAccount(test2Account);
		SpaceRequest.get("/v1/admin/log").basicAuth(test2Account).go(401);
	}
}
