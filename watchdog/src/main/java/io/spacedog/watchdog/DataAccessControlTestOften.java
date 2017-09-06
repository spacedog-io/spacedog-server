/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceTest;
import io.spacedog.model.DataPermission;
import io.spacedog.model.Schema;
import io.spacedog.model.Schema.DataAcl;
import io.spacedog.model.InternalDataSettings;
import io.spacedog.utils.Json;

public class DataAccessControlTestOften extends SpaceTest {

	@Test
	public void testDefaulSchemaAcl() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = vinceSignsUp(guest);
		SpaceDog admin = adminSignsUp(superadmin);

		// set message schema
		Schema messageSchema = Schema.builder("msge").text("t").build();
		superadmin.schema().set(messageSchema);

		// message schema does not contain any acl
		// it means message schema has default acl
		InternalDataSettings settings = superadmin.settings().get(InternalDataSettings.class);
		assertEquals(DataAcl.defaultAcl(), settings.acl.get("msge"));

		// in default acl, only users and admins can create objects
		guest.post("/1/data/msge").bodyJson("t", "hello").go(403);
		vince.put("/1/data/msge/vince").bodyJson("t", "v1").go(201);
		vince.put("/1/data/msge/vince2").bodyJson("t", "v2").go(201);
		admin.put("/1/data/msge/admin").bodyJson("t", "a1").go(201);

		// in default acl, everyone can read any objects
		guest.get("/1/data/msge/vince").go(200);
		guest.get("/1/data/msge/admin").go(200);
		vince.get("/1/data/msge/vince").go(200);
		vince.get("/1/data/msge/admin").go(200);
		admin.get("/1/data/msge/vince").go(200);
		admin.get("/1/data/msge/admin").go(200);

		// in default acl, only users and admins can search for objects
		guest.get("/1/data/msge/").go(403);
		vince.get("/1/data/msge/").go(200);
		admin.get("/1/data/msge/").go(200);

		// in default acl, users can update their own objects
		// admin can update any objects
		guest.put("/1/data/msge/vince").bodyJson("t", "XXX").go(403);
		guest.put("/1/data/msge/admin").bodyJson("t", "XXX").go(403);
		vince.put("/1/data/msge/vince").bodyJson("t", "v3").go(200);
		vince.put("/1/data/msge/admin").bodyJson("t", "XXX").go(403);
		admin.put("/1/data/msge/vince").bodyJson("t", "v4").go(200);
		admin.put("/1/data/msge/admin").bodyJson("t", "a2").go(200);

