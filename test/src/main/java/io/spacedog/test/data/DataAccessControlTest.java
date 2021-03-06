/**
 * © David Attias 2015
 */
package io.spacedog.test.data;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.schema.Schema;
import io.spacedog.test.Message;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Json;

public class DataAccessControlTest extends SpaceTest {

	@Test
	public void testCustomSchemaAcl() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog admin = createTempDog(superadmin, "admin", Roles.admin);

		// superadmin sets message schema
		Schema schema = Message.schema();
		superadmin.schemas().set(schema);

		// superadmin sets data acl
		DataSettings dataSettings = new DataSettings();
		dataSettings.acl().put(schema.name(), Roles.all, Permission.read);
		dataSettings.acl().put(schema.name(), Roles.user, Permission.create, //
				Permission.updateMine, Permission.deleteMine, Permission.search);
		dataSettings.acl().put(schema.name(), Roles.admin, Permission.create, //
				Permission.update, Permission.delete, Permission.search);
		superadmin.data().settings(dataSettings);

		assertEquals(dataSettings, superadmin.settings().get(DataSettings.class));

		// in default acl, only users and admins can create objects
		guest.post("/2/data/message").bodyJson("text", "hello").go(401).asVoid();
		guest.put("/2/data/message/guest").bodyJson("text", "hello").go(401).asVoid();
		vince.put("/2/data/message/vince").bodyJson("text", "v1").go(201).asVoid();
		vince.put("/2/data/message/vince2").bodyJson("text", "v2").go(201).asVoid();
		admin.put("/2/data/message/admin").bodyJson("text", "a1").go(201).asVoid();

		// in default acl, everyone can read any objects
		guest.get("/2/data/message/vince").go(200).asVoid();
		guest.get("/2/data/message/admin").go(200).asVoid();
		vince.get("/2/data/message/vince").go(200).asVoid();
		vince.get("/2/data/message/admin").go(200).asVoid();
		admin.get("/2/data/message/vince").go(200).asVoid();
		admin.get("/2/data/message/admin").go(200).asVoid();

		// in default acl, only users and admins can search for objects
		guest.get("/2/data/message/").go(401).asVoid();
		vince.get("/2/data/message/").go(200).asVoid();
		admin.get("/2/data/message/").go(200).asVoid();

		// in default acl, users can update their own objects
		// admin can update any objects
		guest.put("/2/data/message/vince").bodyJson("text", "XXX").go(401).asVoid();
		guest.put("/2/data/message/admin").bodyJson("text", "XXX").go(401).asVoid();
		vince.put("/2/data/message/vince").bodyJson("text", "v3").go(200).asVoid();
		vince.put("/2/data/message/admin").bodyJson("text", "XXX").go(403).asVoid();
		admin.put("/2/data/message/vince").bodyJson("text", "v4").go(200).asVoid();
		admin.put("/2/data/message/admin").bodyJson("text", "a2").go(200).asVoid();

