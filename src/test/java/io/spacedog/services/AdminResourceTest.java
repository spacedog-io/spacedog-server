package io.spacedog.services;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.google.common.base.Strings;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class AdminResourceTest extends AbstractTest {

	@Test
	public void shouldDeleteSignUpGetLoginTestAccount()
			throws UnirestException, InterruptedException {

		resetTestAccount();

		// test GET

		GetRequest req1 = prepareGet("/v1/admin/account/test");
		JsonObject res1 = get(req1, 200).json();

		assertEquals("test", Json.get(res1, "backendId").asString());
		assertEquals("test", Json.get(res1, "username").asString());
		assertEquals("hi test", Json.get(res1, "password").asString());
		assertEquals("hello@spacedog.io", Json.get(res1, "email").asString());

		// test spacedog key

		GetRequest req2 = prepareGet("/v1/data", testKey);
		JsonObject res2 = get(req2, 200).json();
		assertEquals(0, Json.get(res2, "total").asInt());

		// test login ok

		GetRequest req3 = prepareGet("/v1/admin/login").basicAuth("test",
				"hi test");
		String loginKey = get(req3, 200).response().getHeaders()
				.get(AdminResource.SPACEDOG_KEY_HEADER).get(0);
		assertEquals(testKey, loginKey);

		// test login nok

		GetRequest req4 = prepareGet("/v1/admin/login");
		get(req4, 401);

		GetRequest req5 = prepareGet("/v1/admin/login").basicAuth("XXX",
				"hi test");
		get(req5, 401);

		GetRequest req6 = prepareGet("/v1/admin/login").basicAuth("test",
				"hi XXX");
		get(req6, 401);
	}

	public static void resetTestAccount() throws UnirestException {

		HttpRequestWithBody req1 = prepareDelete("/v1/admin/account/test");
		delete(req1, 200, 404);

		refreshIndex(AdminResource.SPACEDOG_INDEX);
		refreshIndex("test");

		RequestBodyEntity req2 = preparePost("/v1/admin/account/").body(
				Json.builder().add("backendId", "test").add("username", "test")
						.add("password", "hi test")
						.add("email", "hello@spacedog.io").build().toString());

		testKey = post(req2, 201).response().getHeaders()
				.get(AdminResource.SPACEDOG_KEY_HEADER).get(0);

		assertFalse(Strings.isNullOrEmpty(testKey));

		refreshIndex(AdminResource.SPACEDOG_INDEX);
		refreshIndex("test");
	}

	public static String testKey() {
		return testKey;
	}

	private static String testKey;
}
