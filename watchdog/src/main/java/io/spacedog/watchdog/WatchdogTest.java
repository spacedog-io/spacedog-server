/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.model.Schema;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.DataObject;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.SpaceHeaders;

public class WatchdogTest extends SpaceTest {

	public static class Message extends DataObject<Message> {
		public String text;

		public Message() {
		}

		public Message(String text) {
			this.text = text;
		}

		public static Schema schema() {
			return Schema.builder("message").text("text").build();
		}
	}

	@Test
	public void shouldCreateReadSearchAndDeleteDataMessages() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend("v0test");
		SpaceDog vince = createTempUser(superadmin, "vince");

		// set message schema
		superadmin.schema().set(Message.schema());

		// in default acl, only users and admins can create objects
		Message hi = vince.data().save(new Message("hi"));
		Message ola = superadmin.data().save(new Message("ola"));

		// vince reads hi message
		Message message = vince.data().get(Message.class, hi.id());
		assertEquals("hi", message.text);

		// vince reads ola message
		message = vince.data().get(Message.class, ola.id());
		assertEquals("ola", message.text);

		// vince searches for messages
		List<Message> messages = vince.data().getAll()//
				.type("message").refresh().get(Message.class);

		assertEquals(2, messages.size());

		// vince updates hi message
		hi.text = "hello";
		vince.data().save(hi);

		Message hello = vince.data().get(Message.class, hi.id());
		assertEquals("hello", hello.text);

		// vince deletes ola message
		vince.data().delete(hi);

		// vince searches for messages
		messages = vince.data().getAll()//
				.type("message").refresh().get(Message.class);

		assertEquals(1, messages.size());
		assertEquals(ola.id(), messages.get(0).id());
		assertEquals("ola", messages.get(0).text);
	}

	@Test
	public void shouldCreatAndDeleteCredentials() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend("v0test");
		SpaceDog vince = createTempUser(superadmin, "vince");

		// vince logs in
		vince.login();

		// superadmin deletes vince's credentials
		superadmin.credentials().delete(vince.id());

		// vince fails to log in again
		vince.get("/1/login").go(401);
	}

	private static final String FILE_CONTENT = "This is a test file!";

	@Test
	public void shouldShareFiles() throws IOException {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend("v0test");
		SpaceDog vince = createTempUser(superadmin, "vince");

		// backend is brand new, no shared files
		superadmin.get("/1/share").go(200)//
				.assertSizeEquals(0, "results");

		// share small text file
		JsonNode json = vince.put("/1/share/test.txt")//
				.bodyBytes(FILE_CONTENT.getBytes())//
				.go(200).asJson();

		String txtLocation = json.get("location").asText();
		String txtS3Location = json.get("s3").asText();

		// superadmin lists shares
		superadmin.get("/1/share").go(200)//
				.assertSizeEquals(1, "results");

		// download shared text file
		String stringContent = SpaceRequest.get(txtLocation).backend(superadmin).go(200)//
				.assertHeaderEquals("text/plain", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals("vince", SpaceHeaders.SPACEDOG_OWNER)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// download shared text file through direct S3 access
		stringContent = SpaceRequest.get(txtS3Location).go(200)//
				.assertHeaderEquals("text/plain", SpaceHeaders.CONTENT_TYPE)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// owner (vince) can delete its own shared file (test.txt)
		vince.delete(txtLocation).go(200);

		// no more shares
		superadmin.get("/1/share").go(200)//
				.assertSizeEquals(0, "results");
	}
}
