/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder;

public class PushResourceOldTest extends Assert {

	// @Test
	public void subscribeToTopicsAndPush() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		SpaceClient.setSchema(buildSchema(), test);
		User vince = SpaceClient.newCredentials(test, "vince", "hi vince");
		User nath = SpaceClient.newCredentials(test, "nath", "hi nath");
		User fred = SpaceClient.newCredentials(test, "fred", "hi fred");
		User philippe = SpaceClient.newCredentials(test, "philippe", "hi philippe");

		// subscribe
		String vinceDeviceId = SpaceRequest.post("/1/device").userAuth(vince)//
				.body("endpoint", "vince-iphone", "appName", "spacedog-supervisor")//
				.go(200, 201).objectNode().get("id").asText();

		String fredDeviceId = SpaceRequest.post("/1/device").userAuth(fred)//
				.body("endpoint", "vince-iphone", "appName", "spacedog-supervisor")//
				.go(200, 201).objectNode().get("id").asText();

		String nathDeviceId = SpaceRequest.post("/1/device").userAuth(nath)//
				.body("endpoint", "vince-iphone", "appName", "spacedog-supervisor")//
				.go(200, 201).objectNode().get("id").asText();

		SpaceRequest.post("/1/data/message").backend(test)//
				.body(createMessage("vince")).go(201);

		SpaceRequest.post("/1/data/message").backend(test)//
				.body(createMessage("philippe")).go(201);

		ObjectNode pushBody = Json.objectBuilder()//
				.put("type", "message")//
				.object("query")//
				.object("match_all")//
				.end()//
				.end()//
				.array("properties")//
				.add("author")//
				.add("responses.author")//
				.end()//
				.put("message", "Hi dogs").build();

		SpaceRequest.post("/1/push").adminAuth(test).body(pushBody).go(200);

		// push all
		SpaceRequest.post("/1/device/push").adminAuth(test).go(200);
	}

	private ObjectNode buildSchema() {
		return SchemaBuilder.builder("message") //
				.property("text", "text").language("french").required().end() //
				.property("author", "string").required().end() //
				.objectProperty("responses").array() //
				.property("text", "text").language("french").required().end() //
				.property("author", "string").required().end() //
				.end() //
				.build();
	}

	private ObjectNode createMessage(String author) {
		return Json.objectBuilder()//
				.put("text", "Hello")//
				.put("author", author)//
				.array("responses")//
				.object()//
				.put("text", "What's up")//
				.put("author", "vince")//
				.end()//
				.object()//
				.put("text", "Hi yourself")//
				.put("author", "fred")//
				.end()//
				.build();
	}

}
