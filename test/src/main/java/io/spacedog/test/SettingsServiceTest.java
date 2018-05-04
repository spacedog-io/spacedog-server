/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.ObjectNodeWrap.Results;
import io.spacedog.client.settings.SettingsAclSettings;
import io.spacedog.utils.Json;

public class SettingsServiceTest extends SpaceTest {

	@Test
	public void createGetAndDeleteSettings() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");

		ObjectNode animals = Json.object("lion", "Lion", "tiger", "Tiger");
		ObjectNode jobs = Json.object("sailor", Json.array("Sailor", "Marin"), //
				"soldier", Json.object("en", "Soldier", "fr", "Soldat"));

		// only superadmins can get all settings
		assertHttpError(401, () -> guest.settings().getAll());
		assertHttpError(403, () -> vince.settings().getAll());
		assertEquals(0, superadmin.settings().getAll().size());

		// get non existent settings returns NOT FOUND
		assertHttpError(404, () -> superadmin.settings().get("xxx"));

		// by default only superadmins can create settings
		assertHttpError(401, () -> guest.settings().save("animals", animals));
		assertHttpError(403, () -> vince.settings().save("animals", animals));
		superadmin.settings().save("animals", animals);

		// by default only superadmins can get settings
		assertHttpError(401, () -> guest.settings().get("animals"));
		assertHttpError(403, () -> vince.settings().get("animals"));
		assertEquals(animals, superadmin.settings().get("animals"));

		// by default only superadmins can update settings
		animals.put("puma", "Puma");
		assertHttpError(401, () -> guest.settings().save("animals", animals));
		assertHttpError(403, () -> vince.settings().save("animals", animals));
		superadmin.settings().save("animals", animals);
		assertEquals(animals, superadmin.settings().get("animals"));

		// by default only superadmins can delete settings
		assertHttpError(401, () -> guest.settings().delete("animals"));
		assertHttpError(403, () -> vince.settings().delete("animals"));
		superadmin.settings().delete("animals");

		// check animals settings is deleted
		assertHttpError(404, () -> superadmin.settings().get("animals"));

		// superadmin can create complex settings
		superadmin.settings().save("jobs", jobs);
		assertEquals(jobs, superadmin.settings().get("jobs"));

		// put back animals settings
		superadmin.settings().save("animals", animals);

		// only superadmins can get all settings
		ObjectNode allSettings = superadmin.settings().getAll(true);
		assertEquals(2, allSettings.size());
		assertEquals(animals, allSettings.get("animals"));
		assertEquals(jobs, allSettings.get("jobs"));

		// settings are not data objects

		Results results = superadmin.data().getAllRequest().refresh().go();
		assertEquals(0, results.total);

		// superadmin authorizes all to get/read animals settings
		// and authorizes role 'user' to put/update/delete animals settings
		SettingsAclSettings settings = new SettingsAclSettings();
		settings.put("animals", Roles.all, Permission.read);
		settings.put("animals", Roles.user, Permission.update);
		superadmin.settings().save(settings);

		// all can get the animals settings
		assertEquals(animals, guest.settings().get("animals"));
		assertEquals(animals, vince.settings().get("animals"));
		assertEquals(animals, superadmin.settings().get("animals"));

		// users can delete animals settings
		// guests are still forbidden
		// superadmins are always authorized
		assertHttpError(401, () -> guest.settings().delete("animals"));
		vince.settings().delete("animals");

		// superadmin can delete settings acl settings
		// to get back to previous configuration
		superadmin.settings().delete(SettingsAclSettings.class);

		// users can not update animals settings anymore
		// but superadmins can
		assertHttpError(403, () -> vince.settings().save("animals", animals));
		superadmin.settings().save("animals", animals);

		// guests can not read animals settings anymore
		// but superadmins can
		assertHttpError(401, () -> guest.settings().get("animals"));
		assertEquals(animals, superadmin.settings().get("animals"));
	}

	@Test
	public void checkInteralSettingsNotUpdatable() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();

		// schema settings are not directly updatable
		assertHttpError(403, () -> superadmin.settings().save(//
				"inTErnalsettings", Json.object("XXX", "XXX")));
	}

	@Test
	public void superdogCanCreateUpdateAndDeleteSettings() {

		// prepare
		prepareTest();
		SpaceDog superdog = superdog();
		SpaceDog superadmin = clearRootBackend();
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
		assertHttpError(404, () -> superadmin.settings().get("test"));
	}

	@Test
	public void getPutAndDeleteSettingsFields() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");

		// only superadmins can update settings
		assertHttpError(401, () -> guest.settings().save("db", "type", "mysql"));
		assertHttpError(403, () -> vince.settings().save("db", "type", "mysql"));

		// superadmin creates db settings with version field
		superadmin.settings().save("db", "type", "mysql");

		// superadmin sets db settings version
		superadmin.settings().save("db", "version", 12);

		// superadmin sets db settings credentials
		superadmin.settings().save("db", "credentials", //
				Json.object("username", "tiger", "password", "miaou"));

		// superadmin checks settings are correct
		ObjectNode settings = superadmin.settings().get("db");
		assertEquals(Json.object("type", "mysql", "version", 12, "credentials",
				Json.object("username", "tiger", "password", "miaou")), settings);

		// only superadmins can get settings
		assertHttpError(401, () -> guest.settings().get("db", "type"));
		assertHttpError(403, () -> vince.settings().get("db", "type"));

		// superadmin gets each field
		assertEquals("mysql", superadmin.settings().get("db", "type").asText());
		assertEquals(12, superadmin.settings().get("db", "version").asInt());
		assertEquals(Json.object("username", "tiger", "password", "miaou"), //
				superadmin.settings().get("db", "credentials"));

		// superadmin gets an unknown field of db settings
		assertEquals(NullNode.getInstance(), superadmin.settings().get("db", "XXX"));

		// superadmin fails to get an unknown field of an unknown settings
		// he is always allowed since superadmin but not found error
		assertHttpError(404, () -> superadmin.settings().get("XXX", "YYY"));

		// vince fails to get an unknown field of an unknown settings
		// since not allowed
		assertHttpError(403, () -> vince.settings().get("XXX", "YYY"));

		// superadmin updates each field
		superadmin.settings().save("db", "type", "postgres");
		superadmin.settings().save("db", "version", 13);
		superadmin.settings().save("db", "credentials", //
				Json.object("username", "lion", "password", "arf"));

		// superadmin checks settings are correct
		settings = superadmin.settings().get("db");
		assertEquals(Json.object("type", "postgres", "version", 13, "credentials",
				Json.object("username", "lion", "password", "arf")), settings);

		// only superadmins can delete settings fields
		assertHttpError(401, () -> guest.settings().delete("db", "type"));
		assertHttpError(403, () -> vince.settings().delete("db", "type"));

		// superadmin deletes version
		superadmin.settings().delete("db", "version");

		// superadmin nulls credentials
		superadmin.settings().save("db", "credentials", null);

		// superadmin checks settings are correct
		settings = superadmin.settings().get("db");
		assertEquals(Json.object("type", "postgres", "credentials", null), settings);
	}
}
