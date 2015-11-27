/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceDogHelper;

public class AdminResourceTest extends Assert {

	@Test
	public void shouldDeleteSignUpGetLoginTestAccount() throws Exception {

		SpaceDogHelper.Account testAccount = SpaceDogHelper.resetTestAccount();

		// get just created test account should succeed

		JsonNode res1 = SpaceRequest.get("/v1/admin/account/{backendId}").routeParam("backendId", "test")
				.basicAuth(testAccount).go(200).jsonNode();

		assertEquals("test", res1.get("backendId").asText());
		assertEquals("test", res1.get("username").asText());
		assertNotEquals("hi test", res1.get("hashedPassword").asText());
		assertEquals("david@spacedog.io", res1.get("email").asText());

		// create new account with same username should fail

		JsonNode json2 = SpaceRequest.post("/v1/admin/account/").body(Json.startObject().put("backendId", "anothertest")
				.put("username", "test").put("password", "hi test").put("email", "hello@spacedog.io").toString())
				.go(400).jsonNode();

		assertEquals("test", json2.get("invalidParameters").get("username").get("value").asText());

		// create new account with same backend id should fail

		JsonNode json2b = SpaceRequest.post("/v1/admin/account/")
				.body(Json.startObject().put("backendId", "test").put("username", "anotheruser")
						.put("password", "hi anotheruser").put("email", "hello@spacedog.io").toString())
				.go(400).jsonNode();

		assertEquals("test", json2b.get("invalidParameters").get("backendId").get("value").asText());

		// admin user login should succeed

		String backendKey = SpaceRequest.get("/v1/admin/login").basicAuth(testAccount).go(200).httpResponse()
				.getHeaders().get(AdminResource.BACKEND_KEY_HEADER).get(0);

		assertEquals(testAccount.backendKey, backendKey);

		// no header no user login should fail

		SpaceRequest.get("/v1/admin/login").go(401);

		// invalid admin username login should fail

		SpaceRequest.get("/v1/admin/login").basicAuth("XXX", "hi test").go(401);

		// invalid admin password login should fail

		SpaceRequest.get("/v1/admin/login").basicAuth("test", "hi XXX").go(401);

		// data access with client key should succeed

		JsonNode res7 = SpaceRequest.get("/v1/data").backendKey(testAccount).go(200).jsonNode();
		assertEquals(0, res7.get("total").asInt());

		// data access with admin user should succeed

		JsonNode res7b = SpaceRequest.get("/v1/data").basicAuth("test", "hi test").go(200).jsonNode();
		assertEquals(0, res7b.get("total").asInt());

		// data access with admin user but client key should fail

		SpaceRequest.get("/v1/data").backendKey(testAccount).basicAuth(testAccount).go(401).jsonNode();

		// let's create a common user in 'test' backend

		SpaceDogHelper.User john = SpaceDogHelper.createUser(testAccount.backendKey, "john", "hi john",
				"john@dog.io");

		SpaceDogHelper.refresh("test");

		// data access with common user but no client key should fail

		SpaceRequest.get("/v1/data").basicAuth(john).go(401);

		// admin access with regular user and backend key should fail

		SpaceRequest.get("/v1/admin/account").backendKey(testAccount).basicAuth(john).go(401);

	}

	@Test
	public void shouldFailToAccessAnotherAccountWithValidAdminUsername() throws Exception {

		SpaceDogHelper.Account aaaa = SpaceDogHelper.resetAccount("aaaa", "aaaa", "hi aaaa", "hello@spacedog.io");
		SpaceDogHelper.Account zzzz = SpaceDogHelper.resetAccount("zzzz", "zzzz", "hi zzzz", "hello@spacedog.io");

		// should fail to get account since backend is not consistent with
		// account

		SpaceRequest.get("/v1/admin/account/aaaa").basicAuth(zzzz).go(401);
		SpaceRequest.get("/v1/admin/account/zzzz").basicAuth(aaaa).go(401);

		// should fail to delete account since backend is not consistent with
		// account

		SpaceRequest.delete("/v1/admin/account/aaaa").basicAuth(zzzz).go(401);
		SpaceRequest.delete("/v1/admin/account/zzzz").basicAuth(aaaa).go(401);

		// should succeed to get accounts

		SpaceRequest.get("/v1/admin/account/aaaa").basicAuth(aaaa).go(200);
		SpaceRequest.get("/v1/admin/account/zzzz").basicAuth(zzzz).go(200);

		// should succeed to delete accounts

		SpaceRequest.delete("/v1/admin/account/aaaa").basicAuth(aaaa).go(200);
		SpaceRequest.delete("/v1/admin/account/zzzz").basicAuth(zzzz).go(200);
	}
}
