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

public class UserResourceTest extends AbstractTest {

	@BeforeClass
	public static void resetTestAccount() throws UnirestException, InterruptedException, IOException {
		AdminResourceTest.resetTestAccount();
	}

	@Test
	public void shouldSignUpSuccessfullyAndMore() throws UnirestException, InterruptedException, IOException {

		// vince sign up should succeed

		RequestBodyEntity req1 = preparePost("/v1/user", AdminResourceTest.testClientKey()).body(Json.startObject()
				.put("username", "vince").put("password", "hi vince").put("email", "vince@dog.com").build().toString());

		post(req1, 201);

		refreshIndex("test");

		// get vince user object should succeed

		GetRequest req2 = prepareGet("/v1/user/vince", AdminResourceTest.testClientKey()).basicAuth("vince",
				"hi vince");

		ObjectNode res2 = get(req2, 200).objectNode();

		assertEquals(
				Json.startObject().put("username", "vince").put("hashedPassword", User.hashPassword("hi vince"))
						.put("email", "vince@dog.com").startArray("groups").add("test").build(),
				res2.deepCopy().without("meta"));

		// get data with wrong username should fail

		GetRequest req5 = prepareGet("/v1/user/vince", AdminResourceTest.testClientKey()).basicAuth("XXX", "hi vince");

		get(req5, 401);

		// get data with wrong password should fail

		GetRequest req3 = prepareGet("/v1/user/vince", AdminResourceTest.testClientKey()).basicAuth("vince", "XXX");

		get(req3, 401);

		// get data with wrong backend key should fail

		GetRequest req4 = prepareGet("/v1/user/vince", "XXX").basicAuth("vince", "hi vince");

		get(req4, 401);

		// login shoud succeed

		GetRequest req6 = prepareGet("/v1/login", AdminResourceTest.testClientKey()).basicAuth("vince", "hi vince");

		get(req6, 200);

		// login with wrong password should fail

		GetRequest req7 = prepareGet("/v1/login", AdminResourceTest.testClientKey()).basicAuth("vince", "XXX");

		get(req7, 401);

		// email update should succeed

		RequestBodyEntity req8 = preparePut("/v1/user/vince", AdminResourceTest.testClientKey())
				.basicAuth("vince", "hi vince")
				.body(Json.startObject().put("email", "bignose@magic.com").build().toString());

		put(req8, 200);

		refreshIndex("test");

		GetRequest req9 = prepareGet("/v1/user/vince", AdminResourceTest.testClientKey()).basicAuth("vince",
				"hi vince");

		ObjectNode res9 = get(req9, 200).objectNode();

		assertEquals(2, res9.get("meta").get("version").asInt());

		assertEquals(
				Json.startObject().put("username", "vince").put("hashedPassword", User.hashPassword("hi vince"))
						.put("email", "bignose@magic.com").startArray("groups").add("test").build(),
				res9.deepCopy().without("meta"));
	}

	public static void createUser(String backendKey, String username, String password, String email)
			throws UnirestException, IOException {
		RequestBodyEntity req = preparePost("/v1/user/", backendKey).body(
				Json.startObject().put("username", username).put("password", password).put("email", email).toString());

		post(req, 201);
	}
}
