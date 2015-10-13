/**
 * © David Attias 2015
 */
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
	public void shouldDeleteSignUpGetLoginTestAccount() throws UnirestException, InterruptedException {

		resetTestAccount();

		// get just created test account should succeed

		GetRequest req1 = prepareGet("/v1/admin/account/test").basicAuth("test", "hi test");
		JsonObject res1 = get(req1, 200).json();

		assertEquals("test", Json.get(res1, "backendId").asString());
		assertEquals("test", Json.get(res1, "username").asString());
		assertNotEquals("hi test", Json.get(res1, "hashedPassword").asString());
		assertEquals("hello@spacedog.io", Json.get(res1, "email").asString());

		// create new account with same username should fail

		RequestBodyEntity req2 = preparePost("/v1/admin/account/").body(Json.builder().add("backendId", "anothertest")
				.add("username", "test").add("password", "hi test").add("email", "hello@spacedog.io").toString());

		JsonObject json2 = post(req2, 400).json();

		assertEquals("test", Json.get(json2, "invalidParameters.username.value").asString());

		// create new account with same backend id should fail

		RequestBodyEntity req2b = preparePost("/v1/admin/account/")
				.body(Json.builder().add("backendId", "test").add("username", "anotheruser")
						.add("password", "hi anotheruser").add("email", "hello@spacedog.io").toString());

		JsonObject json2b = post(req2b, 400).json();

		assertEquals("test", Json.get(json2b, "invalidParameters.backendId.value").asString());

		// admin user login should succeed

		GetRequest req3 = prepareGet("/v1/admin/login").basicAuth("test", "hi test");
		String loginKey = get(req3, 200).response().getHeaders().get(AdminResource.BACKEND_KEY_HEADER).get(0);
		assertEquals(testClientKey, loginKey);

		// no header no user login should fail

		GetRequest req4 = prepareGet("/v1/admin/login");
		get(req4, 401);

		// invalid admin username login should fail

		GetRequest req5 = prepareGet("/v1/admin/login").basicAuth("XXX", "hi test");
		get(req5, 401);

		// invalid admin password login should fail

		GetRequest req6 = prepareGet("/v1/admin/login").basicAuth("test", "hi XXX");
		get(req6, 401);

		// data access with client key should succeed

		GetRequest req7 = prepareGet("/v1/data", testClientKey);
		JsonObject res7 = get(req7, 200).json();
		assertEquals(0, Json.get(res7, "total").asInt());

		// data access with admin user should succeed

		GetRequest req7b = prepareGet("/v1/data").basicAuth("test", "hi test");
		JsonObject res7b = get(req7b, 200).json();
		assertEquals(0, Json.get(res7b, "total").asInt());

		// data access with admin user but client key should fail

		GetRequest req8 = prepareGet("/v1/data", testClientKey).basicAuth("test", "hi test");
		get(req8, 401).json();

		// let's create a common user in 'test' backend

		RequestBodyEntity req9a = preparePost("/v1/user", testClientKey).body(Json.builder().add("username", "john")
				.add("password", "hi john").add("email", "john@dog.io").toString());
		post(req9a, 201);

		refreshIndex("test");

		// data access with common user but no client key should fail

		GetRequest req9b = prepareGet("/v1/data").basicAuth("john", "hi john");
		get(req9b, 401);

		// admin access with regular user and backend key should fail

		GetRequest req10 = prepareGet("/v1/admin/account/test", testClientKey).basicAuth("john", "hi john");
		get(req10, 401);

	}

	public static void resetTestAccount() throws UnirestException {

		HttpRequestWithBody req1 = prepareDelete("/v1/admin/account/test").basicAuth("test", "hi test");

		// 401 Unauthorized is valid since if this account does not exist
		// delete returns 401 because admin username and password
		// won't match any account
		delete(req1, 200, 401);

		refreshIndex(AdminResource.SPACEDOG_INDEX);
		refreshIndex("test");

		RequestBodyEntity req2 = preparePost("/v1/admin/account/").body(Json.builder().add("backendId", "test")
				.add("username", "test").add("password", "hi test").add("email", "hello@spacedog.io").toString());

		testClientKey = post(req2, 201).response().getHeaders().get(AdminResource.BACKEND_KEY_HEADER).get(0);

		assertFalse(Strings.isNullOrEmpty(testClientKey));

		refreshIndex(AdminResource.SPACEDOG_INDEX);
		refreshIndex("test");
	}

	public static String testClientKey() {
		return testClientKey;
	}

	private static String testClientKey;
}
