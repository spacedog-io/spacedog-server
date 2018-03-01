/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.RolePermissions;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.data.InternalDataAclSettings;
import io.spacedog.client.schema.Schema;

public class DataAccessControlTest extends SpaceTest {

	@Test
	public void testCustomSchemaAcl() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog admin = createTempDog(superadmin, "admin", Roles.admin);

		// set message schema
		Schema schema = Message.schema()//
				.acl(Roles.all, Permission.read)//
				.acl(Roles.user, Permission.create, Permission.updateMine, //
						Permission.deleteMine, Permission.search) //
				.acl(Roles.admin, Permission.create, Permission.update, //
						Permission.delete, Permission.search);

		RolePermissions acl = schema.acl();
		superadmin.schemas().set(schema);

		// message schema does not contain any acl
		// it means message schema has default acl
		assertEquals(acl, superadmin.settings().//
				get(InternalDataAclSettings.class).get(schema.name()));

		// in default acl, only users and admins can create objects
		guest.post("/1/data/message").bodyJson("text", "hello").go(403);
		guest.put("/1/data/message/guest").bodyJson("text", "hello").go(403);
		vince.put("/1/data/message/vince").bodyJson("text", "v1").go(201);
		vince.put("/1/data/message/vince2").bodyJson("text", "v2").go(201);
		admin.put("/1/data/message/admin").bodyJson("text", "a1").go(201);

		// in default acl, everyone can read any objects
		guest.get("/1/data/message/vince").go(200);
		guest.get("/1/data/message/admin").go(200);
		vince.get("/1/data/message/vince").go(200);
		vince.get("/1/data/message/admin").go(200);
		admin.get("/1/data/message/vince").go(200);
		admin.get("/1/data/message/admin").go(200);

		// in default acl, only users and admins can search for objects
		guest.get("/1/data/message/").go(403);
		vince.get("/1/data/message/").go(200);
		admin.get("/1/data/message/").go(200);

		// in default acl, users can update their own objects
		// admin can update any objects
		guest.put("/1/data/message/vince").bodyJson("text", "XXX").go(403);
		guest.put("/1/data/message/admin").bodyJson("text", "XXX").go(403);
		vince.put("/1/data/message/vince").bodyJson("text", "v3").go(200);
		vince.put("/1/data/message/admin").bodyJson("text", "XXX").go(403);
		admin.put("/1/data/message/vince").bodyJson("text", "v4").go(200);
		admin.put("/1/data/message/admin").bodyJson("text", "a2").go(200);