		// in default acl, users can delete their own objects
		// admin can delete any objects
		guest.delete("/2/data/message/vince").go(401).asVoid();
		guest.delete("/2/data/message/admin").go(401).asVoid();
		vince.delete("/2/data/message/vince").go(200).asVoid();
		vince.delete("/2/data/message/admin").go(403).asVoid();
		admin.delete("/2/data/message/vince").go(404).asVoid();
		admin.delete("/2/data/message/vince2").go(200).asVoid();
		admin.delete("/2/data/message/admin").go(200).asVoid();
	}

	@Test
	public void testDefaultSchemaAcl() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog admin = createTempDog(superadmin, "admin", Roles.admin);

		// superadmin sets message schema with default empty acl
		Schema schema = Message.schema();
		superadmin.schemas().set(schema);

		// superadmin check message acl is empty
		assertTrue(superadmin.settings().get(DataSettings.class)//
				.acl().get(schema.name()).isEmpty());

		// in empty acl, nobody can create a message but superadmins
		guest.post("/2/data/message").bodyJson("text", "hi").go(401).asVoid();
		vince.post("/2/data/message").bodyJson("text", "hi").go(403).asVoid();
		admin.post("/2/data/message").bodyJson("text", "hi").go(403).asVoid();
		superadmin.put("/2/data/message/1").bodyJson("text", "hi").go(201).asVoid();

		// in empty acl, nobody can read a message but superadmins
		guest.get("/2/data/message/1").go(401).asVoid();
		vince.get("/2/data/message/1").go(403).asVoid();
		admin.get("/2/data/message/1").go(403).asVoid();
		superadmin.get("/2/data/message/1").go(200).asVoid();

		// in empty acl, nobody can search for objects but superadmins
		guest.get("/2/data/message").go(401).asVoid();
		vince.get("/2/data/message").go(403).asVoid();
		admin.get("/2/data/message").go(403).asVoid();
		superadmin.get("/2/data/message").refresh().go(200)//
				.assertEquals("1", "objects.0.id");

		// in empty acl, nobody can update any object but superadmins
		guest.put("/2/data/message/1").bodyJson("text", "ola").go(401).asVoid();
		vince.put("/2/data/message/1").bodyJson("text", "ola").go(403).asVoid();
		admin.put("/2/data/message/1").bodyJson("text", "ola").go(403).asVoid();
		superadmin.put("/2/data/message/1").bodyJson("text", "ola").go(200).asVoid();

		// in empty acl, nobody can delete any object but superadmins
		guest.delete("/2/data/message/1").go(401).asVoid();
		vince.delete("/2/data/message/1").go(403).asVoid();
		admin.delete("/2/data/message/1").go(403).asVoid();
		superadmin.delete("/2/data/message/1").go(200).asVoid();

	}

	@Test
	public void testAnotherCustomSchemaAcl() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog admin = createTempDog(superadmin, "admin", Roles.admin);

		// superadmin sets message schema
		Schema schema = Message.schema();
		superadmin.schemas().set(schema);

		// superadmin sets data acl
		DataSettings dataSettings = new DataSettings();
		dataSettings.acl().put(schema.name(), Roles.user, Permission.create);
		dataSettings.acl().put(schema.name(), Roles.admin, Permission.search);
		superadmin.data().settings(dataSettings);

		// check message schema acl are set
		assertEquals(dataSettings, superadmin.settings().get(DataSettings.class));

		// only users (and superadmins) can create messages
		guest.post("/2/data/message").bodyJson("text", "hi").go(401).asVoid();
		vince.post("/2/data/message").bodyJson("text", "hi").go(201).asVoid();
		admin.post("/2/data/message").bodyJson("text", "hi").go(403).asVoid();

		// only users (and superadmins) can create messages with specified id
		guest.put("/2/data/message/1").bodyJson("text", "hi").go(401).asVoid();
		vince.put("/2/data/message/2").bodyJson("text", "hi").go(201).asVoid();
		admin.put("/2/data/message/3").bodyJson("text", "hi").go(403).asVoid();

		// only admins (and superadmins) can search for messages
		guest.get("/2/data/message/2").go(401).asVoid();
		vince.get("/2/data/message/2").go(403).asVoid();
		admin.get("/2/data/message/2").go(200).asVoid();

		// only admins (and superadmins) can search for messages
		guest.get("/2/data/message/").go(401).asVoid();
		vince.get("/2/data/message/").go(403).asVoid();
		admin.get("/2/data/message/").refresh().go(200)//
				.assertSizeEquals(2, "objects")//
				.assertEquals("2", "objects.1.id");

		// nobody can update any object (but superadmins)
		guest.put("/2/data/message/2").bodyJson("text", "ola").go(401).asVoid();
		vince.put("/2/data/message/2").bodyJson("text", "ola").go(403).asVoid();
		admin.put("/2/data/message/2").bodyJson("text", "ola").go(403).asVoid();

		// nobody can delete message but superadmins
		guest.delete("/2/data/message/2").go(401).asVoid();
		vince.delete("/2/data/message/2").go(403).asVoid();
		admin.delete("/2/data/message/2").go(403).asVoid();
		superadmin.delete("/2/data/message/2").go(200).asVoid();

		// nobody can delete all message but superadmins
		guest.delete("/2/data/message").go(401).asVoid();
		vince.delete("/2/data/message").go(403).asVoid();
		admin.delete("/2/data/message").go(403).asVoid();
		superadmin.delete("/2/data/message").go(200)//
				.assertEquals(1, "deleted");
	}

	@Test
	public void testGroupDataAccess() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();
		superadmin.credentials().enableGuestSignUp(true);

		// vince and fred signs up
		SpaceDog vince = createTempDog(guest, "vince");
		SpaceDog fred = createTempDog(guest, "fred");

		// vince and fred have their own unique group
		assertFalse(Strings.isNullOrEmpty(vince.group()));
		assertFalse(Strings.isNullOrEmpty(fred.group()));
		assertNotEquals(vince.group(), fred.group());

		// fred creates nath credentials
		SpaceDog nath = createTempDog(fred, "nath");

		// nath has a different group than fred
		assertFalse(Strings.isNullOrEmpty(nath.group()));
		assertNotEquals(fred.group(), nath.group());

		// set message schema
		Schema schema = Message.schema();
		superadmin.schemas().set(schema);

		// superadmin sets message schema acl
		DataSettings dataSettings = new DataSettings();
		dataSettings.acl().get(schema.name())//
				.put(Roles.all, Permission.create)//
				.put(Roles.user, Permission.readGroup, //
						Permission.updateGroup, Permission.deleteGroup);
		superadmin.data().settings(dataSettings);

		// all guests and users can create messages
		guest.data().save(new Message("guest"), "guest");
		vince.data().save(new Message("vince"), "vince");
		fred.data().save(new Message("fred"), "fred");

		// guest don't have read permission even on their own objects
		assertHttpError(401, () -> guest.data().get(Message.TYPE, "guest"));

		// vince and fred have read access on their own objects
		// the one's they have created
		vince.data().get(Message.TYPE, "vince");
		fred.data().get(Message.TYPE, "fred");

		// fred shares his group with nath
		fred.credentials().shareGroup(nath.id(), fred.group());

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
		vince.data().save(new Message("vince2"), "vince");
		fred.data().save(new Message("fred2"), "fred");

		// nath can still read fred's message
		assertEquals("fred2", nath.data().get("fred", Message.class).text);

		// nath has update access on fred's objects
		// since they share the same group
		// and users have 'updateGroup" permission
		fred.data().save(new Message("fred3"), "fred");

		// fred can still read his message modified by nath
		assertEquals("fred3", fred.data().get("fred", Message.class).text);

		// vince does not have update access to fred's objects
		// since not in the same group
		assertHttpError(403, () -> vince.data().save(new Message("XXX"), "fred"));

		// nath does not have update access to vince's objects
		// since not in the same group
		assertHttpError(403, () -> nath.data().save(new Message("XXX"), "vince"));

		// vince does not have delete access to fred's objects
		// since not in the same group
		assertHttpError(403, () -> vince.data().delete(Message.TYPE, "fred"));

		// nath does not have update access to vince's objects
		// since not in the same group
		assertHttpError(403, () -> nath.data().delete(Message.TYPE, "vince"));

		// nath has delete access on fred's objects
		// since they share the same group
		// and users have 'deleteGroup" permission
		nath.data().delete(Message.TYPE, "fred");
	}

	@Test
	public void testDataAccessWithRolesAndPermissions() throws JsonProcessingException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		// set schema
		Schema schema = Message.schema();
		superadmin.schemas().set(schema);

		// superadmin sets data acl
		DataSettings dataSettings = new DataSettings();
		dataSettings.acl().put(schema.name(), "iron", Permission.read);
		dataSettings.acl().put(schema.name(), "silver", Permission.read, Permission.update);
		dataSettings.acl().put(schema.name(), "gold", Permission.read, Permission.update, //
				Permission.create);
		dataSettings.acl().put(schema.name(), "platine", Permission.read, Permission.update, //
				Permission.create, Permission.delete);
		superadmin.data().settings(dataSettings);

		// dave has the platine role
		// he's got all the rights
		SpaceDog dave = createTempDog(superadmin, "dave");
		superadmin.credentials().setRole(dave.id(), "platine");
		DataWrap<Message> message = DataWrap.wrap(new Message("hi")).id("1");
		message = dave.data().save(message);
		message = dave.data().getWrapped(Message.TYPE, "1", Message.class);
		message.source().text = "ola";
		dave.data().save(message);
		dave.data().delete(message);

		// message for users without create permission
		dave.data().save(DataWrap.wrap(Json.object("text", "salut")).type("message").id("2"));

		// maelle is a simple user
		// she's got no right on the message schema
		SpaceDog maelle = createTempDog(superadmin, "maelle");
		maelle.post("/2/data/message").bodyJson("text", "hi").go(403).asVoid();
		maelle.get("/2/data/message/2").go(403).asVoid();
		maelle.put("/2/data/message/2").bodyJson("text", "hi").go(403).asVoid();
		maelle.delete("/2/data/message/2").go(403).asVoid();

		// fred has the iron role
		// he's only got the right to read
		SpaceDog fred = createTempDog(superadmin, "fred");
		superadmin.credentials().setRole(fred.id(), "iron");
		assertHttpError(403, () -> fred.data().create(new Message("hi")));
		fred.data().get(Message.TYPE, "2");
		assertHttpError(403, () -> fred.data().save(new Message("hi"), "2"));
		assertHttpError(403, () -> fred.data().delete(Message.TYPE, "2"));

		// nath has the silver role
		// she's got the right to read and update
		SpaceDog nath = createTempDog(superadmin, "nath");
		superadmin.credentials().setRole(nath.id(), "silver");
		nath.post("/2/data/message").bodyJson("text", "hi").go(403).asVoid();
		nath.get("/2/data/message/2").go(200).asVoid();
		nath.put("/2/data/message/2").bodyJson("text", "hi").go(200).asVoid();
		nath.delete("/2/data/message/2").go(403).asVoid();

		// vince has the gold role
		// he's got the right to create, read and update
		SpaceDog vince = createTempDog(superadmin, "vince");
		superadmin.credentials().setRole(vince.id(), "gold");
		vince.put("/2/data/message/3").bodyJson("text", "grunt").go(201).asVoid();
		vince.get("/2/data/message/3").go(200).asVoid();
		vince.put("/2/data/message/3").bodyJson("text", "flux").go(200).asVoid();
		vince.delete("/2/data/message/3").go(403).asVoid();
	}

	@Test
	public void deleteSchemaDoesNotDeletesItsAccessControlList() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		// create message schema with simple acl
		Schema schema = Message.schema();
		superadmin.schemas().set(schema);

		// superadmin sets data acl
		DataSettings dataSettings = new DataSettings();
		dataSettings.acl().put(schema.name(), Roles.user, Permission.search);
		superadmin.data().settings(dataSettings);

		// check data acl settings contains message acl
		assertEquals(dataSettings, superadmin.settings().get(DataSettings.class));

		// delete message schema
		superadmin.schemas().delete(schema);

		// check data acl settings still contains message acl
		assertEquals(dataSettings, superadmin.settings().get(DataSettings.class));
	}
}
