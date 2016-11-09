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
public class SettingsResourceTestOften extends Assert {

	@Test
	public void createGetAndDeleteSettings() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		User vince = SpaceClient.signUp(test, "vince", "hi vince");

		ObjectNode animals = Json.object("lion", "Lion", "tiger", "Tiger");
		ObjectNode jobs = Json.object("sailor", Json.array("Sailor", "Marin"), //
				"soldier", Json.object("en", "Soldier", "fr", "Soldat"));

		// everybody can get all settings
		SpaceRequest.get("/1/settings").backend(test).go(200)//
				.assertSizeEquals(0, "results");

		// get non existent settings returns NOT FOUND
		SpaceRequest.get("/1/settings/xxx").backend(test).go(404);

		// only admins can create/update settings
		SpaceRequest.put("/1/settings/animals").backend(test).body(animals).go(403);
		SpaceRequest.put("/1/settings/animals").userAuth(vince).body(animals).go(403);
		SpaceRequest.put("/1/settings/animals").adminAuth(test).body(animals).go(201);

		// everybody can get any settings
		SpaceRequest.get("/1/settings/animals").backend(test).go(200).assertEquals(animals);
		SpaceRequest.get("/1/settings/animals").userAuth(vince).go(200).assertEquals(animals);
		SpaceRequest.get("/1/settings/animals").adminAuth(test).go(200).assertEquals(animals);

		// only admins can update settings
		animals.put("puma", "Puma");
		SpaceRequest.put("/1/settings/animals").backend(test).body(animals).go(403);
		SpaceRequest.put("/1/settings/animals").userAuth(vince).body(animals).go(403);
		SpaceRequest.put("/1/settings/animals").adminAuth(test).body(animals).go(200);
		SpaceRequest.get("/1/settings/animals").backend(test).go(200).assertEquals(animals);

		// only admins can delete settings
		SpaceRequest.delete("/1/settings/animals").backend(test).go(403);
		SpaceRequest.delete("/1/settings/animals").userAuth(vince).go(403);
		SpaceRequest.delete("/1/settings/animals").adminAuth(test).go(200);
		SpaceRequest.get("/1/settings/animals").backend(test).go(404);

		// admin can create complex settings
		SpaceRequest.put("/1/settings/jobs").adminAuth(test).body(jobs).go(201);
		SpaceRequest.get("/1/settings/jobs").backend(test).go(200).assertEquals(jobs);

		// put back animals settings
		SpaceRequest.put("/1/settings/animals").adminAuth(test).body(animals).go(201);

		// everybody can get all settings
		SpaceRequest.get("/1/settings").refresh().backend(test).go(200)//
				.assertSizeEquals(2)//
				.assertEquals(animals, "animals")//
				.assertEquals(jobs, "jobs");

		// settings are not data objects
		SpaceRequest.get("/1/data").refresh().adminAuth(test).go(200)//
				.assertSizeEquals(0, "results");
	}
}