		// in default acl, users can delete their own objects
		// admin can delete any objects
		guest.delete("/1/data/msge/vince").go(403);
		guest.delete("/1/data/msge/admin").go(403);
		vince.delete("/1/data/msge/vince").go(200);
		vince.delete("/1/data/msge/admin").go(403);
		admin.delete("/1/data/msge/vince").go(404);
		admin.delete("/1/data/msge/vince2").go(200);
		admin.delete("/1/data/msge/admin").go(200);
	}

	@Test
	public void testEmptySchemaAcl() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = vinceSignsUp(guest);
		SpaceDog admin = adminSignsUp(superadmin);

		// superadmin sets message schema with empty acl
		Schema messageSchema = Schema.builder("msge").text("t").build();
		messageSchema.acl(new DataAcl());
		superadmin.schema().set(messageSchema);

		// check message schema acl are set
		InternalDataSettings settings = superadmin.settings().get(InternalDataSettings.class);
		assertEquals(1, settings.acl.size());
		assertEquals(0, settings.acl.get("msge").size());

		// in empty acl, nobody can create a message but superadmins
		guest.post("/1/data/msge").bodyJson("t", "hi").go(403);
		vince.post("/1/data/msge").bodyJson("t", "hi").go(403);
		admin.post("/1/data/msge").bodyJson("t", "hi").go(403);
		superadmin.put("/1/data/msge/1").bodyJson("t", "hi").go(201);

		// in empty acl, nobody can read a message but superadmins
		guest.get("/1/data/msge/vince").go(403);
		vince.get("/1/data/msge/vince").go(403);
		admin.get("/1/data/msge/vince").go(403);
		superadmin.get("/1/data/msge/1").go(200);

		// in empty acl, nobody can search for objects but superadmins
		guest.get("/1/data/msge/").go(403);
		vince.get("/1/data/msge/").go(403);
		admin.get("/1/data/msge/").go(403);
		superadmin.get("/1/data/msge/").refresh().go(200)//
				.assertEquals("1", "results.0.meta.id");

		// in empty acl, nobody can update any object but superadmins
		guest.put("/1/data/msge/vince").bodyJson("t", "ola").go(403);
		vince.put("/1/data/msge/vince").bodyJson("t", "ola").go(403);
		admin.put("/1/data/msge/vince").bodyJson("t", "ola").go(403);
		superadmin.put("/1/data/msge/1").bodyJson("t", "ola").go(200);

		// in empty acl, nobody can delete any object but superadmins
		guest.delete("/1/data/msge/vince").go(403);
		vince.delete("/1/data/msge/vince").go(403);
		admin.delete("/1/data/msge/vince").go(403);
		superadmin.delete("/1/data/msge/1").go(200);

	}

	@Test
	public void testCustomSchemaAcl() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = vinceSignsUp(guest);
		SpaceDog admin = adminSignsUp(superadmin);

		// set message schema with custom acl settings
		Schema messageSchema = Schema.builder("msge").text("t").build();
		messageSchema.acl("user", DataPermission.create);
		messageSchema.acl("admin", DataPermission.search);
		superadmin.schema().set(messageSchema);

		// check message schema acl are set
		InternalDataSettings settings = superadmin.settings().get(InternalDataSettings.class);
		assertEquals(1, settings.acl.size());
		assertEquals(2, settings.acl.get("msge").size());
		assertEquals(Collections.singleton(DataPermission.search), //
				settings.acl.get("msge").get("admin"));
		assertEquals(Collections.singleton(DataPermission.create), //
				settings.acl.get("msge").get("user"));

		// only users (and superadmins) can create messages
		guest.post("/1/data/msge").bodyJson("t", "hi").go(403);
		vince.post("/1/data/msge").bodyJson("t", "hi").go(201);
		admin.post("/1/data/msge").bodyJson("t", "hi").go(403);

		// only users (and superadmins) can create messages with specified id
		guest.put("/1/data/msge/1").bodyJson("t", "hi").go(403);
		vince.put("/1/data/msge/2").bodyJson("t", "hi").go(201);
		admin.put("/1/data/msge/3").bodyJson("t", "hi").go(403);

		// only admins (and superadmins) can search for messages
		guest.get("/1/data/msge/2").go(403);
		vince.get("/1/data/msge/2").go(403);
		admin.get("/1/data/msge/2").go(200);

		// only admins (and superadmins) can search for messages
		guest.get("/1/data/msge/").go(403);
		vince.get("/1/data/msge/").go(403);
		admin.get("/1/data/msge/").refresh().go(200)//
				.assertSizeEquals(2, "results")//
				.assertEquals("2", "results.1.meta.id");

		// nobody can update any object (but superadmins)
		guest.put("/1/data/msge/2").bodyJson("t", "ola").go(403);
		vince.put("/1/data/msge/2").bodyJson("t", "ola").go(403);
		admin.put("/1/data/msge/2").bodyJson("t", "ola").go(403);

		// nobody can delete message but superadmins
		guest.delete("/1/data/msge/2").go(403);
		vince.delete("/1/data/msge/2").go(403);
		admin.delete("/1/data/msge/2").go(403);
		superadmin.delete("/1/data/msge/2").go(200);

		// nobody can delete all message but superadmins
		guest.delete("/1/data/msge").go(403);
		vince.delete("/1/data/msge").go(403);
		admin.delete("/1/data/msge").go(403);
		superadmin.delete("/1/data/msge").go(200)//
				.assertEquals(1, "totalDeleted");
	}

	private SpaceDog adminSignsUp(SpaceDog superadmin) {
		superadmin.credentials().create(//
				"admin", "hi admin", "platform@spacedog.io", "admin");
		return SpaceDog.backend(superadmin)//
				.username("admin").login("hi admin");
	}

	private SpaceDog vinceSignsUp(SpaceDog guest) {
		return SpaceDog.backend(guest).username("vince")//
				.email("platform@spacedog.io").signUp("hi vince");
	}

	@Test
	public void testDataAccessWithRolesAndPermissions() throws JsonProcessingException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// set schema
		Schema schema = Schema.builder("message")//
				.text("text")//
				.acl("iron", DataPermission.read_all)//
				.acl("silver", DataPermission.read_all, DataPermission.update_all)//
				.acl("gold", DataPermission.read_all, DataPermission.update_all, //
						DataPermission.create)//
				.acl("platine", DataPermission.read_all, DataPermission.update_all, //
						DataPermission.create, DataPermission.delete_all)//
				.build();

		superadmin.schema().set(schema);

		// dave has the platine role
		// he's got all the rights
		SpaceDog dave = signUp(superadmin, "dave", "hi dave");
		superadmin.credentials().setRole(dave.id(), "platine");
		ObjectNode message = Json.object("text", "hi");
		dave.data().create("message", "1", message);
		message = dave.data().get("message", "1");
		message.put("text", "ola");
		dave.data().save("message", "1", message);
		dave.data().delete("message", "1");

		// message for users without create permission
		message.put("text", "salut");
		dave.data().create("message", "2", message);

		// maelle is a simple user
		// she's got no right on the message schema
		SpaceDog maelle = signUp(superadmin, "maelle", "hi maelle");
		maelle.post("/1/data/message").bodyJson("text", "hi").go(403);
		maelle.get("/1/data/message/2").go(403);
		maelle.put("/1/data/message/2").bodyJson("text", "hi").go(403);
		maelle.delete("/1/data/message/2").go(403);

		// fred has the iron role
		// he's only got the right to read
		SpaceDog fred = signUp(superadmin, "fred", "hi fred");
		superadmin.credentials().setRole(fred.id(), "iron");
		fred.post("/1/data/message").bodyJson("text", "hi").go(403);
		fred.get("/1/data/message/2").go(200);
		fred.put("/1/data/message/2").bodyJson("text", "hi").go(403);
		fred.delete("/1/data/message/2").go(403);

		// nath has the silver role
		// she's got the right to read and update
		SpaceDog nath = signUp(superadmin, "nath", "hi nath");
		superadmin.credentials().setRole(nath.id(), "silver");
		nath.post("/1/data/message").bodyJson("text", "hi").go(403);
		nath.get("/1/data/message/2").go(200);
		nath.put("/1/data/message/2").bodyJson("text", "hi").go(200);
		nath.delete("/1/data/message/2").go(403);

		// vince has the gold role
		// he's got the right to create, read and update
		SpaceDog vince = signUp(superadmin, "vince", "hi vince");
		superadmin.credentials().setRole(vince.id(), "gold");
		vince.put("/1/data/message/3").bodyJson("text", "grunt").go(201);
		vince.get("/1/data/message/3").go(200);
		vince.put("/1/data/message/3").bodyJson("text", "flux").go(200);
		vince.delete("/1/data/message/3").go(403);
	}

	@Test
	public void deleteSchemaDeletesItsAccessControlList() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// create message schema with simple acl
		Schema messageSchema = Schema.builder("message")//
				.acl("user", DataPermission.search)//
				.text("text")//
				.build();

		superadmin.schema().set(messageSchema);

		// check schema settings contains message schema acl
		InternalDataSettings settings = superadmin.settings().get(InternalDataSettings.class);
		assertEquals(messageSchema.acl(), settings.acl.get(messageSchema.name()));

		// delete message schema
		superadmin.schema().delete(messageSchema);

		// check schema settings does not contain
		// message schema acl anymore
		settings = superadmin.settings().get(InternalDataSettings.class);
		assertNull(settings.acl.get(messageSchema.name()));
	}
}
