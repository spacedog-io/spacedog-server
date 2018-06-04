/**
 * © David Attias 2015
 */
package io.spacedog.test;

import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.DataPermission;
import io.spacedog.model.Schema;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.DataObject;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.sdk.elastic.ESSearchSourceBuilder;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Json7;

public class DataImportExportTest extends SpaceTest {

	@Test
	public void fullExportPreservingIds() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog fred = createTempUser(superadmin, "fred");

		// superadmin creates message1 schema
		superadmin.schema().set(messageSchema("message1"));

		// superadmin creates messages into message1 schema
		createMessages(superadmin);

		// superadmin exports message1 data objects
		String export = superadmin.post("/1/data/message1/_export")//
				.refresh().go(200).asString();

		// fred not authorized to export data
		// since only superadmins are allowed
		fred.post("/1/data/message1/_export").go(403);

		// superadmin creates message2 index
		superadmin.schema().set(messageSchema("message2"));

		// superadmin imports previously exported messages
		// into message2 schema with id preservation
		superadmin.post("/1/data/message2/_import")//
				.queryParam("preserveIds", "true")//
				.bodyString(export).go(200);

		// fred not authorized to import data
		// since only superadmins are allowed
		fred.post("/1/data/message1/_import").go(403);

		// superamdin gets all message1 messages
		List<Message> messages1 = superadmin.data().search("message1", //
				ESSearchSourceBuilder.searchSource(), Message.class, true)//
				.objects();

		// superamdin gets all message2 messages
		List<Message> messages2 = superadmin.data().search("message2", //
				ESSearchSourceBuilder.searchSource(), Message.class, true)//
				.objects();

		// check message1 and message2 contains the same messages
		assertEquals(messages1.size(), messages2.size());
		for (Message message : messages1)
			assertContains(messages2, message, true);

	}

	@Test
	public void partialExportWithoutIdPreservation() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// superadmin creates message1 schema
		superadmin.schema().set(messageSchema("message1"));

		// superadmin creates messages into message1 schema
		createMessages(superadmin);

		// superadmin exports message1 data objects
		// starting with an 'h'
		String export = superadmin.post("/1/data/message1/_export")//
				.bodyJson("prefix", Json7.object("text", "h"))//
				.refresh().go(200).asString();

		// superadmin creates message2 index
		superadmin.schema().set(messageSchema("message2"));

		// superadmin imports previously exported messages
		// into message2 schema without id preservation
		superadmin.post("/1/data/message2/_import")//
				.bodyString(export).go(200);

		// superadmin gets message1 messages but 'ola'
		ObjectNode searchSource = Json7.object("query", //
				Json7.object("prefix", Json7.object("text", "h")));

		List<Message> messages1 = superadmin.data().search("message1", //
				searchSource.toString(), Message.class, true)//
				.objects();

		// superadmin gets all message2 messages
		List<Message> messages2 = superadmin.data().search("message2", //
				ESSearchSourceBuilder.searchSource(), Message.class, true)//
				.objects();

		// check message1 and message2 contains the same messages
		// minus 'ola' message and without id preservation
		assertEquals(4, messages2.size());
		for (Message message : messages1)
			assertContains(messages2, message, false);

	}

	private void createMessages(SpaceDog superadmin) {
		superadmin.data().create("message1", new Message("hi"));
		superadmin.data().create("message1", new Message("hello"));
		superadmin.data().create("message1", new Message("ola"));
		superadmin.data().create("message1", new Message("hi \"friends\""));
		superadmin.data().create("message1", new Message("hi\nhello\nola"));
	}

	private void assertContains(List<Message> messages2, Message message1, boolean compareId) {
		for (Message message2 : messages2) {
			if (isEqual(message1, message2, compareId))
				return;
		}
		fail(String.format("message [%s] not found", message1.text));
	}

	private boolean isEqual(Message message1, Message message2, boolean compareId) {
		return message1.text.equals(message2.text) //
				&& message1.createdAt().equals(message2.createdAt()) //
				&& message1.updatedAt().equals(message2.updatedAt()) //
				&& message1.createdBy().equals(message2.createdBy()) //
				&& message1.updatedBy().equals(message2.updatedBy()) //
				&& (compareId == false || message1.id().equals(message2.id()));
	}

	public static Schema messageSchema(String name) {
		return Schema.builder(name)//
				.text("text")//
				.acl(Credentials.SUPER_ADMIN, DataPermission.create, DataPermission.search)//
				.build();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Message extends DataObject<Message> {
		public String text;

		public Message() {
		}

		public Message(String text) {
			this.text = text;
		}
	}

}
