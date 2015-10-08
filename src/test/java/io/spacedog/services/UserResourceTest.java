package io.spacedog.services;

import org.junit.BeforeClass;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class UserResourceTest extends AbstractTest {

	@BeforeClass
	public static void resetTestAccount() throws UnirestException,
			InterruptedException {
		AdminResourceTest.resetTestAccount();
	}

	@Test
	public void shouldSignUpSuccessfullyAndMore() throws UnirestException,
			InterruptedException {

		// vince sign up should succeed

		RequestBodyEntity req1 = preparePost("/v1/user",
				AdminResourceTest.testClientKey()).body(
				Json.builder().add("username", "vince")
						.add("password", "hi vince")
						.add("email", "vince@dog.com").build().toString());

		post(req1, 201);

		refreshIndex("test");

		// get vince user object should succeed

		GetRequest req2 = prepareGet("/v1/user/vince",
				AdminResourceTest.testClientKey()).basicAuth("vince",
				"hi vince");

		JsonObject res2 = get(req2, 200).json();

		assertTrue(Json.equals(
				Json.builder().add("username", "vince")
						.add("hashedPassword", User.hashPassword("hi vince"))
						.add("email", "vince@dog.com").stArr("groups")
						.add("test").build(), res2.remove("meta")));

		// get data with wrong username should fail

		GetRequest req5 = prepareGet("/v1/user/vince",
				AdminResourceTest.testClientKey()).basicAuth("XXX", "hi vince");

		get(req5, 401);

		// get data with wrong password should fail

		GetRequest req3 = prepareGet("/v1/user/vince",
				AdminResourceTest.testClientKey()).basicAuth("vince", "XXX");

		get(req3, 401);

		// get data with wrong backend key should fail

		GetRequest req4 = prepareGet("/v1/user/vince", "XXX").basicAuth(
				"vince", "hi vince");

		get(req4, 401);

		// login shoud succeed

		GetRequest req6 = prepareGet("/v1/login",
				AdminResourceTest.testClientKey()).basicAuth("vince",
				"hi vince");

		get(req6, 200);

		// login with wrong password should fail

		GetRequest req7 = prepareGet("/v1/login",
				AdminResourceTest.testClientKey()).basicAuth("vince", "XXX");

		get(req7, 401);

		// email update should succeed

		RequestBodyEntity req8 = preparePut("/v1/user/vince",
				AdminResourceTest.testClientKey()).basicAuth("vince",
				"hi vince").body(
				Json.builder().add("email", "bignose@magic.com").build()
						.toString());

		put(req8, 200);

		refreshIndex("test");

		GetRequest req9 = prepareGet("/v1/user/vince",
				AdminResourceTest.testClientKey()).basicAuth("vince",
				"hi vince");

		JsonObject res9 = get(req9, 200).json();

		assertEquals(Json.get(res9, "meta.version").asInt(), 2);

		assertTrue(Json.equals(
				Json.builder().add("username", "vince")
						.add("hashedPassword", User.hashPassword("hi vince"))
						.add("email", "bignose@magic.com").stArr("groups")
						.add("test").build(), res9.remove("meta")));
	}
}
