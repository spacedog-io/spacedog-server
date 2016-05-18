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

		Backend testBackend = SpaceClient.resetTestBackend();
		SpaceClient.initUserDefaultSchema(testBackend);

		ObjectNode message = SchemaBuilder2.builder("message")//
				.textProperty("text", "english", true).build();
		SpaceClient.setSchema(message, testBackend);

		ObjectNode rubric = SchemaBuilder2.builder("rubric")//
				.textProperty("name", "english", true).build();
		SpaceClient.setSchema(rubric, testBackend);

		// creates 4 messages, 1 rubric and 1 user

		SpaceClient.createUser(testBackend, "riri", "hi riri", "riri@disney.com");

		SpaceRequest.post("/1/data/rubric").adminAuth(testBackend)//
				.body(Json.object("name", "riri, fifi and loulou")).go(201);

		SpaceRequest.post("/1/data/message").adminAuth(testBackend)//
				.body(Json.object("text", "what's up?")).go(201);
		SpaceRequest.post("/1/data/message").adminAuth(testBackend)//
				.body(Json.object("text", "wanna drink something?")).go(201);
		SpaceRequest.post("/1/data/message").adminAuth(testBackend)//
				.body(Json.object("text", "pretty cool, hein?")).go(201);
		SpaceRequest.post("/1/data/message").adminAuth(testBackend)//
				.body(Json.object("text", "so long guys")).go(201);

		SpaceRequest.get("/1/data?refresh=true").adminAuth(testBackend).go(200)//
				.assertEquals(6, "total");

		// deletes messages containing 'up' by query

		ObjectNode query = Json.objectBuilder().object("query")//
				.object("match").put("text", "up").build();
		SpaceRequest.delete("/1/search/message").adminAuth(testBackend).body(query).go(200);
		SpaceRequest.get("/1/data/message?refresh=true").adminAuth(testBackend).go(200)//
				.assertEquals(3, "total");

		// deletes data objects containing 'wanna' or 'riri' but not users

		query = Json.objectBuilder().object("query")//
				.object("match").put("_all", "wanna riri").build();
		SpaceRequest.delete("/1/search").adminAuth(testBackend).body(query).go(200);
		SpaceRequest.get("/1/data?refresh=true").adminAuth(testBackend).go(200)//
				.assertEquals(3, "total");
	}

	@Test
	public void aggregateToGetDistinctUserEmails() throws Exception {

		// prepare backend

		SpaceClient.prepareTest();
		Backend testBackend = SpaceClient.resetTestBackend();
		SpaceClient.initUserDefaultSchema(testBackend);

		// creates 5 users but whith only 3 distinct emails

		SpaceClient.createUser(testBackend, "riri", "hi riri", "hello@disney.com");
		SpaceClient.createUser(testBackend, "fifi", "hi fifi", "hello@disney.com");
		SpaceClient.createUser(testBackend, "loulou", "hi loulou", "hello@disney.com");
		SpaceClient.createUser(testBackend, "donald", "hi donald", "donald@disney.com");
		SpaceClient.createUser(testBackend, "mickey", "hi mickey", "mickey@disney.com");

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

		SpaceRequest.post("/1/search?refresh=true").backend(testBackend).body(query).go(200)//
				.assertEquals(0, "results")//
				.assertEquals(3, "aggregations.distinctEmails.buckets")//
				.assertContainsValue("hello@disney.com", "key")//
				.assertContainsValue("donald@disney.com", "key")//
				.assertContainsValue("mickey@disney.com", "key");

	}

}