/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTest;
import io.spacedog.utils.Json;
import io.spacedog.utils.Schema;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class SearchResourceTestOften extends SpaceTest {

	@Test
	public void searchAndDeleteObjects() {

		// prepare

		prepareTest();
		Backend test = resetTestBackend();

		setSchema(//
				Schema.builder("message").text("text").build(), //
				test);

		setSchema(//
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
				.body("text", "pretty cool something, hein?").go(201);
		SpaceRequest.post("/1/data/message").adminAuth(test)//
				.body("text", "so long guys").go(201);

		SpaceRequest.get("/1/data").refresh().adminAuth(test).go(200)//
				.assertEquals(5, "total");

		// search for messages with full text query
		ObjectNode query = Json.objectBuilder()//
				.object("query").object("match").put("text", "something to drink")//
				.build();

		ObjectNode results = SpaceRequest.post("/1/search")//
				.debugServer().adminAuth(test).body(query).go(200)//
				.assertEquals("wanna drink something?", "results.0.text")//
				.assertEquals("pretty cool something, hein?", "results.1.text")//
				.objectNode();

		// check search scores
		assertTrue(Json.checkDouble(Json.get(results, "results.0.meta.score")) > 1);
		assertTrue(Json.checkDouble(Json.get(results, "results.1.meta.score")) < 1);

		// check all meta are there
		assertNotNull(Json.get(results, "results.0.meta.id"));
		assertNotNull(Json.get(results, "results.0.meta.type"));
		assertNotNull(Json.get(results, "results.0.meta.version"));
		assertNotNull(Json.get(results, "results.0.meta.createdBy"));
		assertNotNull(Json.get(results, "results.0.meta.createdAt"));
		assertNotNull(Json.get(results, "results.0.meta.updatedBy"));
		assertNotNull(Json.get(results, "results.0.meta.updatedAt"));

		// deletes messages containing 'up' by query

		query = Json.objectBuilder().object("query")//
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
	public void aggregateToGetDistinctCityNames() {

		// prepare backend

		prepareTest();
		Backend test = resetTestBackend();
		User vince = signUp(test, "vince", "hi vince");

		setSchema(//
				Schema.builder("city").string("name").build(), //
				test);

		// creates 5 cities but whith only 3 distinct names

		SpaceRequest.post("/1/data/city").userAuth(vince).body("name", "Paris").go(201);
		SpaceRequest.post("/1/data/city").userAuth(vince).body("name", "Bordeaux").go(201);
		SpaceRequest.post("/1/data/city").userAuth(vince).body("name", "Nice").go(201);
		SpaceRequest.post("/1/data/city").userAuth(vince).body("name", "Paris").go(201);
		SpaceRequest.post("/1/data/city").userAuth(vince).body("name", "Nice").go(201);

		// search with 'terms' aggregation to get
		// all 3 distinct city names Paris, Bordeaux and Nice

		ObjectNode query = Json.objectBuilder()//
				.put("size", 0)//
				.object("aggs")//
				.object("distinctCities")//
				.object("terms")//
				.put("field", "name")//
				.build();

		SpaceRequest.post("/1/search").refresh().userAuth(vince).body(query).go(200)//
				.assertEquals(0, "results")//
				.assertEquals(3, "aggregations.distinctCities.buckets")//
				.assertContainsValue("Paris", "key")//
				.assertContainsValue("Bordeaux", "key")//
				.assertContainsValue("Nice", "key");

	}

	@Test
	public void sortSearchResults() {

		// prepare
		prepareTest();
		Backend test = resetTestBackend();

		setSchema(//
				Schema.builder("number").integer("i").string("t").build(), //
				test);

		// creates 5 numbers
		for (int i = 0; i < 3; i++)
			SpaceRequest.post("/1/data/number").adminAuth(test)//
					.body("i", i, "t", "" + i).go(201);

		// search with ascendent sorting
		ObjectNode query = Json.objectBuilder()//
				.array("sort").add("i").end()//
				.object("query").object("match_all")//
				.build();

		SpaceRequest.post("/1/search").refresh()//
				.adminAuth(test).body(query).go(200)//
				.assertEquals(0, "results.0.i")//
				.assertEquals(0, "results.0.meta.sort.0")//
				.assertEquals(1, "results.1.i")//
				.assertEquals(1, "results.1.meta.sort.0")//
				.assertEquals(2, "results.2.i")//
				.assertEquals(2, "results.2.meta.sort.0");

		// search with descendant sorting
		query = Json.objectBuilder()//
				.array("sort").object().put("t", "desc").end().end()//
				.object("query").object("match_all")//
				.build();

		SpaceRequest.post("/1/search").refresh()//
				.adminAuth(test).body(query).go(200)//
				.assertEquals(2, "results.0.i")//
				.assertEquals("2", "results.0.meta.sort.0")//
				.assertEquals(1, "results.1.i")//
				.assertEquals("1", "results.1.meta.sort.0")//
				.assertEquals(0, "results.2.i")//
				.assertEquals("0", "results.2.meta.sort.0");
	}
}
