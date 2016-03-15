/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Backend;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder2;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class UserResourceTest extends Assert {

	@Test
	public void userIsSigningUpAndMore() throws Exception {

		SpaceDogHelper.prepareTest();
		SpaceDogHelper.Backend testAccount = SpaceDogHelper.resetTestBackend();

		// fails since invalid users

		// empty user body
		SpaceRequest.post("/v1/user/").backend(testAccount)//
				.body(Json.objectBuilder()).go(400);
		// no username
		SpaceRequest.post("/v1/user/").backend(testAccount)//
				.body(Json.object("password", "hi titi", "email", "titi@dog.com")).go(400);
		// no email
		SpaceRequest.post("/v1/user/").backend(testAccount)//
				.body(Json.object("username", "titi", "password", "hi titi")).go(400);
		// username too small
		SpaceRequest.post("/v1/user/").backend(testAccount)//
				.body(Json.object("username", "ti", "password", "hi titi")).go(400);
		// password too small
		SpaceRequest.post("/v1/user/").backend(testAccount)//
				.body(Json.object("username", "titi", "password", "hi")).go(400);

		// fails to inject forged hashedPassword

		SpaceRequest.post("/v1/user/").backend(testAccount)
				.body(//
						Json.object("username", "titi", "password", "hi titi", //
								"email", "titi@dog.com", "hashedPassword", "hi titi"))
				.go(400);

		// vince sign up should succeed

		SpaceDogHelper.User vince = SpaceDogHelper.createUser(testAccount, "vince", "hi vince", "vince@dog.com");

		// get vince user object should succeed

		ObjectNode res2 = SpaceRequest.get("/v1/user/vince").basicAuth(vince).go(200).objectNode();

		assertEquals(//
				Json.object("username", "vince", "email", "vince@dog.com"), //
				res2.deepCopy().without("meta"));

		// get data with wrong username should fail

		SpaceRequest.get("/v1/user/vince").basicAuth(testAccount, "XXX", "hi vince").go(401);

		// get data with wrong password should fail

		SpaceRequest.get("/v1/user/vince").basicAuth(testAccount, "vince", "XXX").go(401);

		// get data with wrong backend id should fail

		SpaceRequest.get("/v1/user/vince")//
				.basicAuth("XXX", vince.username, vince.password).go(401);

		// login shoud succeed

		SpaceRequest.get("/v1/login").basicAuth(vince).go(200);

		// login with wrong password should fail

		SpaceRequest.get("/v1/login").basicAuth(testAccount, "vince", "XXX").go(401);

		// email update should succeed

		SpaceRequest.put("/v1/user/vince").basicAuth(vince)//
				.body(Json.object("email", "bignose@magic.com")).go(200);

		ObjectNode res9 = SpaceRequest.get("/v1/user/vince").basicAuth(vince).go(200)//
				.assertEquals("vince", "username")//
				.assertEquals("bignose@magic.com", "email")//
				.assertEquals(2, "meta.version")//
				.objectNode();
	}

	@Test
	public void setAndResetPassword() throws Exception {

		SpaceDogHelper.prepareTest();
		SpaceDogHelper.Backend testAccount = SpaceDogHelper.resetTestBackend();
		SpaceDogHelper.createUser(testAccount, "toto", "hi toto", "toto@dog.com");

		// sign up without password should succeed

		String passwordResetCode = SpaceRequest.post("/v1/user/")//
				.backend(testAccount)//
				.body(Json.object("username", "titi", "email", "titi@dog.com"))//
				.go(201)//
				.assertNotNull("passwordResetCode")//
				.getFromJson("passwordResetCode")//
				.asText();

		// no password user login should fail
		// I can not pass a null password anyway to the basicAuth method

		SpaceRequest.get("/v1/login").basicAuth(testAccount, "titi", "XXX").go(401);

		// no password user trying to create password with empty reset code
		// should fail

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=").backend(testAccount)//
				.field("password", "hi titi").go(400);

		SpaceRequest.get("/v1/login").basicAuth(testAccount, "titi", "hi titi").go(401);

		// no password user setting password with wrong reset code should fail

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=XXX").backend(testAccount)
				.field("password", "hi titi").go(400);

		SpaceRequest.get("/v1/login").basicAuth(testAccount, "titi", "hi titi").go(401);

		// titi inits its own password with right reset code should succeed

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=" + passwordResetCode)//
				.backend(testAccount).field("password", "hi titi").go(200);

		SpaceRequest.get("/v1/login").basicAuth(testAccount, "titi", "hi titi").go(200);

		// toto user changes titi password should fail

		SpaceRequest.put("/v1/user/titi/password").basicAuth(testAccount, "toto", "hi toto")//
				.field("password", "XXX").go(401);

		SpaceRequest.get("/v1/login").basicAuth(testAccount, "titi", "XXX").go(401);

		// titi changes its password should fail since password size < 6

		SpaceRequest.put("/v1/user/titi/password").basicAuth(testAccount, "titi", "hi titi")//
				.field("password", "XXX").go(400);

		// titi changes its password should succeed

		SpaceRequest.put("/v1/user/titi/password").basicAuth(testAccount, "titi", "hi titi")
				.field("password", "hi titi 2").go(200);

		SpaceRequest.get("/v1/login").basicAuth(testAccount, "titi", "hi titi 2").go(200);

		// login with old password should fail

		SpaceRequest.get("/v1/login").basicAuth(testAccount, "titi", "hi titi").go(401);

		// admin user changes titi user password should succeed

		SpaceRequest.put("/v1/user/titi/password").basicAuth(testAccount, "test", "hi test")//
				.field("password", "hi titi 3").go(200);
		SpaceRequest.get("/v1/login").basicAuth(testAccount, "titi", "hi titi 3").go(200);

		// login with old password should fail

		SpaceRequest.get("/v1/login").basicAuth(testAccount, "titi", "hi titi 2").go(401);

		// admin deletes titi password should succeed

		String newPasswordResetCode = SpaceRequest.delete("/v1/user/titi/password")
				.basicAuth(testAccount, "test", "hi test").go(200).getFromJson("passwordResetCode").asText();

		// titi login should fail

		SpaceRequest.get("/v1/login").basicAuth(testAccount, "titi", "hi titi 3").go(401);

		// titi inits its password with old reset code should fail

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=" + passwordResetCode)//
				.backend(testAccount).field("password", "hi titi").go(400);

		// titi inits its password with new reset code should fail

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=" + newPasswordResetCode)//
				.backend(testAccount).field("password", "hi titi").go(200);

		SpaceRequest.get("/v1/login").basicAuth(testAccount, "titi", "hi titi").go(200);
	}

	@Test
	public void setUserCustomSchemaAndMore() throws Exception {

		SpaceDogHelper.prepareTest();
		Backend testAccount = SpaceDogHelper.resetTestBackend();

		// vince sign up should succeed

		SpaceDogHelper.createUser(testAccount, "vince", "hi vince", "vince@dog.com");
		SpaceRequest.get("/v1/data?refresh=true").backend(testAccount).go(200)//
				.assertEquals(2, "total");

		// update user schema with custom schema

		ObjectNode customUserSchema = getDefaultUserSchemaBuilder()//
				.stringProperty("firstname", true)//
				.stringProperty("lastname", true)//
				.build();

		SpaceRequest.put("/v1/schema/user").basicAuth(testAccount).body(customUserSchema).go(200);

		// create new custom user

		ObjectNode fred = Json.object("username", "fred", "password", "hi fred", //
				"email", "fred@dog.com", "firstname", "Frédérique", "lastname", "Fallière");

		SpaceRequest.post("/v1/user/").backend(testAccount).body(fred).go(201);

		// get the brand new user and check properties are correct

		ObjectNode fredFromServer = SpaceRequest.get("/v1/user/fred")//
				.basicAuth(testAccount).go(200).objectNode();
		assertEquals(fred.without("password"), //
				fredFromServer.without(Arrays.asList("hashedPassword", "groups", "meta")));
	}

	static final String USER_TYPE = "user";

	public static final String EMAIL = "email";
	public static final String USERNAME = "username";

	public static SchemaBuilder2 getDefaultUserSchemaBuilder() {
		return SchemaBuilder2.builder(USER_TYPE, USERNAME)//
				.stringProperty(USERNAME, true)//
				.stringProperty(EMAIL, true);
	}

	public static ObjectNode getDefaultUserSchema() {
		return getDefaultUserSchemaBuilder().build();
	}
}
