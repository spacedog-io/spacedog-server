/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.services.AdminResourceTest.ClientAccount;

public class UserResourceTest extends Assert {

	@Test
	public void shouldSignUpSuccessfullyAndMore() throws Exception {

		ClientAccount testAccount = AdminResourceTest.resetTestAccount();

		// fails since invalid users

		SpaceRequest.post("/v1/user/").backendKey(testAccount).body(Json.startObject()).go(400);
		SpaceRequest.post("/v1/user/").backendKey(testAccount).body(//
				Json.startObject().put("username", "titi").put("email", "titi@dog.com")).go(400);
		SpaceRequest.post("/v1/user/").backendKey(testAccount).body(//
				Json.startObject().put("password", "hi titi").put("email", "titi@dog.com")).go(400);
		SpaceRequest.post("/v1/user/").backendKey(testAccount).body(//
				Json.startObject().put("username", "titi").put("password", "hi titi")).go(400);

		// fails to inject forged hashedPassword

		SpaceRequest.post("/v1/user/").backendKey(testAccount)
				.body(//
						Json.startObject().put("username", "titi")//
								.put("password", "hi titi")//
								.put("email", "titi@dog.com")//
								.put("hashedPassword", "hi titi"))
				.go(400);

		// vince sign up should succeed

		ClientUser vince = createUser(testAccount.backendKey, "vince", "hi vince", "vince@dog.com");

		SpaceRequest.refresh(testAccount);

		// get vince user object should succeed

		ObjectNode res2 = SpaceRequest.get("/v1/user/vince").backendKey(testAccount).basicAuth(vince).go(200)
				.objectNode();

		assertEquals(
				Json.startObject().put("username", "vince").put("hashedPassword", UserUtils.hashPassword("hi vince"))
						.put("email", "vince@dog.com").startArray("groups").add("test").build(),
				res2.deepCopy().without("meta"));

		// get data with wrong username should fail

		SpaceRequest.get("/v1/user/vince").backendKey(testAccount).basicAuth("XXX", "hi vince").go(401);

		// get data with wrong password should fail

		SpaceRequest.get("/v1/user/vince").backendKey(testAccount).basicAuth("vince", "XXX").go(401);

		// get data with wrong backend key should fail

		SpaceRequest.get("/v1/user/vince").backendKey("XXX").basicAuth(vince).go(401);

		// login shoud succeed

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth(vince).go(200);

		// login with wrong password should fail

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("vince", "XXX").go(401);

		// email update should succeed

		SpaceRequest.put("/v1/user/vince").backendKey(testAccount).basicAuth(vince)
				.body(Json.startObject().put("email", "bignose@magic.com").build().toString()).go(200);

		SpaceRequest.refresh(testAccount);

		ObjectNode res9 = SpaceRequest.get("/v1/user/vince").backendKey(testAccount).basicAuth(vince).go(200)
				.objectNode();

		assertEquals(2, res9.get("meta").get("version").asInt());

		assertEquals(
				Json.startObject().put("username", "vince").put("hashedPassword", UserUtils.hashPassword("hi vince"))
						.put("email", "bignose@magic.com").startArray("groups").add("test").build(),
				res9.deepCopy().without("meta"));
	}

	@Test
	public void shouldSetUserCustomScemaAndMore() throws Exception {

		ClientAccount testAccount = AdminResourceTest.resetTestAccount();

		// vince sign up should succeed

		UserResourceTest.createUser(testAccount.backendKey, "vince", "hi vince", "vince@dog.com");

		SpaceRequest.refresh(testAccount);

		// update test account user schema

		ObjectNode customUserSchema = UserResource.getDefaultUserSchemaBuilder()//
				.stringProperty("firstname", true)//
				.stringProperty("lastname", true)//
				.build();

		SpaceRequest.put("/v1/schema/user").basicAuth(testAccount).body(customUserSchema).go(201);

		// create new custom user

		ObjectNode fred = Json.startObject().put("username", "fred")//
				.put("password", "hi fred")//
				.put("email", "fred@dog.com")//
				.put("firstname", "Frédérique")//
				.put("lastname", "Fallière")//
				.build();

		SpaceRequest.post("/v1/user/").backendKey(testAccount).body(fred).go(201);

		// get the brand new user and check properties are correct

		ObjectNode fredFromServer = SpaceRequest.get("/v1/user/fred").basicAuth(testAccount).go(200).objectNode();
		assertEquals(fred.without("password"), //
				fredFromServer.without(Arrays.asList("hashedPassword", "groups", "meta")));
	}

	public static ClientUser createUser(String backendKey, String username, String password, String email)
			throws Exception {

		String id = SpaceRequest.post("/v1/user/").backendKey(backendKey)
				.body(Json.startObject().put("username", username).put("password", password).put("email", email))
				.go(201).objectNode().get("id").asText();

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

	public static void deleteUser(String string, ClientAccount account) throws Exception {
		deleteUser(string, account.username, account.password);
	}

	public static void deleteUser(String username, String adminUsername, String password) throws Exception {
		SpaceRequest.delete("/v1/user/{username}").routeParam("username", username).basicAuth(adminUsername, password)
				.go(200, 404);
	}

}
