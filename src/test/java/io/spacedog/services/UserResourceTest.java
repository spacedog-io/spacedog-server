/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceRequest;

public class UserResourceTest extends Assert {

	@Test
	public void shouldSignUpSuccessfullyAndMore() throws Exception {

		SpaceDogHelper.Account testAccount = SpaceDogHelper.resetTestAccount();

		// fails since invalid users

		SpaceRequest.post("/v1/user/").backendKey(testAccount).body(Json.objectBuilder()).go(400);
		SpaceRequest.post("/v1/user/").backendKey(testAccount).body(//
				Json.objectBuilder().put("password", "hi titi").put("email", "titi@dog.com")).go(400);
		SpaceRequest.post("/v1/user/").backendKey(testAccount).body(//
				Json.objectBuilder().put("username", "titi").put("password", "hi titi")).go(400);

		// fails to inject forged hashedPassword

		SpaceRequest.post("/v1/user/").backendKey(testAccount)
				.body(//
						Json.objectBuilder().put("username", "titi")//
								.put("password", "hi titi")//
								.put("email", "titi@dog.com")//
								.put("hashedPassword", "hi titi"))
				.go(400);

		// vince sign up should succeed

		SpaceDogHelper.User vince = SpaceDogHelper.createUser(testAccount.backendKey, "vince", "hi vince",
				"vince@dog.com");

		// get vince user object should succeed

		ObjectNode res2 = SpaceRequest.get("/v1/user/vince").backendKey(testAccount).basicAuth(vince).go(200)
				.objectNode();

		assertEquals(
				Json.objectBuilder().put("username", "vince").put("hashedPassword", UserUtils.hashPassword("hi vince"))
						.put("email", "vince@dog.com").array("groups").add("test").build(),
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
				.body(Json.objectBuilder().put("email", "bignose@magic.com").build().toString()).go(200);

		ObjectNode res9 = SpaceRequest.get("/v1/user/vince").backendKey(testAccount).basicAuth(vince).go(200)
				.objectNode();

		assertEquals(2, res9.get("meta").get("version").asInt());

		assertEquals(
				Json.objectBuilder().put("username", "vince").put("hashedPassword", UserUtils.hashPassword("hi vince"))
						.put("email", "bignose@magic.com").array("groups").add("test").build(),
				res9.deepCopy().without("meta"));
	}

	@Test
	public void shouldSetAndResetPassword() throws Exception {

		SpaceDogHelper.Account testAccount = SpaceDogHelper.resetTestAccount();
		SpaceDogHelper.createUser(testAccount, "toto", "hi toto", "toto@dog.com");

		// sign up without password should succeed

		String passwordResetCode = SpaceRequest.post("/v1/user/").backendKey(testAccount)
				.body(Json.objectBuilder().put("username", "titi").put("email", "titi@dog.com")).go(201)
				.getFromJson("passwordResetCode").asText();

		assertFalse(Strings.isNullOrEmpty(passwordResetCode));

		// no password user login should fail
		// I can not pass a null password anyway to the basicAuth method

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "XXX").go(401);

		// no password user trying to create password with empty reset code
		// should fail

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=").backendKey(testAccount)
				.body(new TextNode("hi titi")).go(400);

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi").go(401);

		// no password user setting password with wrong reset code should fail

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=XXX").backendKey(testAccount)
				.body(new TextNode("hi titi")).go(400);

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi").go(401);

		// titi inits its own password with right reset code should
		// succeed

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=" + passwordResetCode).backendKey(testAccount)
				.body(new TextNode("hi titi")).go(200);

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi").go(200);

		// toto user changes titi password should fail

		SpaceRequest.put("/v1/user/titi/password").backendKey(testAccount).basicAuth("toto", "hi toto")
				.body(new TextNode("XXX")).go(401);

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "XXX").go(401);

		// titi changes its password should succeed

		SpaceRequest.put("/v1/user/titi/password").backendKey(testAccount).basicAuth("titi", "hi titi")
				.body(new TextNode("hi titi 2")).go(200);

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi 2").go(200);

		// login with old password should fail

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi").go(401);

		// admin user changes titi user password should succeed

		SpaceRequest.put("/v1/user/titi/password").basicAuth("test", "hi test").body(new TextNode("hi titi 3")).go(200);
		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi 3").go(200);

		// login with old password should fail

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi 2").go(401);

		// admin deletes titi password should succeed

		String newPasswordResetCode = SpaceRequest.delete("/v1/user/titi/password").basicAuth("test", "hi test").go(200)
				.getFromJson("passwordResetCode").asText();

		// titi login should fail

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi 3").go(401);

		// titi inits its password with old reset code should fail

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=" + passwordResetCode).backendKey(testAccount)
				.body(new TextNode("hi titi")).go(400);

		// titi inits its password with new reset code should fail

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=" + newPasswordResetCode).backendKey(testAccount)
				.body(new TextNode("hi titi")).go(200);

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi").go(200);

	}

	@Test
	public void shouldSetUserCustomSchemaAndMore() throws Exception {

		SpaceDogHelper.Account testAccount = SpaceDogHelper.resetTestAccount();

		// vince sign up should succeed

		SpaceDogHelper.createUser(testAccount.backendKey, "vince", "hi vince", "vince@dog.com");
		SpaceRequest.get("/v1/data?refresh=true").backendKey(testAccount).go(200).assertEquals(1, "total");

		// update user schema with custom schema

		ObjectNode customUserSchema = UserResource.getDefaultUserSchemaBuilder()//
				.stringProperty("firstname", true)//
				.stringProperty("lastname", true)//
				.build();

		SpaceRequest.put("/v1/schema/user").basicAuth(testAccount).body(customUserSchema).go(201);

		// create new custom user

		ObjectNode fred = Json.objectBuilder().put("username", "fred")//
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
