/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.body.RequestBodyEntity;

import io.spacedog.services.AdminResourceTest.ClientAccount;

public class UserResourceTest extends AbstractTest {

	private static ClientAccount testAccount;

	@BeforeClass
	public static void resetTestAccount() throws UnirestException, InterruptedException, IOException {
		testAccount = AdminResourceTest.resetTestAccount();
	}

	@Test
	public void shouldSignUpSuccessfullyAndMore() throws UnirestException, InterruptedException, IOException {

		// vince sign up should succeed

		RequestBodyEntity req1 = preparePost("/v1/user", testAccount.backendKey).body(Json.startObject()
				.put("username", "vince").put("password", "hi vince").put("email", "vince@dog.com").build().toString());

		post(req1, 201);

		refreshIndex("test");

		// get vince user object should succeed

		GetRequest req2 = prepareGet("/v1/user/vince", testAccount.backendKey).basicAuth("vince", "hi vince");

		ObjectNode res2 = get(req2, 200).objectNode();

		assertEquals(
				Json.startObject().put("username", "vince").put("hashedPassword", User.hashPassword("hi vince"))
						.put("email", "vince@dog.com").startArray("groups").add("test").build(),
				res2.deepCopy().without("meta"));

		// get data with wrong username should fail

		GetRequest req5 = prepareGet("/v1/user/vince", testAccount.backendKey).basicAuth("XXX", "hi vince");

		get(req5, 401);

		// get data with wrong password should fail

		GetRequest req3 = prepareGet("/v1/user/vince", testAccount.backendKey).basicAuth("vince", "XXX");

		get(req3, 401);

		// get data with wrong backend key should fail

		GetRequest req4 = prepareGet("/v1/user/vince", "XXX").basicAuth("vince", "hi vince");

		get(req4, 401);

		// login shoud succeed

		GetRequest req6 = prepareGet("/v1/login", testAccount.backendKey).basicAuth("vince", "hi vince");

		get(req6, 200);

		// login with wrong password should fail

		GetRequest req7 = prepareGet("/v1/login", testAccount.backendKey).basicAuth("vince", "XXX");

		get(req7, 401);

		// email update should succeed

		RequestBodyEntity req8 = preparePut("/v1/user/vince", testAccount.backendKey).basicAuth("vince", "hi vince")
				.body(Json.startObject().put("email", "bignose@magic.com").build().toString());

		put(req8, 200);

		refreshIndex("test");

		GetRequest req9 = prepareGet("/v1/user/vince", testAccount.backendKey).basicAuth("vince", "hi vince");

		ObjectNode res9 = get(req9, 200).objectNode();

		assertEquals(2, res9.get("meta").get("version").asInt());

		assertEquals(
				Json.startObject().put("username", "vince").put("hashedPassword", User.hashPassword("hi vince"))
						.put("email", "bignose@magic.com").startArray("groups").add("test").build(),
				res9.deepCopy().without("meta"));
	}

	public static ClientUser createUser(String backendKey, String username, String password, String email)
			throws UnirestException, IOException {
		RequestBodyEntity req = preparePost("/v1/user/", backendKey).body(
				Json.startObject().put("username", username).put("password", password).put("email", email).toString());

		String id = post(req, 201).objectNode().get("id").asText();

		return new ClientUser(id, username, password, email);
	}

	public static class ClientUser {
		String id;
		String username;
		String password;
		String email;

		public ClientUser(String id, String username, String password, String email) {
			this.id = id;
			this.username = username;
			this.password = password;
			this.email = email;
		}
	}

}
