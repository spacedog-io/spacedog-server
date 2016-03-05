/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.TextNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceDogHelper.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder2;

public class BackendResourceTest extends Assert {

	@Test
	public void testBackend() throws Exception {

		SpaceDogHelper.prepareTest("testz");
		Account admin = SpaceDogHelper.resetBackend("testz", "testz", "hi testz");

		// get all backend names
		SpaceRequest.get("/v1/backend").superdogAuth().go(200)//
				.assertContains(TextNode.valueOf("testz"), "results");

		// set a message schema
		SpaceDogHelper.setSchema(//
				SchemaBuilder2.builder("message")//
						.textProperty("text", "english", true).build(),
				admin);

		// should successfully create 4 messages and 1 user

		User riri = SpaceDogHelper.createUser((String) null, "riri", "hi riri", "riri@dog.com");

		SpaceRequest.post("/v1/data/message").basicAuth(riri)//
				.body(Json.object("text", "what's up?")).go(201);
		SpaceRequest.post("/v1/data/message").basicAuth(riri)//
				.body(Json.object("text", "wanna drink something?")).go(201);
		SpaceRequest.post("/v1/data/message").basicAuth(riri)//
				.body(Json.object("text", "pretty cool, hein?")).go(201);
		SpaceRequest.post("/v1/data/message").basicAuth(riri)//
				.body(Json.object("text", "so long guys")).go(201);

		SpaceRequest.get("/v1/data?refresh=true").go(200).assertEquals(6, "total");

		// should succeed to delete all users
		SpaceRequest.delete("/v1/user/riri").go(401);
		SpaceRequest.delete("/v1/user/riri").basicAuth(riri).go(200);
	}
}
