/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder2;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class UserResourceTest extends Assert {

	@Test
	public void userIsSigningUpAndMore() throws Exception {

		SpaceDogHelper.prepareTest();
		SpaceDogHelper.Account testAccount = SpaceDogHelper.resetTestAccount();

		// fails since invalid users

		// empty user body
		SpaceRequest.post("/v1/user/").backendKey(testAccount).body(Json.objectBuilder()).go(400);
		// no username
		SpaceRequest.post("/v1/user/").backendKey(testAccount).body(//
				Json.objectBuilder().put("password", "hi titi").put("email", "titi@dog.com")).go(400);
		// no email
		SpaceRequest.post("/v1/user/").backendKey(testAccount).body(//
				Json.objectBuilder().put("username", "titi").put("password", "hi titi")).go(400);
		// username too small
		SpaceRequest.post("/v1/user/").backendKey(testAccount).body(//
				Json.objectBuilder().put("username", "ti").put("password", "hi titi")).go(400);
		// password too small
		SpaceRequest.post("/v1/user/").backendKey(testAccount).body(//
				Json.objectBuilder().put("username", "titi").put("password", "hi")).go(400);

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

		assertEquals(//
				Json.object("username", "vince", "email", "vince@dog.com"), //
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

		assertEquals(//
				Json.object("username", "vince", "email", "bignose@magic.com"), //
				res9.deepCopy().without("meta"));
	}

	@Test
	public void setAndResetPassword() throws Exception {

		SpaceDogHelper.prepareTest();
		SpaceDogHelper.Account testAccount = SpaceDogHelper.resetTestAccount();
		SpaceDogHelper.createUser(testAccount, "toto", "hi toto", "toto@dog.com");

		// sign up without password should succeed

		String passwordResetCode = SpaceRequest.post("/v1/user/")//
				.backendKey(testAccount)//
				.body(Json.objectBuilder().put("username", "titi").put("email", "titi@dog.com"))//
				.go(201)//
				.assertNotNull("passwordResetCode")//
				.getFromJson("passwordResetCode")//
				.asText();

		// no password user login should fail
		// I can not pass a null password anyway to the basicAuth method

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "XXX").go(401);

		// no password user trying to create password with empty reset code
		// should fail

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=").backendKey(testAccount)
				.field("password", "hi titi").go(400);

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi").go(401);

		// no password user setting password with wrong reset code should fail

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=XXX").backendKey(testAccount)
				.field("password", "hi titi").go(400);

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi").go(401);

		// titi inits its own password with right reset code should succeed

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=" + passwordResetCode).backendKey(testAccount)
				.field("password", "hi titi").go(200);

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi").go(200);

		// toto user changes titi password should fail

		SpaceRequest.put("/v1/user/titi/password").backendKey(testAccount).basicAuth("toto", "hi toto")
				.field("password", "XXX").go(401);

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "XXX").go(401);

		// titi changes its password should fail since password size < 6

		SpaceRequest.put("/v1/user/titi/password").backendKey(testAccount).basicAuth("titi", "hi titi")
				.field("password", "XXX").go(400);

		// titi changes its password should succeed

		SpaceRequest.put("/v1/user/titi/password").backendKey(testAccount).basicAuth("titi", "hi titi")
				.field("password", "hi titi 2").go(200);

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi 2").go(200);

		// login with old password should fail

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi").go(401);

		// admin user changes titi user password should succeed

		SpaceRequest.put("/v1/user/titi/password").basicAuth("test", "hi test")//
				.field("password", "hi titi 3").go(200);
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
				.field("password", "hi titi").go(400);

		// titi inits its password with new reset code should fail

		SpaceRequest.post("/v1/user/titi/password?passwordResetCode=" + newPasswordResetCode).backendKey(testAccount)
				.field("password", "hi titi").go(200);

		SpaceRequest.get("/v1/login").backendKey(testAccount).basicAuth("titi", "hi titi").go(200);
	}

	@Test
	public void setUserCustomSchemaAndMore() throws Exception {

		SpaceDogHelper.prepareTest();
		SpaceDogHelper.Account testAccount = SpaceDogHelper.resetTestAccount();

		// vince sign up should succeed

		SpaceDogHelper.createUser(testAccount.backendKey, "vince", "hi vince", "vince@dog.com");
		SpaceRequest.get("/v1/data?refresh=true").backendKey(testAccount).go(200).assertEquals(1, "total");

		// update user schema with custom schema

		ObjectNode customUserSchema = getDefaultUserSchemaBuilder()//
				.stringProperty("firstname", true)//
				.stringProperty("lastname", true)//
				.build();

		SpaceRequest.put("/v1/schema/user").basicAuth(testAccount).body(customUserSchema).go(200);

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

	static final String USER_TYPE = "user";

	public static final String ACCOUNT_ID = "accountId";
	public static final String GROUPS = "groups";
	public static final String EMAIL = "email";
	public static final String USERNAME = "username";
	public static final String HASHED_PASSWORD = "hashedPassword";
	public static final String PASSWORD_RESET_CODE = "passwordResetCode";
	public static final String ENDPOINT_ARN = "endpointArn";

	public static SchemaBuilder2 getDefaultUserSchemaBuilder() {
		return SchemaBuilder2.builder(USER_TYPE, USERNAME)//
				.stringProperty(USERNAME, true)//
				.stringProperty(HASHED_PASSWORD, false)//
				.stringProperty(PASSWORD_RESET_CODE, false)//
				.stringProperty(EMAIL, true)//
				.stringProperty(ACCOUNT_ID, true)//
				.stringProperty(GROUPS, false, true)//
				.stringProperty(ENDPOINT_ARN, false, false);
	}

	public static ObjectNode getDefaultUserSchema() {
		return getDefaultUserSchemaBuilder().build();
	}

}
