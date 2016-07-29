/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
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
		SpaceClient.newCredentials(test, "vince", "hi vince");

		// there is no default user schema
		SpaceRequest.get("/1/schema/user").backend(test).go(404);

		// sets user schema
		SpaceClient.setSchema(//
				Schema.builder("user").text("firstname").text("lastname").build(), //
				test);

		// add user data to vince credentials
		SpaceRequest.post("/1/user?id=vince").backend(test)//
				.body("firstname", "Vincent", "lastname", "Miramond")//
				.go(201);

		SpaceRequest.get("/1/user/vince").backend(test).go(200)//
				.assertEquals("Vincent", "firstname");

		// create new custom user
		SpaceRequest.post("/1/credentials/").backend(test)//
				.body("username", "fred", "password", "hi fred", "email", "fred@dog.com")//
				.go(201);

		SpaceRequest.get("/1/login").basicAuth("test", "fred", "hi fred").go(200);

		SpaceRequest.post("/1/user?id=fred").backend(test)//
				.body("firstname", "Frédérique", "lastname", "Fallière")//
				.go(201);

		SpaceRequest.get("/1/user/fred").backend(test).go(200)//
				.assertEquals("Frédérique", "firstname");
	}
}
