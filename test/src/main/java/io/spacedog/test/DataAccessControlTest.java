/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceTest;
import io.spacedog.model.DataObject;
import io.spacedog.model.InternalDataAclSettings;
import io.spacedog.model.JsonDataObject;
import io.spacedog.model.Permission;
import io.spacedog.model.RolePermissions;
import io.spacedog.model.Schema;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Type;
import io.spacedog.utils.Json;

public class DataAccessControlTest extends SpaceTest {

	@Test
	public void testCustomSchemaAcl() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog admin = createTempDog(superadmin, "admin", Type.admin.name());

		// set message schema
		Schema schema = Schema.builder("msge").text("t").build();
		RolePermissions acl = new RolePermissions()//
				.put(Credentials.ALL_ROLE, Permission.read_all)//
				.put(Credentials.Type.user.name(), Permission.create, //
						Permission.update, Permission.delete, Permission.search) //
				.put(Credentials.Type.admin.name(), Permission.create, //
						Permission.update_all, Permission.delete_all, Permission.search);
		schema.acl(acl);
		superadmin.schema().set(schema);

		// message schema does not contain any acl
		// it means message schema has default acl
		assertEquals(acl, //
				superadmin.settings().get(InternalDataAclSettings.class).get(schema.name()));

		// in default acl, only users and admins can create objects
		guest.post("/1/data/msge").bodyJson("t", "hello").go(403);
		guest.put("/1/data/msge/guest").bodyJson("t", "hello").go(403);
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
	public void testDefaultSchemaAcl() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog admin = createTempDog(superadmin, "admin", Type.admin.name());

		// superadmin sets message schema with default empty acl
		Schema schema = Schema.builder("msge").text("t").build();
		superadmin.schema().set(schema);

		// superadmin check schema acl are empty
		InternalDataAclSettings settings = superadmin.settings().get(InternalDataAclSettings.class);
		assertEquals(0, settings.size());
		assertTrue(settings.get(schema.name()).isEmpty());

		// in empty acl, nobody can create a message but superadmins
		guest.post("/1/data/msge").bodyJson("t", "hi").go(403);
		vince.post("/1/data/msge").bodyJson("t", "hi").go(403);
		admin.post("/1/data/msge").bodyJson("t", "hi").go(403);
		superadmin.put("/1/data/msge/1").bodyJson("t", "hi").go(201);

		// in empty acl, nobody can read a message but superadmins
		guest.get("/1/data/msge/1").go(403);
		vince.get("/1/data/msge/1").go(403);
		admin.get("/1/data/msge/1").go(403);
		superadmin.get("/1/data/msge/1").go(200);

		// in empty acl, nobody can search for objects but superadmins
		guest.get("/1/data/msge").go(403);
		vince.get("/1/data/msge").go(403);
		admin.get("/1/data/msge").go(403);
		superadmin.get("/1/data/msge").refresh().go(200)//
				.assertEquals("1", "results.0.id");

		// in empty acl, nobody can update any object but superadmins
		guest.put("/1/data/msge/1").bodyJson("t", "ola").go(403);
		vince.put("/1/data/msge/1").bodyJson("t", "ola").go(403);
		admin.put("/1/data/msge/1").bodyJson("t", "ola").go(403);
		superadmin.put("/1/data/msge/1").bodyJson("t", "ola").go(200);

		// in empty acl, nobody can delete any object but superadmins
		guest.delete("/1/data/msge/1").go(403);
		vince.delete("/1/data/msge/1").go(403);
		admin.delete("/1/data/msge/1").go(403);
		superadmin.delete("/1/data/msge/1").go(200);

	}

	@Test
	public void testAnotherCustomSchemaAcl() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog admin = createTempDog(superadmin, "admin", Type.admin.name());

		// set message schema with custom acl settings
		Schema schema = Schema.builder("msge").text("t").build();
		schema.acl("user", Permission.create);
		schema.acl("admin", Permission.search);
		superadmin.schema().set(schema);

		// check message schema acl are set
		InternalDataAclSettings settings = superadmin.settings().get(InternalDataAclSettings.class);
		assertEquals(1, settings.size());
		assertEquals(schema.acl(), settings.get(schema.name()));

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
				.assertEquals("2", "results.1.id");

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

	@Test
	public void testDataAccessWithRolesAndPermissions() throws JsonProcessingException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// set schema
		Schema schema = Schema.builder("message")//
				.text("text")//
				.acl("iron", Permission.read_all)//
				.acl("silver", Permission.read_all, Permission.update_all)//
				.acl("gold", Permission.read_all, Permission.update_all, //
						Permission.create)//
				.acl("platine", Permission.read_all, Permission.update_all, //
						Permission.create, Permission.delete_all)//
				.build();

		superadmin.schema().set(schema);

		// dave has the platine role
		// he's got all the rights
		SpaceDog dave = createTempDog(superadmin, "dave");
		superadmin.credentials().setRole(dave.id(), "platine");
		DataObject<ObjectNode> message = new JsonDataObject()//
				.source(Json.object("text", "hi"))//
				.type("message")//
				.id("1");
		message = dave.data().save(message);
		message = dave.data().get("message", "1");
		message.source().put("text", "ola");
		dave.data().save(message);
		dave.data().delete(message);

		// message for users without create permission
		message.id("2");
		message.source().put("text", "salut");
		dave.data().save(message);

		// maelle is a simple user
		// she's got no right on the message schema
		SpaceDog maelle = createTempDog(superadmin, "maelle");
		maelle.post("/1/data/message").bodyJson("text", "hi").go(403);
		maelle.get("/1/data/message/2").go(403);
		maelle.put("/1/data/message/2").bodyJson("text", "hi").go(403);
		maelle.delete("/1/data/message/2").go(403);

		// fred has the iron role
		// he's only got the right to read
		SpaceDog fred = createTempDog(superadmin, "fred");
		superadmin.credentials().setRole(fred.id(), "iron");
		fred.post("/1/data/message").bodyJson("text", "hi").go(403);
		fred.get("/1/data/message/2").go(200);
		fred.put("/1/data/message/2").bodyJson("text", "hi").go(403);
		fred.delete("/1/data/message/2").go(403);

		// nath has the silver role
		// she's got the right to read and update
		SpaceDog nath = createTempDog(superadmin, "nath");
		superadmin.credentials().setRole(nath.id(), "silver");
		nath.post("/1/data/message").bodyJson("text", "hi").go(403);
		nath.get("/1/data/message/2").go(200);
		nath.put("/1/data/message/2").bodyJson("text", "hi").go(200);
		nath.delete("/1/data/message/2").go(403);

		// vince has the gold role
		// he's got the right to create, read and update
		SpaceDog vince = createTempDog(superadmin, "vince");
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
		Schema schema = Schema.builder("message")//
				.acl("user", Permission.search)//
				.text("text")//
				.build();

		superadmin.schema().set(schema);

		// check schema settings contains message schema acl
		InternalDataAclSettings settings = superadmin.settings().get(InternalDataAclSettings.class);
		assertEquals(schema.acl(), settings.get(schema.name()));

		// delete message schema
		superadmin.schema().delete(schema);

		// check schema settings does not contain
		// message schema acl anymore
		settings = superadmin.settings().get(InternalDataAclSettings.class);
		assertNull(settings.get(schema.name()));
	}
}
