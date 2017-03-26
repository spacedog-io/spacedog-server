/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.spacedog.model.SchemaSettings;
import io.spacedog.model.SettingsSettings;
import io.spacedog.model.SettingsSettings.SettingsAcl;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json7;

public class SettingsResourceTest extends SpaceTest {

	@Test
	public void createGetAndDeleteSettings() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog vince = signUp(test, "vince", "hi vince");

		ObjectNode animals = Json7.object("lion", "Lion", "tiger", "Tiger");
		ObjectNode jobs = Json7.object("sailor", Json7.array("Sailor", "Marin"), //
				"soldier", Json7.object("en", "Soldier", "fr", "Soldat"));

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
		test.settings().save(settings);

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
		test.settings().delete(SettingsSettings.class);

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
		SpaceDog test = resetTestBackend();

		// schema settings are not directly updatable
		SpaceRequest.put("/1/settings/schema")//
				.adminAuth(test).bodySettings(new SchemaSettings()).go(400);
	}

	@Test
	public void superdogCanCreateUpdateAndDeleteSettings() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog superdog = superdog(test);
		ObjectNode settings = Json7.object("toto", 23);

		// superdog creates test settings
		superdog.settings().save("test", settings);

		// superadmin checks test settings
		assertEquals(settings, test.settings().get("test"));

		// superdog saves test settings
		settings.put("titi", false);
		superdog.settings().save("test", settings);

		// superadmin checks test settings
		assertEquals(settings, test.settings().get("test"));

		// superdog deletes test settings
		superdog.settings().delete("test");

		// superadmin checks test settings are gone
		test.get("/1/settings/test").go(404);
	}

	@Test
	public void getPutAndDeleteSettingsFields() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(test);
		SpaceDog vince = signUp(test, "vince", "hi vince");

		// only superadmins can update settings
		guest.put("/1/settings/db/type").go(403);
		vince.put("/1/settings/db/type").go(403);

		// test creates db settings with version field
		test.settings().save("db", "type", TextNode.valueOf("mysql"));

		// test sets db settings version
		test.settings().save("db", "version", LongNode.valueOf(12));

		// test sets db settings credentials
		test.settings().save("db", "credentials", //
				Json7.object("username", "tiger", "password", "miaou"));

		// test checks settings are correct
		ObjectNode settings = test.settings().get("db");
		assertEquals(Json7.object("type", "mysql", "version", 12, "credentials",
				Json7.object("username", "tiger", "password", "miaou")), settings);

		// only superadmins can get settings
		guest.get("/1/settings/db/type").go(403);
		vince.get("/1/settings/db/type").go(403);

		// test gets each field
		assertEquals("mysql", test.settings().get("db", "type").asText());
		assertEquals(12, test.settings().get("db", "version").asInt());
		assertEquals(Json7.object("username", "tiger", "password", "miaou"), //
				test.settings().get("db", "credentials"));

		// test gets an unknown field of db settings
		assertEquals(NullNode.getInstance(), test.settings().get("db", "XXX"));

		// test gets an unknown field of an unknown settings
		test.get("/1/settings/XXX/YYY").go(404);

		// test updates each field
		test.settings().save("db", "type", TextNode.valueOf("postgres"));
		test.settings().save("db", "version", LongNode.valueOf(13));
		test.settings().save("db", "credentials", //
				Json7.object("username", "lion", "password", "arf"));

		// test checks settings are correct
		settings = test.settings().get("db");
		assertEquals(Json7.object("type", "postgres", "version", 13, "credentials",
				Json7.object("username", "lion", "password", "arf")), settings);

		// only superadmins can delete settings fields
		guest.delete("/1/settings/db/type").go(403);
		vince.delete("/1/settings/db/type").go(403);

		// test deletes version
		test.settings().delete("db", "version");

		// test nulls credentials
		test.settings().save("db", "credentials", NullNode.getInstance());

		// test checks settings are correct
		settings = test.settings().get("db");
		assertEquals(Json7.object("type", "postgres", "credentials", null), settings);
	}
}
