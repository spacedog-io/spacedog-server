/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Schema;
import io.spacedog.utils.Schema.SchemaAcl;
import io.spacedog.utils.SchemaSettings;

public class DataAccessControlTestOften extends SpaceTest {

	@Test
	public void testSchemaAclManagement() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog vince = signUp(test, "vince", "hi vince");

		// set message schema
		Schema messageSchema = Schema.builder("msge").text("t").build();
		test.schema().set(messageSchema);

		// message schema does not contain any acl
		// it means message schema has default acl
		SchemaSettings settings = test.settings().get(SchemaSettings.class);
		assertEquals(SchemaAcl.defaultAcl(), settings.acl.get("msge"));

		// in default acl, only users and admins can create objects
		SpaceRequest.post("/1/data/msge").backend(test).body("t", "hello").go(403);
		SpaceRequest.post("/1/data/msge?id=vince").userAuth(vince).body("t", "v1").go(201);
		SpaceRequest.post("/1/data/msge?id=vince2").userAuth(vince).body("t", "v2").go(201);
		SpaceRequest.post("/1/data/msge?id=admin").adminAuth(test).body("t", "a1").go(201);

		// in default acl, everyone can read any objects
		SpaceRequest.get("/1/data/msge/vince").backend(test).go(200);
		SpaceRequest.get("/1/data/msge/admin").backend(test).go(200);
		SpaceRequest.get("/1/data/msge/vince").userAuth(vince).go(200);
		SpaceRequest.get("/1/data/msge/admin").userAuth(vince).go(200);
		SpaceRequest.get("/1/data/msge/vince").adminAuth(test).go(200);
		SpaceRequest.get("/1/data/msge/admin").adminAuth(test).go(200);

		// in default acl, only users and admins can search for objects
		SpaceRequest.get("/1/data/msge/").backend(test).go(403);
		SpaceRequest.get("/1/data/msge/").userAuth(vince).go(200);
		SpaceRequest.get("/1/data/msge/").adminAuth(test).go(200);

