/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceDogHelper;

public class UserResourceTest extends Assert {

	@Test
	public void shouldSignUpSuccessfullyAndMore() throws Exception {

		SpaceDogHelper.Account testAccount = SpaceDogHelper.resetTestAccount();

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

		SpaceDogHelper.User vince = SpaceDogHelper.createUser(testAccount.backendKey, "vince", "hi vince", "vince@dog.com");

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

		SpaceDogHelper.Account testAccount = SpaceDogHelper.resetTestAccount();

		// vince sign up should succeed

		SpaceDogHelper.createUser(testAccount.backendKey, "vince", "hi vince", "vince@dog.com");

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

}
