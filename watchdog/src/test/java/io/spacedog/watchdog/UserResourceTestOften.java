/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Schema;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class UserResourceTestOften extends Assert {

	@Test
	public void setUserCustomSchemaAndMore() throws Exception {

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// vince gets credentials
		User vince = SpaceClient.newCredentials(test, "vince", "hi vince");

		// there is no default user schema
		SpaceRequest.get("/1/schema/user").backend(test).go(404);

		// sets user schema
		SpaceClient.setSchema(//
				Schema.builder("user").text("firstname").text("lastname").build(), //
				test);

		// add user data to vince credentials
		SpaceRequest.post("/1/user?id=vince").userAuth(vince)//
				.body("firstname", "Vincent", "lastname", "Miramond")//
				.go(201);

		SpaceRequest.get("/1/user/vince").backend(test).go(200)//
				.assertEquals("Vincent", "firstname");

		// create new custom user
		User fred = SpaceClient.newCredentials(test, "fred", "hi fred");
		SpaceRequest.get("/1/login").userAuth(fred).go(200);

		SpaceRequest.post("/1/user?id=fred").userAuth(fred)//
				.body("firstname", "Frédérique", "lastname", "Fallière")//
				.go(201);

		SpaceRequest.get("/1/user/fred").backend(test).go(200)//
				.assertEquals("Frédérique", "firstname");
	}
}
