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
import io.spacedog.utils.Schema;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class SearchResourceTestOften extends Assert {

	@Test
	public void searchAndDeleteObjects() throws Exception {

		// prepare

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		SpaceClient.setSchema(//
				Schema.builder("message").text("text").build(), //
				test);

		SpaceClient.setSchema(//
				Schema.builder("rubric").text("name").build(), //
				test);

		// creates 4 messages and 1 rubric

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
				.assertEquals(5, "total");

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
				.assertEquals(2, "total");
	}

	@Test
	public void aggregateToGetDistinctCityNames() throws Exception {

		// prepare backend

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		SpaceClient.setSchema(//
				Schema.builder("city").string("name").build(), //
				test);

		// creates 5 cities but whith only 3 distinct names

		SpaceRequest.post("/1/data/city").backend(test).body("name", "Paris").go(201);
		SpaceRequest.post("/1/data/city").backend(test).body("name", "Bordeaux").go(201);
		SpaceRequest.post("/1/data/city").backend(test).body("name", "Nice").go(201);
		SpaceRequest.post("/1/data/city").backend(test).body("name", "Paris").go(201);
		SpaceRequest.post("/1/data/city").backend(test).body("name", "Nice").go(201);

		// search with 'terms' aggregation to get
		// all 3 distinct city names Paris, Bordeaux and Nice

		ObjectNode query = Json.objectBuilder()//
				.put("size", 0)//
				.object("aggs")//
				.object("distinctCities")//
				.object("terms")//
				.put("field", "name")//
				.build();

		SpaceRequest.post("/1/search").refresh().backend(test).body(query).go(200)//
				.assertEquals(0, "results")//
				.assertEquals(3, "aggregations.distinctCities.buckets")//
				.assertContainsValue("Paris", "key")//
				.assertContainsValue("Bordeaux", "key")//
				.assertContainsValue("Nice", "key");

	}

}