		// in default acl, users can update their own objects
		// admin can update any objects
		SpaceRequest.put("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.put("/1/data/msge/admin").backend(test).go(403);
		SpaceRequest.put("/1/data/msge/vince").userAuth(vince).body("t", "v3").go(200);
		SpaceRequest.put("/1/data/msge/admin").userAuth(vince).go(403);
		SpaceRequest.put("/1/data/msge/vince").adminAuth(test).body("t", "v4").go(200);
		SpaceRequest.put("/1/data/msge/admin").adminAuth(test).body("t", "a2").go(200);

		// in default acl, users can delete their own objects
		// admin can delete any objects
		SpaceRequest.delete("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.delete("/1/data/msge/admin").backend(test).go(403);
		SpaceRequest.delete("/1/data/msge/vince").userAuth(vince).go(200);
		SpaceRequest.delete("/1/data/msge/admin").userAuth(vince).go(403);
		SpaceRequest.delete("/1/data/msge/vince").adminAuth(test).go(404);
		SpaceRequest.delete("/1/data/msge/vince2").adminAuth(test).go(200);
		SpaceRequest.delete("/1/data/msge/admin").adminAuth(test).go(200);

		// vince creates a message before security tightens
		SpaceRequest.post("/1/data/msge?id=vince").userAuth(vince).body("t", "hello").go(201);

		// set message schema acl to empty
		messageSchema.acl(new SchemaAcl());
		test.schema().set(messageSchema);

		// check message schema acl are set
		settings = test.settings().get(SchemaSettings.class);
		assertEquals(1, settings.acl.size());
		assertEquals(0, settings.acl.get("msge").size());

		// in empty acl, nobody can create an object
		SpaceRequest.post("/1/data/msge").backend(test).go(403);
		SpaceRequest.post("/1/data/msge").userAuth(vince).go(403);
		SpaceRequest.post("/1/data/msge").adminAuth(test).go(403);

		// in empty acl, nobody can read an object
		SpaceRequest.get("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.get("/1/data/msge/vince").userAuth(vince).go(403);
		SpaceRequest.get("/1/data/msge/vince").adminAuth(test).go(403);

		// in empty acl, nobody can search for objects
		SpaceRequest.get("/1/data/msge/").backend(test).go(403);
		SpaceRequest.get("/1/data/msge/").userAuth(vince).go(403);
		SpaceRequest.get("/1/data/msge/").adminAuth(test).go(403);

		// in empty acl, nobody can update any object
		SpaceRequest.put("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.put("/1/data/msge/vince").userAuth(vince).go(403);
		SpaceRequest.put("/1/data/msge/vince").adminAuth(test).go(403);

		// in empty acl, nobody can delete any object
		SpaceRequest.delete("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.delete("/1/data/msge/vince").userAuth(vince).go(403);
		SpaceRequest.delete("/1/data/msge/vince").adminAuth(test).go(403);

		// set message schema new acl settings
		messageSchema.acl("admin", DataPermission.search);
		test.schema().set(messageSchema);

		// check message schema acl are set
		settings = test.settings().get(SchemaSettings.class);
		assertEquals(1, settings.acl.size());
		assertEquals(1, settings.acl.get("msge").size());
		assertEquals(Collections.singleton(DataPermission.search), //
				settings.acl.get("msge").get("admin"));

		// with this schema acl, nobody can create an object
		SpaceRequest.post("/1/data/msge").backend(test).go(403);
		SpaceRequest.post("/1/data/msge").userAuth(vince).go(403);
		SpaceRequest.post("/1/data/msge").adminAuth(test).go(403);

		// with this schema acl, nobody can read an object
		SpaceRequest.get("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.get("/1/data/msge/vince").userAuth(vince).go(403);
		SpaceRequest.get("/1/data/msge/vince").adminAuth(test).go(200);

		// with this schema acl, only admins can search for objects
		SpaceRequest.get("/1/data/msge/").backend(test).go(403);
		SpaceRequest.get("/1/data/msge/").userAuth(vince).go(403);
		SpaceRequest.get("/1/data/msge/").adminAuth(test).go(200);

		// with this schema acl, nobody can update any object
		SpaceRequest.put("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.put("/1/data/msge/vince").userAuth(vince).go(403);
		SpaceRequest.put("/1/data/msge/vince").adminAuth(test).go(403);

		// with this schema acl, nobody can delete any object
		SpaceRequest.delete("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.delete("/1/data/msge/vince").userAuth(vince).go(403);
		SpaceRequest.delete("/1/data/msge/vince").adminAuth(test).go(403);
	}

	@Test
	public void testDataAccessWithRolesAndPermissions() throws JsonProcessingException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

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

		test.schema().set(schema);

		// dave has the platine role
		// he's got all the rights
		SpaceDog dave = signUp(test, "dave", "hi dave");
		SpaceRequest.put("/1/credentials/" + dave.id() + "/roles/platine").adminAuth(test).go(200);
		SpaceRequest.post("/1/data/message?id=dave").userAuth(dave).body("text", "Dave").go(201);
		SpaceRequest.get("/1/data/message/dave").userAuth(dave).go(200);
		SpaceRequest.put("/1/data/message/dave").userAuth(dave).body("text", "Salut Dave").go(200);
		SpaceRequest.delete("/1/data/message/dave").userAuth(dave).go(200);

		// message for users without create permission
		SpaceRequest.post("/1/data/message?id=1").userAuth(dave).body("text", "Hello").go(201);

		// maelle is a simple user
		// she's got no right on the message schema
		SpaceDog maelle = signUp(test, "maelle", "hi maelle");
		SpaceRequest.post("/1/data/message").userAuth(maelle).body("text", "Maelle").go(403);
		SpaceRequest.get("/1/data/message/1").userAuth(maelle).go(403);
		SpaceRequest.put("/1/data/message/1").userAuth(maelle).body("text", "Salut Maelle").go(403);
		SpaceRequest.delete("/1/data/message/1").userAuth(maelle).go(403);

		// fred has the iron role
		// he's only got the right to read
		SpaceDog fred = signUp(test, "fred", "hi fred");
		SpaceRequest.put("/1/credentials/" + fred.id() + "/roles/iron").adminAuth(test).go(200);
		SpaceRequest.post("/1/data/message").userAuth(fred).body("text", "Fred").go(403);
		SpaceRequest.get("/1/data/message/1").userAuth(fred).go(200);
		SpaceRequest.put("/1/data/message/1").userAuth(fred).body("text", "Salut Fred").go(403);
		SpaceRequest.delete("/1/data/message/1").userAuth(fred).go(403);

		// nath has the silver role
		// she's got the right to read and update
		SpaceDog nath = signUp(test, "nath", "hi nath");
		SpaceRequest.put("/1/credentials/" + nath.id() + "/roles/silver").adminAuth(test).go(200);
		SpaceRequest.post("/1/data/message").userAuth(nath).body("text", "Nath").go(403);
		SpaceRequest.get("/1/data/message/1").userAuth(nath).go(200);
		SpaceRequest.put("/1/data/message/1").userAuth(nath).body("text", "Salut Nath").go(200);
		SpaceRequest.delete("/1/data/message/1").userAuth(nath).go(403);

		// vince has the gold role
		// he's got the right to create, read and update
		SpaceDog vince = signUp(test, "vince", "hi vince");
		SpaceRequest.put("/1/credentials/" + vince.id() + "/roles/gold").adminAuth(test).go(200);
		SpaceRequest.post("/1/data/message?id=vince").userAuth(vince).body("text", "Vince").go(201);
		SpaceRequest.get("/1/data/message/vince").userAuth(vince).go(200);
		SpaceRequest.put("/1/data/message/vince").userAuth(vince).body("text", "Salut Vince").go(200);
		SpaceRequest.delete("/1/data/message/vince").userAuth(vince).go(403);
	}

	@Test
	public void deleteSchemaDeletesItsAccessControlList() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// create message schema with simple acl
		Schema messageSchema = Schema.builder("message")//
				.acl("user", DataPermission.search)//
				.text("text")//
				.build();

		test.schema().set(messageSchema);

		// check schema settings contains message schema acl
		SchemaSettings settings = test.settings().get(SchemaSettings.class);
		assertEquals(messageSchema.acl(), settings.acl.get(messageSchema.name()));

		// delete message schema
		test.schema().delete(messageSchema);

		// check schema settings does not contain
		// message schema acl anymore
		settings = test.settings().get(SchemaSettings.class);
		assertNull(settings.acl.get(messageSchema.name()));
	}
}
