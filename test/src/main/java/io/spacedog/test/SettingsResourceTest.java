/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.spacedog.model.SettingsSettings;
import io.spacedog.model.SettingsSettings.SettingsAcl;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json;

public class SettingsResourceTest extends SpaceTest {

	@Test
	public void createGetAndDeleteSettings() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = signUp(superadmin, "vince", "hi vince");

		ObjectNode animals = Json.object("lion", "Lion", "tiger", "Tiger");
		ObjectNode jobs = Json.object("sailor", Json.array("Sailor", "Marin"), //
				"soldier", Json.object("en", "Soldier", "fr", "Soldat"));

		// only super admins can get all settings
		guest.get("/1/settings").go(403);
		vince.get("/1/settings").go(403);
		superadmin.get("/1/settings").go(200)//
				.assertSizeEquals(0, "results");

		// get non existent settings returns NOT FOUND
		superadmin.get("/1/settings/xxx").go(404);

		// by default only super admins can create settings
		guest.put("/1/settings/animals").bodyJson(animals).go(403);
		vince.put("/1/settings/animals").bodyJson(animals).go(403);
		superadmin.put("/1/settings/animals").bodyJson(animals).go(201);

		// by default only super admins can get settings
		guest.get("/1/settings/animals").go(403);
		vince.get("/1/settings/animals").go(403);
		superadmin.get("/1/settings/animals").go(200).assertEquals(animals);

		// by default only super admins can update settings
		animals.put("puma", "Puma");
		guest.put("/1/settings/animals").bodyJson(animals).go(403);
		vince.put("/1/settings/animals").bodyJson(animals).go(403);
		superadmin.put("/1/settings/animals").bodyJson(animals).go(200);
		superadmin.get("/1/settings/animals").go(200).assertEquals(animals);

		// by default only super admins can delete settings
		guest.delete("/1/settings/animals").go(403);
		vince.delete("/1/settings/animals").go(403);
		superadmin.delete("/1/settings/animals").go(200);

		// check animals settings is deleted
		superadmin.get("/1/settings/animals").go(404);

		// super admin can create complex settings
		superadmin.put("/1/settings/jobs").bodyJson(jobs).go(201);
		superadmin.get("/1/settings/jobs").go(200).assertEquals(jobs);

		// put back animals settings
		superadmin.put("/1/settings/animals").bodyJson(animals).go(201);

		// only super admins can get all settings
		superadmin.get("/1/settings").refresh().go(200)//
				.assertSizeEquals(2)//
				.assertEquals(animals, "animals")//
				.assertEquals(jobs, "jobs");

		// settings are not data objects
		superadmin.get("/1/data").refresh().go(200)//
				.assertSizeEquals(0, "results");

		// super admin authorizes role 'key' to get/read animals settings
		// and authorizes role 'user' to the put/update/delete animals settings
		SettingsAcl acl = new SettingsAcl();
		acl.read("all");
		acl.update("user");
		SettingsSettings settings = new SettingsSettings();
		settings.put("animals", acl);
		superadmin.settings().save(settings);

		// guests can get the animals settings
		// users are still forbidden
		// superadmins are always authorized
		guest.get("/1/settings/animals").go(200).assertEquals(animals);
		vince.get("/1/settings/animals").go(200).assertEquals(animals);
		superadmin.get("/1/settings/animals").go(200).assertEquals(animals);

		// users can delete the animals settings
		// guests are still forbidden
		// super admins are always authorized
		guest.delete("/1/settings/animals").bodyJson(animals).go(403);
		vince.delete("/1/settings/animals").bodyJson(animals).go(200);

		// super admin can delete settings settings
		// to get back to previous configuration
		superadmin.settings().delete(SettingsSettings.class);

		// users can not update animals settings anymore
		// but super admins can
		vince.put("/1/settings/animals").bodyJson(animals).go(403);
		superadmin.put("/1/settings/animals").bodyJson(animals).go(201);

		// guests can not read animals settings anymore
		// but super admins can
		guest.get("/1/settings/animals").backend(superadmin).go(403);
		superadmin.get("/1/settings/animals").go(200).assertEquals(animals);
	}

	@Test
	public void checkInteralSettingsNotUpdatable() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// schema settings are not directly updatable
		superadmin.put("/1/settings/inTErnalsettings").bodyJson("XXX", "XXX").go(403);
	}

	@Test
	public void superdogCanCreateUpdateAndDeleteSettings() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog superdog = superdog(superadmin);
		ObjectNode settings = Json.object("toto", 23);

		// superdog creates test settings
		superdog.settings().save("test", settings);

		// superadmin checks test settings
		assertEquals(settings, superadmin.settings().get("test"));

		// superdog saves test settings
		settings.put("titi", false);
		superdog.settings().save("test", settings);

		// superadmin checks test settings
		assertEquals(settings, superadmin.settings().get("test"));

		// superdog deletes test settings
		superdog.settings().delete("test");

		// superadmin checks test settings are gone
		superadmin.get("/1/settings/test").go(404);
	}

	@Test
	public void getPutAndDeleteSettingsFields() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = signUp(superadmin, "vince", "hi vince");

		// only superadmins can update settings
		guest.put("/1/settings/db/type").go(403);
		vince.put("/1/settings/db/type").go(403);

		// test creates db settings with version field
		superadmin.settings().save("db", "type", TextNode.valueOf("mysql"));

		// test sets db settings version
		superadmin.settings().save("db", "version", LongNode.valueOf(12));

		// test sets db settings credentials
		superadmin.settings().save("db", "credentials", //
				Json.object("username", "tiger", "password", "miaou"));

		// test checks settings are correct
		ObjectNode settings = superadmin.settings().get("db");
		assertEquals(Json.object("type", "mysql", "version", 12, "credentials",
				Json.object("username", "tiger", "password", "miaou")), settings);

		// only superadmins can get settings
		guest.get("/1/settings/db/type").go(403);
		vince.get("/1/settings/db/type").go(403);

		// test gets each field
		assertEquals("mysql", superadmin.settings().get("db", "type").asText());
		assertEquals(12, superadmin.settings().get("db", "version").asInt());
		assertEquals(Json.object("username", "tiger", "password", "miaou"), //
				superadmin.settings().get("db", "credentials"));

		// test gets an unknown field of db settings
		assertEquals(NullNode.getInstance(), superadmin.settings().get("db", "XXX"));

		// test gets an unknown field of an unknown settings
		superadmin.get("/1/settings/XXX/YYY").go(404);

		// test updates each field
		superadmin.settings().save("db", "type", TextNode.valueOf("postgres"));
		superadmin.settings().save("db", "version", LongNode.valueOf(13));
		superadmin.settings().save("db", "credentials", //
				Json.object("username", "lion", "password", "arf"));

		// test checks settings are correct
		settings = superadmin.settings().get("db");
		assertEquals(Json.object("type", "postgres", "version", 13, "credentials",
				Json.object("username", "lion", "password", "arf")), settings);

		// only superadmins can delete settings fields
		guest.delete("/1/settings/db/type").go(403);
		vince.delete("/1/settings/db/type").go(403);

		// test deletes version
		superadmin.settings().delete("db", "version");

		// test nulls credentials
		superadmin.settings().save("db", "credentials", NullNode.getInstance());

		// test checks settings are correct
		settings = superadmin.settings().get("db");
		assertEquals(Json.object("type", "postgres", "credentials", null), settings);
	}
}
