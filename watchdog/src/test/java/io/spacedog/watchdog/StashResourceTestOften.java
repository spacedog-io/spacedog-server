/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class StashResourceTestOften extends Assert {

	@Test
	public void createGetAndDeleteStashObjects() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		SpaceClient.initUserDefaultSchema(test);
		User vince = SpaceClient.createUser(test, "vince", "hi vince");

		ObjectNode animals = Json.object("lion", "Lion", "tiger", "Tiger");
		ObjectNode jobs = Json.object("sailor", Json.array("Sailor", "Marin"), //
				"soldier", Json.object("en", "Soldier", "fr", "Soldat"));

		// use stash endpoint before stash index is created returns 404
		SpaceRequest.get("/1/stash").adminAuth(test).go(404);
		SpaceRequest.get("/1/stash/animals").adminAuth(test).go(404);
		SpaceRequest.put("/1/stash/animals").adminAuth(test).body(animals).go(404);
		SpaceRequest.delete("/1/stash/animals").adminAuth(test).go(404);

		// admin creates a stash index in test backend
		SpaceRequest.put("/1/stash").adminAuth(test).go(200);

		// get non existent stash objects returns NOT FOUND
		SpaceRequest.get("/1/stash/animals").backend(test).go(404);

		// only admin are allowed to create stash objects
		SpaceRequest.put("/1/stash/animals").backend(test).body(animals).go(401);
		SpaceRequest.put("/1/stash/animals").userAuth(vince).body(animals).go(401);
		SpaceRequest.put("/1/stash/animals").adminAuth(test).body(animals).go(201);

		// anonymous gets stash objects
		SpaceRequest.get("/1/stash/animals").backend(test).go(200).assertEquals(animals);

		// only admin can update stash objects
		animals.put("puma", "Puma");
		SpaceRequest.put("/1/stash/animals").backend(test).body(animals).go(401);
		SpaceRequest.put("/1/stash/animals").userAuth(vince).body(animals).go(401);
		SpaceRequest.put("/1/stash/animals").adminAuth(test).body(animals).go(200);
		SpaceRequest.get("/1/stash/animals").backend(test).go(200).assertEquals(animals);

		// admin can create complex stash objects
		SpaceRequest.put("/1/stash/jobs").adminAuth(test).body(jobs).go(201);
		SpaceRequest.get("/1/stash/jobs").backend(test).go(200).assertEquals(jobs);

		// only admin gets all stash objects
		SpaceRequest.get("/1/stash").backend(test).go(401);
		SpaceRequest.get("/1/stash").userAuth(vince).go(401);
		SpaceRequest.get("/1/stash").refresh(true).adminAuth(test).go(200)//
				.assertSizeEquals(2, "results")//
				.assertEquals(animals, "results.0")//
				.assertEquals(jobs, "results.1");

		// stash objects are also data objects
		SpaceRequest.get("/1/data").refresh(true).backend(test).go(200)//
				.assertSizeEquals(3, "results")//
				.assertContainsValue("animals", "id")//
				.assertContainsValue("jobs", "id")//
				.assertContainsValue("vince", "id");

		// only admin deletes stash objects
		SpaceRequest.delete("/1/stash/jobs").backend(test).go(401);
		SpaceRequest.delete("/1/stash/jobs").userAuth(vince).go(401);
		SpaceRequest.delete("/1/stash/jobs").adminAuth(test).go(200);
		SpaceRequest.delete("/1/stash/animals").adminAuth(test).go(200);

		// delete non existent object returns NOT FOUND
		SpaceRequest.delete("/1/stash/jobs").adminAuth(test).go(404);
		SpaceRequest.delete("/1/stash/animals").adminAuth(test).go(404);
	}
}
