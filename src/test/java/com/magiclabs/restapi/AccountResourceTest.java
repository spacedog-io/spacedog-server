package com.magiclabs.restapi;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class AccountResourceTest extends AbstractTest {

	@Test
	public void shouldSignUpTestAccount() throws UnirestException,
			InterruptedException {
		resetTestAccount();

		GetRequest req1 = Unirest.get("http://localhost:8080/v1/account/test");
		JsonObject res1 = get(req1, 200);
		assertEquals(Json.builder().add("id", "test").build(), res1);

		GetRequest req2 = Unirest.get("http://localhost:8080/v1/user/dave")
				.basicAuth("dave", "hi_dave")
				.header("x-magic-account-id", "test");
		JsonObject res2 = get(req2, 200);
		assertTrue(Json.equals(
				Json.builder().add("username", "dave")
						.add("password", "hi_dave")
						.add("email", "dave@magic.com").stArr("groups")
						.add("admin").build(), res2));

	}

	public static void resetTestAccount() throws UnirestException {
		HttpRequestWithBody req1 = Unirest
				.delete("http://localhost:8080/v1/account/test");

		delete(req1, 200, 404);

		RequestBodyEntity req2 = Unirest.post(
				"http://localhost:8080/v1/account/").body(
				Json.builder().add("id", "test").add("username", "dave")
						.add("password", "hi_dave")
						.add("email", "dave@magic.com").build().toString());

		post(req2, 201);

		refreshIndex("admin");
		refreshIndex("test");
	}

}
