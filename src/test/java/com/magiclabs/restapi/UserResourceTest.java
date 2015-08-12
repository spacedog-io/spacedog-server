package com.magiclabs.restapi;

import org.junit.BeforeClass;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class UserResourceTest extends AbstractTest {

	@BeforeClass
	public static void resetTestAccount() throws UnirestException,
			InterruptedException {
		AccountResourceTest.resetTestAccount();
	}

	@Test
	public void shouldSignUpSuccessfullyAndMore() throws UnirestException,
			InterruptedException {

		// signup ok

		RequestBodyEntity req1 = Unirest
				.post("http://localhost:8080/v1/user/")
				.basicAuth("dave", "hi_dave")
				.header("x-magic-app-id", "test")
				.body(Json.builder().add("username", "vince")
						.add("password", "hi_vince")
						.add("email", "vince@magic.com").build().toString());

		post(req1, 201);

		refreshIndex("test");

		// get ok

		GetRequest req2 = Unirest.get("http://localhost:8080/v1/user/vince")
				.basicAuth("vince", "hi_vince")
				.header("x-magic-app-id", "test");

		JsonObject res2 = get(req2, 200);

		assertTrue(Json.equals(
				Json.builder().add("username", "vince")
						.add("password", "hi_vince")
						.add("email", "vince@magic.com").stArr("groups")
						.add("test").build(), res2.remove("meta")));

		// wrong username

		GetRequest req5 = Unirest.get("http://localhost:8080/v1/user/vince")
				.basicAuth("XXX", "hi_vince").header("x-magic-app-id", "test");

		get(req5, 401);

		// wrong password

		GetRequest req3 = Unirest.get("http://localhost:8080/v1/user/vince")
				.basicAuth("vince", "XXX").header("x-magic-app-id", "test");

		get(req3, 401);

		// wrong account id

		GetRequest req4 = Unirest.get("http://localhost:8080/v1/user/vince")
				.basicAuth("vince", "hi_vince").header("x-magic-app-id", "XXX");

		get(req4, 401);

		// login ok

		GetRequest req6 = Unirest.get("http://localhost:8080/v1/login")
				.basicAuth("vince", "hi_vince")
				.header("x-magic-app-id", "test");

		get(req6, 200);

		// login nok

		GetRequest req7 = Unirest.get("http://localhost:8080/v1/login")
				.basicAuth("vince", "XXX").header("x-magic-app-id", "test");

		get(req7, 401);

		// email update ok

		RequestBodyEntity req8 = Unirest
				.put("http://localhost:8080/v1/user/vince")
				.basicAuth("vince", "hi_vince")
				.header("x-magic-app-id", "test")
				.body(Json.builder().add("email", "bignose@magic.com").build()
						.toString());

		put(req8, 200);

		refreshIndex("test");

		// get ok

		GetRequest req9 = Unirest.get("http://localhost:8080/v1/user/vince")
				.basicAuth("vince", "hi_vince")
				.header("x-magic-app-id", "test");

		JsonObject res9 = get(req9, 200);

		assertEquals(Json.get(res9, "meta.version").asInt(), 2);

		assertTrue(Json.equals(
				Json.builder().add("username", "vince")
						.add("password", "hi_vince")
						.add("email", "bignose@magic.com").stArr("groups")
						.add("test").build(), res9.remove("meta")));
	}
}
