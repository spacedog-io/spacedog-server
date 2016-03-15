/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Backend;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder2;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class SearchResourceTest extends Assert {

	@Test
	public void searchAndDeleteObjects() throws Exception {

		// prepare

		SpaceDogHelper.prepareTest();
		Backend testAccount = SpaceDogHelper.resetTestBackend();
		ObjectNode message = SchemaBuilder2.builder("message")//
				.textProperty("text", "english", true).build();
		SpaceDogHelper.setSchema(message, testAccount);
		ObjectNode rubric = SchemaBuilder2.builder("rubric")//
				.textProperty("name", "english", true).build();
		SpaceDogHelper.setSchema(rubric, testAccount);

		// creates 4 messages, 1 rubric and 1 user

		SpaceDogHelper.createUser(testAccount, "riri", "hi riri", "riri@disney.com");

		SpaceRequest.post("/v1/data/rubric").basicAuth(testAccount)//
				.body(Json.object("name", "riri, fifi and loulou")).go(201);

		SpaceRequest.post("/v1/data/message").basicAuth(testAccount)//
				.body(Json.object("text", "what's up?")).go(201);
		SpaceRequest.post("/v1/data/message").basicAuth(testAccount)//
				.body(Json.object("text", "wanna drink something?")).go(201);
		SpaceRequest.post("/v1/data/message").basicAuth(testAccount)//
				.body(Json.object("text", "pretty cool, hein?")).go(201);
		SpaceRequest.post("/v1/data/message").basicAuth(testAccount)//
				.body(Json.object("text", "so long guys")).go(201);

		SpaceRequest.get("/v1/data?refresh=true").basicAuth(testAccount).go(200)//
				.assertEquals(7, "total");

		// deletes messages containing 'up' by query

		ObjectNode query = Json.objectBuilder().object("query")//
				.object("match").put("text", "up").build();
		SpaceRequest.delete("/v1/search/message").basicAuth(testAccount).body(query).go(200);
		SpaceRequest.get("/v1/data/message?refresh=true").basicAuth(testAccount).go(200)//
				.assertEquals(3, "total");

		// deletes data objects containing 'wanna' or 'riri' but not users

		query = Json.objectBuilder().object("query")//
				.object("match").put("_all", "wanna riri").build();
		SpaceRequest.delete("/v1/search").basicAuth(testAccount).body(query).go(200);
		SpaceRequest.get("/v1/data?refresh=true").basicAuth(testAccount).go(200)//
				.assertEquals(4, "total");
	}
}
