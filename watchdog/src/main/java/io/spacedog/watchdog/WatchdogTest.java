/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequest;
import io.spacedog.utils.Json;

public class WatchdogTest {

	@Test
	public void userIsSigningUpAndMore() {

		// prepare
		// prepareTest();
		SpaceDog superadmin = null; // resetTestBackend();

		superadmin.credentials().enableGuestSignUp(true);

		// fails since empty user body
		SpaceRequest.post("/1/credentials/").backend(superadmin)//
				.bodyJson(Json.object()).go(400);

		// fails since no username
		SpaceRequest.post("/1/credentials/").backend(superadmin)//
				.bodyJson("password", "hi titi", "email", "titi@dog.com").go(400);

		// fails since no email
		SpaceRequest.post("/1/credentials/").backend(superadmin)//
				.bodyJson("username", "titi", "password", "hi titi").go(400);

		// fails since username too small
		SpaceRequest.post("/1/credentials/").backend(superadmin)//
				.bodyJson("username", "ti", "password", "hi titi").go(400);

		// fails since password too small
		SpaceRequest.post("/1/credentials/").backend(superadmin)//
				.bodyJson("username", "titi", "password", "hi", "email", "titi@dog.com").go(400);

		// vince signs up
		String vinceId = SpaceRequest.post("/1/credentials").backend(superadmin)
				.bodyJson("username", "vince", "password", "hi vince", "email", "vince@dog.com")//
				.go(201).getString("id");

		// vince fails to sign up again since his credentials already exixts
		SpaceRequest.post("/1/credentials").backend(superadmin)
				.bodyJson("username", "vince", "password", "hello boby", "email", "vince@dog.com")//
				.go(400)//
				.assertEquals("already-exists", "error.code");

		// vince logs in
		ObjectNode node = SpaceRequest.get("/1/login")//
				.backend(superadmin).basicAuth("vince", "hi vince").go(200)//
				.assertPresent("accessToken")//
				.assertPresent("expiresIn")//
				.assertEquals(vinceId, "credentials.id")//
				.assertEquals("vince", "credentials.username")//
				.assertEquals("vince@dog.com", "credentials.email")//
				.assertEquals(true, "credentials.enabled")//
				.assertSizeEquals(1, "credentials.roles")//
				.assertEquals("user", "credentials.roles.0")//
				.assertPresent("credentials.createdAt")//
				.assertPresent("credentials.updatedAt")//
				.asJsonObject();

		SpaceDog vince = SpaceDog.backendId("test").username("vince")//
				.id(vinceId).password("hi vince").email("vince@dog.com")//
				.accessToken(node.get("accessToken").asText()) //
				.expiresAt(DateTime.now().plus(node.get("expiresIn").asLong()));

		// vince gets his credentials
		vince.get("/1/credentials/" + vince.id()).go(200)//
				.assertEquals("vince", "username")//
				.assertEquals("vince@dog.com", "email")//
				.assertSizeEquals(1, "roles")//
				.assertDateIsRecent("createdAt")//
				.assertDateIsRecent("updatedAt");

		// vince fails to get his credentials if wrong username
		SpaceRequest.get("/1/credentials/" + vince.id())//
				.backend(superadmin).basicAuth("XXX", "hi vince").go(401);

		// vince fails to get his credentials if wrong password
		SpaceRequest.get("/1/credentials/" + vince.id())//
				.backend(superadmin).basicAuth("vince", "XXX").go(401);

		// vince fails to get his credentials if wrong backend id
		vince.get("/1/credentials/" + vince.id()).backend("XXX").go(401);

		// anonymous fails to get vince credentials
		SpaceRequest.get("/1/credentials/" + vince.id()).backend(superadmin).go(403);

		// another user fails to get vince credentials
		SpaceDog fred = null;// signUpTempDog("test", "fred");
		fred.get("/1/credentials/" + vince.id()).go(403);

		// vince succeeds to login
		vince.login();

		// vince fails to login if wrong password
		SpaceRequest.get("/1/login").backend(superadmin).basicAuth("vince", "XXX").go(401);
	}
}
