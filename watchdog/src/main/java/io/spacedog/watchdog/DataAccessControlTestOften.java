/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.spacedog.model.DataPermission;
import io.spacedog.model.Schema;
import io.spacedog.model.Schema.SchemaAcl;
import io.spacedog.model.SchemaSettings;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

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
		SpaceRequest.post("/1/data/msge").backend(test).bodyJson("t", "hello").go(403);
		SpaceRequest.post("/1/data/msge").id("vince").auth(vince).bodyJson("t", "v1").go(201);
		SpaceRequest.post("/1/data/msge").id("vince2").auth(vince).bodyJson("t", "v2").go(201);
		SpaceRequest.post("/1/data/msge").id("admin").auth(test).bodyJson("t", "a1").go(201);

		// in default acl, everyone can read any objects
		SpaceRequest.get("/1/data/msge/vince").backend(test).go(200);
		SpaceRequest.get("/1/data/msge/admin").backend(test).go(200);
		SpaceRequest.get("/1/data/msge/vince").auth(vince).go(200);
		SpaceRequest.get("/1/data/msge/admin").auth(vince).go(200);
		SpaceRequest.get("/1/data/msge/vince").auth(test).go(200);
		SpaceRequest.get("/1/data/msge/admin").auth(test).go(200);

		// in default acl, only users and admins can search for objects
		SpaceRequest.get("/1/data/msge/").backend(test).go(403);
		SpaceRequest.get("/1/data/msge/").auth(vince).go(200);
		SpaceRequest.get("/1/data/msge/").auth(test).go(200);

		// in default acl, users can update their own objects
		// admin can update any objects
		SpaceRequest.put("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.put("/1/data/msge/admin").backend(test).go(403);
		SpaceRequest.put("/1/data/msge/vince").auth(vince).bodyJson("t", "v3").go(200);
		SpaceRequest.put("/1/data/msge/admin").auth(vince).go(403);
		SpaceRequest.put("/1/data/msge/vince").auth(test).bodyJson("t", "v4").go(200);
		SpaceRequest.put("/1/data/msge/admin").auth(test).bodyJson("t", "a2").go(200);

		// in default acl, users can delete their own objects
		// admin can delete any objects
		SpaceRequest.delete("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.delete("/1/data/msge/admin").backend(test).go(403);
		SpaceRequest.delete("/1/data/msge/vince").auth(vince).go(200);
		SpaceRequest.delete("/1/data/msge/admin").auth(vince).go(403);
		SpaceRequest.delete("/1/data/msge/vince").auth(test).go(404);
		SpaceRequest.delete("/1/data/msge/vince2").auth(test).go(200);
		SpaceRequest.delete("/1/data/msge/admin").auth(test).go(200);

		// vince creates a message before security tightens
		SpaceRequest.post("/1/data/msge").id("vince").auth(vince).bodyJson("t", "hello").go(201);

		// set message schema acl to empty
		messageSchema.acl(new SchemaAcl());
		test.schema().set(messageSchema);

		// check message schema acl are set
		settings = test.settings().get(SchemaSettings.class);
		assertEquals(1, settings.acl.size());
		assertEquals(0, settings.acl.get("msge").size());

		// in empty acl, nobody can create an object
		SpaceRequest.post("/1/data/msge").backend(test).go(403);
		SpaceRequest.post("/1/data/msge").auth(vince).go(403);
		SpaceRequest.post("/1/data/msge").auth(test).go(403);

		// in empty acl, nobody can read an object
		SpaceRequest.get("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.get("/1/data/msge/vince").auth(vince).go(403);
		SpaceRequest.get("/1/data/msge/vince").auth(test).go(403);

		// in empty acl, nobody can search for objects
		SpaceRequest.get("/1/data/msge/").backend(test).go(403);
		SpaceRequest.get("/1/data/msge/").auth(vince).go(403);
		SpaceRequest.get("/1/data/msge/").auth(test).go(403);

		// in empty acl, nobody can update any object
		SpaceRequest.put("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.put("/1/data/msge/vince").auth(vince).go(403);
		SpaceRequest.put("/1/data/msge/vince").auth(test).go(403);

		// in empty acl, nobody can delete any object
		SpaceRequest.delete("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.delete("/1/data/msge/vince").auth(vince).go(403);
		SpaceRequest.delete("/1/data/msge/vince").auth(test).go(403);

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
		SpaceRequest.post("/1/data/msge").auth(vince).go(403);
		SpaceRequest.post("/1/data/msge").auth(test).go(403);

		// with this schema acl, nobody can read an object
		SpaceRequest.get("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.get("/1/data/msge/vince").auth(vince).go(403);
		SpaceRequest.get("/1/data/msge/vince").auth(test).go(200);

		// with this schema acl, only admins can search for objects
		SpaceRequest.get("/1/data/msge/").backend(test).go(403);
		SpaceRequest.get("/1/data/msge/").auth(vince).go(403);
		SpaceRequest.get("/1/data/msge/").auth(test).go(200);

		// with this schema acl, nobody can update any object
		SpaceRequest.put("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.put("/1/data/msge/vince").auth(vince).go(403);
		SpaceRequest.put("/1/data/msge/vince").auth(test).go(403);

		// with this schema acl, nobody can delete any object
		SpaceRequest.delete("/1/data/msge/vince").backend(test).go(403);
		SpaceRequest.delete("/1/data/msge/vince").auth(vince).go(403);
		SpaceRequest.delete("/1/data/msge/vince").auth(test).go(403);
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
		SpaceRequest.put("/1/credentials/" + dave.id() + "/roles/platine").auth(test).go(200);
		SpaceRequest.post("/1/data/message").id("dave").auth(dave).bodyJson("text", "Dave").go(201);
		SpaceRequest.get("/1/data/message/dave").auth(dave).go(200);
		SpaceRequest.put("/1/data/message/dave").auth(dave).bodyJson("text", "Salut Dave").go(200);
		SpaceRequest.delete("/1/data/message/dave").auth(dave).go(200);

		// message for users without create permission
		SpaceRequest.post("/1/data/message").id("1").auth(dave).bodyJson("text", "Hello").go(201);

		// maelle is a simple user
		// she's got no right on the message schema
		SpaceDog maelle = signUp(test, "maelle", "hi maelle");
		SpaceRequest.post("/1/data/message").auth(maelle).bodyJson("text", "Maelle").go(403);
		SpaceRequest.get("/1/data/message/1").auth(maelle).go(403);
		SpaceRequest.put("/1/data/message/1").auth(maelle).bodyJson("text", "Salut Maelle").go(403);
		SpaceRequest.delete("/1/data/message/1").auth(maelle).go(403);

		// fred has the iron role
		// he's only got the right to read
		SpaceDog fred = signUp(test, "fred", "hi fred");
		SpaceRequest.put("/1/credentials/" + fred.id() + "/roles/iron").auth(test).go(200);
		SpaceRequest.post("/1/data/message").auth(fred).bodyJson("text", "Fred").go(403);
		SpaceRequest.get("/1/data/message/1").auth(fred).go(200);
		SpaceRequest.put("/1/data/message/1").auth(fred).bodyJson("text", "Salut Fred").go(403);
		SpaceRequest.delete("/1/data/message/1").auth(fred).go(403);

		// nath has the silver role
		// she's got the right to read and update
		SpaceDog nath = signUp(test, "nath", "hi nath");
		SpaceRequest.put("/1/credentials/" + nath.id() + "/roles/silver").auth(test).go(200);
		SpaceRequest.post("/1/data/message").auth(nath).bodyJson("text", "Nath").go(403);
		SpaceRequest.get("/1/data/message/1").auth(nath).go(200);
		SpaceRequest.put("/1/data/message/1").auth(nath).bodyJson("text", "Salut Nath").go(200);
		SpaceRequest.delete("/1/data/message/1").auth(nath).go(403);

		// vince has the gold role
		// he's got the right to create, read and update
		SpaceDog vince = signUp(test, "vince", "hi vince");
		SpaceRequest.put("/1/credentials/" + vince.id() + "/roles/gold").auth(test).go(200);
		SpaceRequest.post("/1/data/message").id("vince").auth(vince).bodyJson("text", "Vince").go(201);
		SpaceRequest.get("/1/data/message/vince").auth(vince).go(200);
		SpaceRequest.put("/1/data/message/vince").auth(vince).bodyJson("text", "Salut Vince").go(200);
		SpaceRequest.delete("/1/data/message/vince").auth(vince).go(403);
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
