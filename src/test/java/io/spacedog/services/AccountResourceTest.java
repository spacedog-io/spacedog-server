package io.spacedog.services;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.google.common.base.Strings;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class AccountResourceTest extends AbstractTest {

	@Test
	public void shouldSignUpTestAccount() throws UnirestException,
			InterruptedException {

		resetTestAccount();

		GetRequest req1 = prepareGet("/v1/account/test");
		JsonObject res1 = get(req1, 200).json();

		assertEquals("test", Json.get(res1, "backendId").asString());
		assertEquals("dave", Json.get(res1, "username").asString());
		assertEquals("hi dave", Json.get(res1, "password").asString());
		assertEquals("david@spacedog.io", Json.get(res1, "email").asString());

		GetRequest req2 = prepareGet("/v1/data", testKey);
		JsonObject res2 = get(req2, 200).json();
		assertEquals(0, Json.get(res2, "total").asInt());

	}

	public static void resetTestAccount() throws UnirestException {

		HttpRequestWithBody req1 = prepareDelete("/v1/account/test");
		delete(req1, 200, 404);

		RequestBodyEntity req2 = preparePost("/v1/account/").body(
				Json.builder().add("backendId", "test").add("username", "dave")
						.add("password", "hi dave")
						.add("email", "david@spacedog.io").build().toString());

		testKey = post(req2, 201).response().getHeaders()
				.get(AccountResource.SPACEDOG_KEY_HEADER).get(0);

		assertFalse(Strings.isNullOrEmpty(testKey));

		refreshIndex(AccountResource.SPACEDOG_INDEX);
		refreshIndex("test");
	}

	public static String testKey() {
		return testKey;
	}

	private static String testKey;
}
