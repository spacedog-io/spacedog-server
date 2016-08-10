/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.Schema;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class UserResourceTestOften extends Assert {

	@Test
	public void dataEndpointsBehaveTheSameThanUserEnpoints() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// admin sets user schema
		SpaceClient.setSchema(//
				Schema.builder("user").id("username").string("username").string("email").build(), //
				test);

		// vince signs up
		SpaceRequest.post("/1/user").backend(test)//
				.debugServer()//
				.body("username", "vince", "email", "vince@dog.com", "password", "hi vince")//
				.go(201);

		User vince = new User(test.backendId, "vince", "vince", "hi vince", "vince@dog.com");

		// fred signs up
		SpaceRequest.post("/1/user").backend(test)//
				.body("username", "fred", "email", "fred@dog.com", "password", "hi fred")//
				.go(201);

		// vince can login
		SpaceRequest.get("/1/login").userAuth(vince).go(200);

		// vince puts his data object
		SpaceRequest.put("/1/data/user/vince").userAuth(vince)//
				.body("email", "vince@dog.com").go(200);
		SpaceRequest.get("/1/login").userAuth(vince).go(200);

		// vince gets his data object
		SpaceRequest.get("/1/data/user/vince").userAuth(vince).go(200)//
				.assertEquals("vince", "username")//
				.assertEquals("vince@dog.com", "email");

		// admin gets all data and it returns vince
		SpaceRequest.get("/1/data").refresh().adminAuth(test).go(200)//
				.assertContainsValue("vince", "username")//
				.assertContainsValue("fred", "username");

		// admin gets all data of type user and it returns vince
		SpaceRequest.get("/1/data/user").adminAuth(test).go(200)//
				.assertContainsValue("vince", "username")//
				.assertContainsValue("fred", "username");

		// vince deletes his user object
		SpaceRequest.delete("/1/data/user/vince").userAuth(vince).go(200);
		SpaceRequest.get("/1/login").userAuth(vince).go(401);

		// admin deletes all objects of type user
		SpaceRequest.delete("/1/data/user").adminAuth(test).go(200)//
				.assertEquals(1, "totalDeleted");
		SpaceRequest.get("/1/data").refresh().adminAuth(test).go(200)//
				.assertSizeEquals(0, "total");
	}

	@Test
	public void userIsSigningUpAndMore() {

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// admin sets user schema
		SpaceClient.setSchema(//
				Schema.builder("user").id("username").string("username").string("email").build(), //
				test);

		// fails since invalid users

		// empty user body
		SpaceRequest.post("/1/user/").backend(test)//
				.body(Json.object()).go(400);
		// no username
		SpaceRequest.post("/1/user/").backend(test)//
				.body("password", "hi titi", "email", "titi@dog.com").go(400);
		// no email
		SpaceRequest.post("/1/user/").backend(test)//
				.body("username", "titi", "password", "hi titi").go(400);
		// username too small
		SpaceRequest.post("/1/user/").backend(test)//
				.body("username", "ti", "password", "hi titi").go(400);
		// password too small
		SpaceRequest.post("/1/user/").backend(test)//
				.body("username", "titi", "password", "hi").go(400);

		// fails to inject forged hashedPassword

		SpaceRequest.post("/1/user/").backend(test)//
				.body("username", "titi", "password", "hi titi", //
						"email", "titi@dog.com", "hashedPassword", "hi titi")
				.go(400);

		// vince signs up
		SpaceRequest.post("/1/user").backend(test)//
				.body("username", "vince", "email", "vince@dog.com", "password", "hi vince")//
				.go(201);

		User vince = new User(test.backendId, "vince", "vince", "hi vince", "vince@dog.com");

		// vince gets his user data
		ObjectNode res2 = SpaceRequest.get("/1/user/vince").userAuth(vince).go(200).objectNode();

		assertEquals(//
				Json.object("username", "vince", "email", "vince@dog.com"), //
				res2.deepCopy().without("meta"));

		// vince fails to get his user data if wrong username
		SpaceRequest.get("/1/user/vince").basicAuth(test, "XXX", "hi vince").go(401);

		// vince fails to get his user data if wrong password
		SpaceRequest.get("/1/user/vince").basicAuth(test, "vince", "XXX").go(401);

		// vince fails to get his user data if wrong backend id
		SpaceRequest.get("/1/user/vince")//
				.basicAuth("XXX", vince.username, vince.password).go(401);

		// vince succeeds to login
		SpaceRequest.get("/1/login").userAuth(vince).go(200);

		// vince fails to login if wrong password
		SpaceRequest.get("/1/login").basicAuth(test, "vince", "XXX").go(401);

		// vince updates his email
		SpaceRequest.put("/1/user/vince").userAuth(vince)//
				.body("email", "bignose@magic.com").go(200);

		SpaceRequest.get("/1/user/vince").userAuth(vince).go(200)//
				.assertEquals("vince", "username")//
				.assertEquals("bignose@magic.com", "email")//
				.assertEquals(2, "meta.version");
	}

	@Test
	public void usersCanReadOtherUsersPersonalData() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// admin sets user schema
		SpaceClient.setSchema(//
				Schema.builder("user").id("username").string("username").string("email").build(), //
				test);

		// vince signs up
		SpaceRequest.post("/1/user").backend(test)//
				.body("username", "vince", "email", "vince@dog.com", "password", "hi vince")//
				.go(201);

		// fred signs up
		SpaceRequest.post("/1/user").backend(test)//
				.body("username", "fred", "email", "fred@dog.com", "password", "hi fred")//
				.go(201);

		User fred = new User(test.backendId, "fred", "fred", "hi fred", "fred@dog.com");

		// anonymous gets vince user object
		SpaceRequest.get("/1/user/vince").backend(test).go(200);

		// anonymous fails to get all user objects
		SpaceRequest.get("/1/user").backend(test).go(403);

		// fred gets vince user object
		SpaceRequest.get("/1/user/vince").userAuth(fred).go(200);

		// fred gets all its fellow user objects
		SpaceRequest.get("/1/user").refresh().userAuth(fred).go(200)//
				.assertSizeEquals(2, "results")//
				.assertContainsValue("vince", "id")//
				.assertContainsValue("fred", "id");
	}

	@Test
	public void setAndResetPassword() {

		// prepare

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// admin sets user schema
		SpaceClient.setSchema(//
				Schema.builder("user").id("username").string("username").string("email").build(), //
				test);

		// toto signs up
		SpaceRequest.post("/1/user").backend(test)//
				.body("username", "toto", "email", "toto@dog.com", "password", "hi toto")//
				.go(201);

		// sign up without password should succeed

		String passwordResetCode = SpaceRequest.post("/1/user/")//
				.backend(test)//
				.body("username", "titi", "email", "titi@dog.com")//
				.go(201)//
				.assertNotNull("passwordResetCode")//
				.getFromJson("passwordResetCode")//
				.asText();

		// no password user login should fail
		// I can not pass a null password anyway to the basicAuth method

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "XXX").go(401);

		// no password user trying to create password with empty reset code
		// should fail

		SpaceRequest.post("/1/user/titi/password?passwordResetCode=").backend(test)//
				.formField("password", "hi titi").go(400);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(401);

		// no password user setting password with wrong reset code should fail

		SpaceRequest.post("/1/user/titi/password?passwordResetCode=XXX").backend(test)//
				.formField("password", "hi titi").go(400);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(401);

		// titi inits its own password with right reset code should succeed

		SpaceRequest.post("/1/user/titi/password?passwordResetCode=" + passwordResetCode)//
				.backend(test).formField("password", "hi titi").go(200);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(200);

		// toto user changes titi password should fail

		SpaceRequest.put("/1/user/titi/password").basicAuth(test, "toto", "hi toto")//
				.formField("password", "XXX").go(403);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "XXX").go(401);

		// titi changes its password should fail since password size < 6

		SpaceRequest.put("/1/user/titi/password").basicAuth(test, "titi", "hi titi")//
				.formField("password", "XXX").go(400);

		// titi changes its password should succeed

		SpaceRequest.put("/1/user/titi/password").basicAuth(test, "titi", "hi titi")//
				.formField("password", "hi titi 2").go(200);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi 2").go(200);

		// login with old password should fail

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(401);

		// admin user changes titi user password should succeed

		SpaceRequest.put("/1/user/titi/password").basicAuth(test, "test", "hi test")//
				.formField("password", "hi titi 3").go(200);
		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi 3").go(200);

		// login with old password should fail

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi 2").go(401);

		// admin deletes titi password should succeed

		String newPasswordResetCode = SpaceRequest.delete("/1/user/titi/password")//
				.basicAuth(test, "test", "hi test").go(200).getFromJson("passwordResetCode").asText();

		// titi login should fail

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi 3").go(401);

		// titi inits its password with old reset code should fail

		SpaceRequest.post("/1/user/titi/password?passwordResetCode=" + passwordResetCode)//
				.backend(test).formField("password", "hi titi").go(400);

		// titi inits its password with new reset code should fail

		SpaceRequest.post("/1/user/titi/password?passwordResetCode=" + newPasswordResetCode)//
				.backend(test).formField("password", "hi titi").go(200);

		SpaceRequest.get("/1/login").basicAuth(test, "titi", "hi titi").go(200);
	}

	@Test
	public void setUserCustomSchemaAndMore() {

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// admin sets user schema
		SpaceClient.setSchema(//
				Schema.builder("user").id("username").string("username").string("email").build(), //
				test);

		// vince signs up
		SpaceRequest.post("/1/user").backend(test)//
				.body("username", "vince", "email", "vince@dog.com", "password", "hi vince")//
				.go(201);

		SpaceRequest.get("/1/data").refresh().adminAuth(test).go(200)//
				.assertEquals(1, "total");

		// gets the user schema from server

		ObjectNode userSchema = SpaceRequest.get("/1/schema/user")//
				.adminAuth(test).go(200).objectNode();

		// update user schema with custom schema

		userSchema.with("user").with("firstname")//
				.put("_type", "string")//
				.put("_required", true);

		userSchema.with("user").with("lastname")//
				.put("_type", "string")//
				.put("_required", true);

		SpaceRequest.put("/1/schema/user").adminAuth(test).body(userSchema).go(200);

		// create new custom user

		ObjectNode fred = Json.object("username", "fred", "password", "hi fred", //
				"email", "fred@dog.com", "firstname", "Frédérique", "lastname", "Fallière");

		SpaceRequest.post("/1/user/").backend(test).body(fred).go(201);

		// get the brand new user and check properties are correct

		ObjectNode fredFromServer = SpaceRequest.get("/1/user/fred")//
				.adminAuth(test).go(200).objectNode();
		assertEquals(fred.without("password"), //
				fredFromServer.without(Arrays.asList("hashedPassword", "groups", "meta")));
	}
}