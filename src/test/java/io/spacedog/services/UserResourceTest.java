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

		// signup ok

		RequestBodyEntity req1 = preparePost("/v1/user",
				AdminResourceTest.testKey()).body(
				Json.builder().add("username", "vince")
						.add("password", "hi vince")
						.add("email", "vince@dog.com").build().toString());

		post(req1, 201);

		refreshIndex("test");

		// get ok

		GetRequest req2 = prepareGet("/v1/user/vince",
				AdminResourceTest.testKey()).basicAuth("vince", "hi vince");

		JsonObject res2 = get(req2, 200).json();

		assertTrue(Json.equals(
				Json.builder().add("username", "vince")
						.add("password", "hi vince")
						.add("email", "vince@dog.com").stArr("groups")
						.add("test").build(), res2.remove("meta")));

		// wrong username

		GetRequest req5 = prepareGet("/v1/user/vince",
				AdminResourceTest.testKey()).basicAuth("XXX", "hi vince");

		get(req5, 401);

		// wrong password

		GetRequest req3 = prepareGet("/v1/user/vince",
				AdminResourceTest.testKey()).basicAuth("vince", "XXX");

		get(req3, 401);

		// wrong account key

		GetRequest req4 = prepareGet("/v1/user/vince", "XXX").basicAuth(
				"vince", "hi vince");

		get(req4, 401);

		// login ok

		GetRequest req6 = prepareGet("/v1/login", AdminResourceTest.testKey())
				.basicAuth("vince", "hi vince");

		get(req6, 200);

		// login nok

		GetRequest req7 = prepareGet("/v1/login", AdminResourceTest.testKey())
				.basicAuth("vince", "XXX");

		get(req7, 401);

		// email update ok

		RequestBodyEntity req8 = preparePut("/v1/user/vince",
				AdminResourceTest.testKey()).basicAuth("vince", "hi vince")
				.body(Json.builder().add("email", "bignose@magic.com").build()
						.toString());

		put(req8, 200);

		refreshIndex("test");

		// get ok

		GetRequest req9 = prepareGet("/v1/user/vince",
				AdminResourceTest.testKey()).basicAuth("vince", "hi vince");

		JsonObject res9 = get(req9, 200).json();

		assertEquals(Json.get(res9, "meta.version").asInt(), 2);

		assertTrue(Json.equals(
				Json.builder().add("username", "vince")
						.add("password", "hi vince")
						.add("email", "bignose@magic.com").stArr("groups")
						.add("test").build(), res9.remove("meta")));
	}
}
