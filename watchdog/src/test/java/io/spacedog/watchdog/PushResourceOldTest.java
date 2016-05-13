/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder;

public class PushResourceOldTest extends Assert {

	// @Test
	public void subscribeToTopicsAndPush() throws Exception {

		// prepare
		SpaceDogHelper.prepareTest();
		SpaceDogHelper.Backend testBackend = SpaceDogHelper.resetTestBackend();
		SpaceDogHelper.setSchema(buildSchema(), testBackend);
		SpaceDogHelper.User vince = SpaceDogHelper.createUser(testBackend, "vince", "hi vince", "david@spacedog.io");
		SpaceDogHelper.User nath = SpaceDogHelper.createUser(testBackend, "nath", "hi nath", "attias666@gmail.com");
		SpaceDogHelper.User fred = SpaceDogHelper.createUser(testBackend, "fred", "hi fred", "davattias@gmail.com");
		SpaceDogHelper.User philippe = SpaceDogHelper.createUser(testBackend, "philippe", "hi philippe",
				"davattias@gmail.com");

		// subscribe
		String vinceDeviceId = SpaceRequest.post("/1/device").backend(testBackend).userAuth(vince)//
				.body(Json.objectBuilder().put("endpoint", "vince-iphone")//
						.put("appName", "spacedog-supervisor"))//
				.go(200, 201).objectNode().get("id").asText();

		String fredDeviceId = SpaceRequest.post("/1/device").backend(testBackend).userAuth(fred)//
				.body(Json.objectBuilder().put("endpoint", "vince-iphone")//
						.put("appName", "spacedog-supervisor"))//
				.go(200, 201).objectNode().get("id").asText();

		String nathDeviceId = SpaceRequest.post("/1/device").backend(testBackend).userAuth(nath)//
				.body(Json.objectBuilder().put("endpoint", "vince-iphone")//
						.put("appName", "spacedog-supervisor"))//
				.go(200, 201).objectNode().get("id").asText();

		SpaceRequest.post("/1/data/message").backend(testBackend)//
				.body(createMessage("vince")).go(201);

		SpaceRequest.post("/1/data/message").backend(testBackend)//
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

		SpaceRequest.post("/1/push").adminAuth(testBackend)//
				.body(pushBody).go(200);

		// push all
		SpaceRequest.post("/1/device/push").adminAuth(testBackend).go(200);
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
