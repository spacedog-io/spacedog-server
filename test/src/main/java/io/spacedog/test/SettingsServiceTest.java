/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceTest;
import io.spacedog.model.Permission;
import io.spacedog.model.SettingsAclSettings;
import io.spacedog.utils.Json;

public class SettingsServiceTest extends SpaceTest {

	@Test
	public void createGetAndDeleteSettings() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin.backend());
		SpaceDog vince = createTempDog(superadmin, "vince");

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

		// superadmin authorizes all to get/read animals settings
		// and authorizes role 'user' to put/update/delete animals settings
		SettingsAclSettings settings = new SettingsAclSettings();
		settings.put("animals", "all", Permission.read);
		settings.put("animals", "user", Permission.update);
		superadmin.settings().save(settings);

		// all can get the animals settings
		guest.get("/1/settings/animals").go(200).assertEquals(animals);
		vince.get("/1/settings/animals").go(200).assertEquals(animals);
		superadmin.get("/1/settings/animals").go(200).assertEquals(animals);

		// users can delete animals settings
		// guests are still forbidden
		// superadmins are always authorized
		guest.delete("/1/settings/animals").bodyJson(animals).go(403);
		vince.delete("/1/settings/animals").bodyJson(animals).go(200);

		// superadmin can delete settings acl settings
		// to get back to previous configuration
		superadmin.settings().delete(SettingsAclSettings.class);

		// users can not update animals settings anymore
		// but superadmins can
		vince.put("/1/settings/animals").bodyJson(animals).go(403);
		superadmin.put("/1/settings/animals").bodyJson(animals).go(201);

		// guests can not read animals settings anymore
		// but superadmins can
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
		SpaceDog guest = SpaceDog.backend(superadmin.backend());
		SpaceDog vince = createTempDog(superadmin, "vince");

		// only superadmins can update settings
		guest.put("/1/settings/db/type").go(403);
		vince.put("/1/settings/db/type").go(403);

		// superadmin creates db settings with version field
		superadmin.settings().save("db", "type", TextNode.valueOf("mysql"));

		// superadmin sets db settings version
		superadmin.settings().save("db", "version", LongNode.valueOf(12));

		// superadmin sets db settings credentials
		superadmin.settings().save("db", "credentials", //
				Json.object("username", "tiger", "password", "miaou"));

		// superadmin checks settings are correct
		ObjectNode settings = superadmin.settings().get("db");
		assertEquals(Json.object("type", "mysql", "version", 12, "credentials",
				Json.object("username", "tiger", "password", "miaou")), settings);

		// only superadmins can get settings
		guest.get("/1/settings/db/type").go(403);
		vince.get("/1/settings/db/type").go(403);

		// superadmin gets each field
		assertEquals("mysql", superadmin.settings().get("db", "type").asText());
		assertEquals(12, superadmin.settings().get("db", "version").asInt());
		assertEquals(Json.object("username", "tiger", "password", "miaou"), //
				superadmin.settings().get("db", "credentials"));

		// superadmin gets an unknown field of db settings
		assertEquals(NullNode.getInstance(), superadmin.settings().get("db", "XXX"));

		// superadmin gets an unknown field of an unknown settings
		superadmin.get("/1/settings/XXX/YYY").go(404);

		// superadmin updates each field
		superadmin.settings().save("db", "type", TextNode.valueOf("postgres"));
		superadmin.settings().save("db", "version", LongNode.valueOf(13));
		superadmin.settings().save("db", "credentials", //
				Json.object("username", "lion", "password", "arf"));

		// superadmin checks settings are correct
		settings = superadmin.settings().get("db");
		assertEquals(Json.object("type", "postgres", "version", 13, "credentials",
				Json.object("username", "lion", "password", "arf")), settings);

		// only superadmins can delete settings fields
		guest.delete("/1/settings/db/type").go(403);
		vince.delete("/1/settings/db/type").go(403);

		// superadmin deletes version
		superadmin.settings().delete("db", "version");

		// superadmin nulls credentials
		superadmin.settings().save("db", "credentials", NullNode.getInstance());

		// superadmin checks settings are correct
		settings = superadmin.settings().get("db");
		assertEquals(Json.object("type", "postgres", "credentials", null), settings);
	}
}