		// in default acl, users can delete their own objects
		// admin can delete any objects
		guest.delete("/1/data/message/vince").go(403);
		guest.delete("/1/data/message/admin").go(403);
		vince.delete("/1/data/message/vince").go(200);
		vince.delete("/1/data/message/admin").go(403);
		admin.delete("/1/data/message/vince").go(404);
		admin.delete("/1/data/message/vince2").go(200);
		admin.delete("/1/data/message/admin").go(200);
	}

	@Test
	public void testDefaultSchemaAcl() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog admin = createTempDog(superadmin, "admin", Roles.admin);

		// superadmin sets message schema with default empty acl
		Schema schema = Message.schema();
		superadmin.schemas().set(schema);

		// superadmin check schema acl are empty
		InternalDataAclSettings settings = superadmin.settings()//
				.get(InternalDataAclSettings.class);
		assertEquals(1, settings.size());
		assertTrue(settings.get(schema.name()).isEmpty());

		// in empty acl, nobody can create a message but superadmins
		guest.post("/1/data/message").bodyJson("text", "hi").go(403);
		vince.post("/1/data/message").bodyJson("text", "hi").go(403);
		admin.post("/1/data/message").bodyJson("text", "hi").go(403);
		superadmin.put("/1/data/message/1").bodyJson("text", "hi").go(201);

		// in empty acl, nobody can read a message but superadmins
		guest.get("/1/data/message/1").go(403);
		vince.get("/1/data/message/1").go(403);
		admin.get("/1/data/message/1").go(403);
		superadmin.get("/1/data/message/1").go(200);

		// in empty acl, nobody can search for objects but superadmins
		guest.get("/1/data/message").go(403);
		vince.get("/1/data/message").go(403);
		admin.get("/1/data/message").go(403);
		superadmin.get("/1/data/message").refresh().go(200)//
				.assertEquals("1", "results.0.id");

		// in empty acl, nobody can update any object but superadmins
		guest.put("/1/data/message/1").bodyJson("text", "ola").go(403);
		vince.put("/1/data/message/1").bodyJson("text", "ola").go(403);
		admin.put("/1/data/message/1").bodyJson("text", "ola").go(403);
		superadmin.put("/1/data/message/1").bodyJson("text", "ola").go(200);

		// in empty acl, nobody can delete any object but superadmins
		guest.delete("/1/data/message/1").go(403);
		vince.delete("/1/data/message/1").go(403);
		admin.delete("/1/data/message/1").go(403);
		superadmin.delete("/1/data/message/1").go(200);

	}

	@Test
	public void testAnotherCustomSchemaAcl() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog admin = createTempDog(superadmin, "admin", Roles.admin);

		// set message schema with custom acl settings
		Schema schema = Message.schema()//
				.acl(Roles.user, Permission.create)//
				.acl(Roles.admin, Permission.search);
		superadmin.schemas().set(schema);

		// check message schema acl are set
		InternalDataAclSettings settings = superadmin.settings().get(InternalDataAclSettings.class);
		assertEquals(1, settings.size());
		assertEquals(schema.acl(), settings.get(schema.name()));

		// only users (and superadmins) can create messages
		guest.post("/1/data/message").bodyJson("text", "hi").go(403);
		vince.post("/1/data/message").bodyJson("text", "hi").go(201);
		admin.post("/1/data/message").bodyJson("text", "hi").go(403);

		// only users (and superadmins) can create messages with specified id
		guest.put("/1/data/message/1").bodyJson("text", "hi").go(403);
		vince.put("/1/data/message/2").bodyJson("text", "hi").go(201);
		admin.put("/1/data/message/3").bodyJson("text", "hi").go(403);

		// only admins (and superadmins) can search for messages
		guest.get("/1/data/message/2").go(403);
		vince.get("/1/data/message/2").go(403);
		admin.get("/1/data/message/2").go(200);

		// only admins (and superadmins) can search for messages
		guest.get("/1/data/message/").go(403);
		vince.get("/1/data/message/").go(403);
		admin.get("/1/data/message/").refresh().go(200)//
				.assertSizeEquals(2, "results")//
				.assertEquals("2", "results.1.id");

		// nobody can update any object (but superadmins)
		guest.put("/1/data/message/2").bodyJson("text", "ola").go(403);
		vince.put("/1/data/message/2").bodyJson("text", "ola").go(403);
		admin.put("/1/data/message/2").bodyJson("text", "ola").go(403);

		// nobody can delete message but superadmins
		guest.delete("/1/data/message/2").go(403);
		vince.delete("/1/data/message/2").go(403);
		admin.delete("/1/data/message/2").go(403);
		superadmin.delete("/1/data/message/2").go(200);

		// nobody can delete all message but superadmins
		guest.delete("/1/data/message").go(403);
		vince.delete("/1/data/message").go(403);
		admin.delete("/1/data/message").go(403);
		superadmin.delete("/1/data/message").go(200)//
				.assertEquals(1, "deleted");
	}

	@Test
	public void testGroupDataAccess() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		superadmin.credentials().enableGuestSignUp(true);

		// vince and fred signs up
		SpaceDog vince = createTempDog(guest, "vince");
		SpaceDog fred = createTempDog(guest, "fred");

		// fred creates nath credentials
		// they share the same group
		SpaceDog nath = createTempDog(fred, "nath");

		// set message schema with custom acl settings
		Schema schema = Message.schema()//
				.acl(Roles.all, Permission.create)//
				.acl(Roles.user, Permission.readGroup, //
						Permission.updateGroup, Permission.deleteGroup);
		superadmin.schemas().set(schema);

		// only users (and superadmins) can create messages
		guest.data().save(Message.TYPE, "guest", new Message("guest"));
		vince.data().save(Message.TYPE, "vince", new Message("vince"));
		fred.data().save(Message.TYPE, "fred", new Message("fred"));

		// guest don't have read permission even on their own objects
		assertHttpError(403, () -> guest.data().get(Message.TYPE, "guest"));

		// vince and fred have read access on their own objects
		// the one's they have created
		vince.data().get(Message.TYPE, "vince");
		fred.data().get(Message.TYPE, "fred");

		// nath has read access on fred's objects
		// since they share the same group
		// and users have 'readGroup" permission
		nath.data().get(Message.TYPE, "fred");

		// vince does not have read access to fred's objects
		// since not in the same group
		assertHttpError(403, () -> vince.data().get(Message.TYPE, "fred"));

		// nath does not have read access to vince's objects
		// since not in the same group
		assertHttpError(403, () -> nath.data().get(Message.TYPE, "vince"));

		// vince and fred have update access on their own objects
		// the one's they have created
		vince.data().save(Message.TYPE, "vince", new Message("vince2"));
		fred.data().save(Message.TYPE, "fred", new Message("fred2"));

		// nath can still read fred's message
		assertEquals("fred2", nath.data()//
				.get(Message.TYPE, "fred", Message.Wrap.class).source().text);

		// nath has update access on fred's objects
		// since they share the same group
		// and users have 'updateGroup" permission
		fred.data().save(Message.TYPE, "fred", new Message("fred3"));

		// fred can still read his message modified by nath
		assertEquals("fred3", fred.data()//
				.get(Message.TYPE, "fred", Message.Wrap.class).source().text);

		// vince does not have update access to fred's objects
		// since not in the same group
		assertHttpError(403, () -> vince.data()//
				.save(Message.TYPE, "fred", new Message("XXX")));

		// nath does not have update access to vince's objects
		// since not in the same group
		assertHttpError(403, () -> nath.data()//
				.save(Message.TYPE, "vince", new Message("XXX")));

		// vince does not have delete access to fred's objects
		// since not in the same group
		assertHttpError(403, () -> vince.data()//
				.delete(Message.TYPE, "fred"));

		// nath does not have update access to vince's objects
		// since not in the same group
		assertHttpError(403, () -> nath.data()//
				.delete(Message.TYPE, "vince"));

		// nath has delete access on fred's objects
		// since they share the same group
		// and users have 'deleteGroup" permission
		nath.data().delete(Message.TYPE, "fred");
	}

	@Test
	public void testDataAccessWithRolesAndPermissions() throws JsonProcessingException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();

		// set schema
		Schema schema = Message.schema()//
				.acl("iron", Permission.read)//
				.acl("silver", Permission.read, Permission.update)//
				.acl("gold", Permission.read, Permission.update, //
						Permission.create)//
				.acl("platine", Permission.read, Permission.update, //
						Permission.create, Permission.delete);

		superadmin.schemas().set(schema);

		// dave has the platine role
		// he's got all the rights
		SpaceDog dave = createTempDog(superadmin, "dave");
		superadmin.credentials().setRole(dave.id(), "platine");
		DataWrap<Message> message = new Message.Wrap()//
				.source(new Message("hi")).id("1");
		message = dave.data().save(message);
		message = dave.data().get(Message.TYPE, "1", Message.Wrap.class);
		message.source().text = "ola";
		dave.data().save(message);
		dave.data().delete(message);

		// message for users without create permission
		message.id("2");
		message.source().text = "salut";
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
		assertHttpError(403, () -> fred.data()//
				.save(Message.TYPE, new Message("hi")));
		fred.data().get(Message.TYPE, "2");
		assertHttpError(403, () -> fred.data()//
				.save(Message.TYPE, "2", new Message("hi")));
		assertHttpError(403, () -> fred.data().delete(Message.TYPE, "2"));

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
		SpaceDog superadmin = clearRootBackend();

		// create message schema with simple acl
		Schema schema = Message.schema()//
				.acl(Roles.user, Permission.search);
		superadmin.schemas().set(schema);

		// check schema settings contains message schema acl
		InternalDataAclSettings settings = superadmin.settings()//
				.get(InternalDataAclSettings.class);
		assertEquals(schema.acl(), settings.get(schema.name()));

		// delete message schema
		superadmin.schemas().delete(schema);

		// check schema settings does not contain
		// message schema acl anymore
		settings = superadmin.settings().get(InternalDataAclSettings.class);
		assertTrue(settings.get(schema.name()).isEmpty());
	}
}
