/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTest;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaSettings;
import io.spacedog.utils.SettingsSettings;
import io.spacedog.utils.SettingsSettings.SettingsAcl;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class SettingsResourceTestOften extends SpaceTest {

	@Test
	public void createGetAndDeleteSettings() {

		// prepare
		prepareTest();
		Backend test = resetTestBackend();
		User vince = signUp(test, "vince", "hi vince");

		ObjectNode animals = Json.object("lion", "Lion", "tiger", "Tiger");
		ObjectNode jobs = Json.object("sailor", Json.array("Sailor", "Marin"), //
				"soldier", Json.object("en", "Soldier", "fr", "Soldat"));

		// only super admins can get all settings
		SpaceRequest.get("/1/settings").backend(test).go(403);
		SpaceRequest.get("/1/settings").userAuth(vince).go(403);
		SpaceRequest.get("/1/settings").adminAuth(test).go(200)//
				.assertSizeEquals(0, "results");

		// get non existent settings returns NOT FOUND
		SpaceRequest.get("/1/settings/xxx").adminAuth(test).go(404);

		// by default only super admins can create settings
		SpaceRequest.put("/1/settings/animals").backend(test).body(animals).go(403);
		SpaceRequest.put("/1/settings/animals").userAuth(vince).body(animals).go(403);
		SpaceRequest.put("/1/settings/animals").adminAuth(test).body(animals).go(201);

		// by default only super admins can get settings
		SpaceRequest.get("/1/settings/animals").backend(test).go(403);
		SpaceRequest.get("/1/settings/animals").userAuth(vince).go(403);
		SpaceRequest.get("/1/settings/animals").adminAuth(test).go(200).assertEquals(animals);

		// by default only super admins can update settings
		animals.put("puma", "Puma");
		SpaceRequest.put("/1/settings/animals").backend(test).body(animals).go(403);
		SpaceRequest.put("/1/settings/animals").userAuth(vince).body(animals).go(403);
		SpaceRequest.put("/1/settings/animals").adminAuth(test).body(animals).go(200);
		SpaceRequest.get("/1/settings/animals").adminAuth(test).go(200).assertEquals(animals);

		// by default only super admins can delete settings
		SpaceRequest.delete("/1/settings/animals").backend(test).go(403);
		SpaceRequest.delete("/1/settings/animals").userAuth(vince).go(403);
		SpaceRequest.delete("/1/settings/animals").adminAuth(test).go(200);

		// check animals settings is deleted
		SpaceRequest.get("/1/settings/animals").adminAuth(test).go(404);

		// super admin can create complex settings
		SpaceRequest.put("/1/settings/jobs").adminAuth(test).body(jobs).go(201);
		SpaceRequest.get("/1/settings/jobs").adminAuth(test).go(200).assertEquals(jobs);

		// put back animals settings
		SpaceRequest.put("/1/settings/animals").adminAuth(test).body(animals).go(201);

		// only super admins can get all settings
		SpaceRequest.get("/1/settings").refresh().adminAuth(test).go(200)//
				.assertSizeEquals(2)//
				.assertEquals(animals, "animals")//
				.assertEquals(jobs, "jobs");

		// settings are not data objects
		SpaceRequest.get("/1/data").refresh().adminAuth(test).go(200)//
				.assertSizeEquals(0, "results");

		// super admin authorizes role 'key' to get/read animals settings
		// and authorizes role 'user' to the put/update/delete animals settings
		SettingsAcl acl = new SettingsAcl();
		acl.read("key");
		acl.update("user");
		SettingsSettings settings = new SettingsSettings();
		settings.put("animals", acl);
		saveSettings(test, settings);

		// guests can get the animals settings
		// users are still forbidden
		// super admins are always authorized
		SpaceRequest.get("/1/settings/animals").userAuth(vince).go(403);
		SpaceRequest.get("/1/settings/animals").backend(test).go(200).assertEquals(animals);
		SpaceRequest.get("/1/settings/animals").adminAuth(test).go(200).assertEquals(animals);

		// users can delete the animals settings
		// guests are still forbidden
		// super admins are always authorized
		SpaceRequest.delete("/1/settings/animals").backend(test).body(animals).go(403);
		SpaceRequest.delete("/1/settings/animals").userAuth(vince).body(animals).go(200);

		// super admin can delete settings settings
		// to get back to previous configuration
		deleteSettings(test, SettingsSettings.class);

		// users can not update animals settings anymore
		// but super admins can
		SpaceRequest.put("/1/settings/animals").userAuth(vince).body(animals).go(403);
		SpaceRequest.put("/1/settings/animals").adminAuth(test).body(animals).go(201);

		// guests can not read animals settings anymore
		// but super admins can
		SpaceRequest.get("/1/settings/animals").backend(test).go(403);
		SpaceRequest.get("/1/settings/animals").adminAuth(test).go(200).assertEquals(animals);
	}

	@Test
	public void checkNonDirectlyUpdatableSettingsAreNotUpdatable() {

		// prepare
		prepareTest();
		Backend test = resetTestBackend();

		// schema settings are not directly updatable
		SpaceRequest.put("/1/settings/schema")//
				.adminAuth(test).bodySettings(new SchemaSettings()).go(400);
	}
}
