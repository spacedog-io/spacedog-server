/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder2;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class SearchResourceTestOften extends Assert {

	@Test
	public void searchAndDeleteObjects() throws Exception {

		// prepare

		SpaceClient.prepareTest();

		Backend test = SpaceClient.resetTestBackend();
		SpaceClient.initUserDefaultSchema(test);

		ObjectNode message = SchemaBuilder2.builder("message")//
				.textProperty("text", "english", true).build();
		SpaceClient.setSchema(message, test);

		ObjectNode rubric = SchemaBuilder2.builder("rubric")//
				.textProperty("name", "english", true).build();
		SpaceClient.setSchema(rubric, test);

		// creates 4 messages, 1 rubric and 1 user

		SpaceClient.createUser(test, "riri", "hi riri");

		SpaceRequest.post("/1/data/rubric").adminAuth(test)//
				.body("name", "riri, fifi and loulou").go(201);

		SpaceRequest.post("/1/data/message").adminAuth(test)//
				.body("text", "what's up?").go(201);
		SpaceRequest.post("/1/data/message").adminAuth(test)//
				.body("text", "wanna drink something?").go(201);
		SpaceRequest.post("/1/data/message").adminAuth(test)//
				.body("text", "pretty cool, hein?").go(201);
		SpaceRequest.post("/1/data/message").adminAuth(test)//
				.body("text", "so long guys").go(201);

		SpaceRequest.get("/1/data").refresh().adminAuth(test).go(200)//
				.assertEquals(6, "total");

		// deletes messages containing 'up' by query

		ObjectNode query = Json.objectBuilder().object("query")//
				.object("match").put("text", "up").build();
		SpaceRequest.delete("/1/search/message").adminAuth(test).body(query).go(200);
		SpaceRequest.get("/1/data/message").refresh().adminAuth(test).go(200)//
				.assertEquals(3, "total");

		// deletes data objects containing 'wanna' or 'riri' but not users

		query = Json.objectBuilder().object("query")//
				.object("match").put("_all", "wanna riri").build();
		SpaceRequest.delete("/1/search").adminAuth(test).body(query).go(200);
		SpaceRequest.get("/1/data").refresh().adminAuth(test).go(200)//
				.assertEquals(3, "total");
	}

	@Test
	public void aggregateToGetDistinctUserEmails() throws Exception {

		// prepare backend

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		SpaceClient.initUserDefaultSchema(test);

		// creates 5 users but whith only 3 distinct emails

		SpaceClient.createUser(test, "riri", "hi riri", "hello@disney.com");
		SpaceClient.createUser(test, "fifi", "hi fifi", "hello@disney.com");
		SpaceClient.createUser(test, "loulou", "hi loulou", "hello@disney.com");
		SpaceClient.createUser(test, "donald", "hi donald", "donald@disney.com");
		SpaceClient.createUser(test, "mickey", "hi mickey", "mickey@disney.com");

		// search with 'terms' aggregation to get
		// all 3 distinct emails hello, donald and mickey @disney.com
		// the super admin who has created the backend is not a user

		ObjectNode query = Json.objectBuilder()//
				.put("size", 0)//
				.object("aggs")//
				.object("distinctEmails")//
				.object("terms")//
				.put("field", "email")//
				.build();

		SpaceRequest.post("/1/search").refresh().backend(test).body(query).go(200)//
				.assertEquals(0, "results")//
				.assertEquals(3, "aggregations.distinctEmails.buckets")//
				.assertContainsValue("hello@disney.com", "key")//
				.assertContainsValue("donald@disney.com", "key")//
				.assertContainsValue("mickey@disney.com", "key");

	}

}
