/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.Schema;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json7;

public class SearchResourceTestOften extends SpaceTest {

	@Test
	public void searchAndDeleteObjects() {

		// prepare

		prepareTest();
		SpaceDog test = resetTestBackend();

		test.schema().set(Schema.builder("message").text("text").build());

		test.schema().set(Schema.builder("rubric").text("name").build());

		// creates 4 messages and 1 rubric

		SpaceRequest.post("/1/data/rubric").auth(test)//
				.bodyJson("name", "riri, fifi and loulou").go(201);

		SpaceRequest.post("/1/data/message").auth(test)//
				.bodyJson("text", "what's up?").go(201);
		SpaceRequest.post("/1/data/message").auth(test)//
				.bodyJson("text", "wanna drink something?").go(201);
		SpaceRequest.post("/1/data/message").auth(test)//
				.bodyJson("text", "pretty cool something, hein?").go(201);
		SpaceRequest.post("/1/data/message").auth(test)//
				.bodyJson("text", "so long guys").go(201);

		SpaceRequest.get("/1/data").refresh().auth(test).go(200)//
				.assertEquals(5, "total");

		// search for messages with full text query
		ObjectNode query = Json7.objectBuilder()//
				.object("query").object("match").put("text", "something to drink")//
				.build();

		ObjectNode results = SpaceRequest.post("/1/search")//
		.debugServer().auth(test).bodyJson(query).go(200)//
				.assertEquals("wanna drink something?", "results.0.text")//
				.assertEquals("pretty cool something, hein?", "results.1.text")//
				.asJsonObject();

		// check search scores
		assertTrue(Json7.checkDouble(Json7.get(results, "results.0.meta.score")) > 1);
		assertTrue(Json7.checkDouble(Json7.get(results, "results.1.meta.score")) < 1);

		// check all meta are there
		assertNotNull(Json7.get(results, "results.0.meta.id"));
		assertNotNull(Json7.get(results, "results.0.meta.type"));
		assertNotNull(Json7.get(results, "results.0.meta.version"));
		assertNotNull(Json7.get(results, "results.0.meta.createdBy"));
		assertNotNull(Json7.get(results, "results.0.meta.createdAt"));
		assertNotNull(Json7.get(results, "results.0.meta.updatedBy"));
		assertNotNull(Json7.get(results, "results.0.meta.updatedAt"));

		// deletes messages containing 'up' by query

		query = Json7.objectBuilder().object("query")//
				.object("match").put("text", "up").build();
		SpaceRequest.delete("/1/search/message").auth(test).bodyJson(query).go(200);
		SpaceRequest.get("/1/data/message").refresh().auth(test).go(200)//
				.assertEquals(3, "total");

		// deletes data objects containing 'wanna' or 'riri' but not users

		query = Json7.objectBuilder().object("query")//
				.object("match").put("_all", "wanna riri").build();
		SpaceRequest.delete("/1/search").auth(test).bodyJson(query).go(200);
		SpaceRequest.get("/1/data").refresh().auth(test).go(200)//
				.assertEquals(2, "total");
	}

	@Test
	public void aggregateToGetDistinctCityNames() {

		// prepare backend

		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog vince = signUp(test, "vince", "hi vince");

		test.schema().set(Schema.builder("city").string("name").build());

		// creates 5 cities but whith only 3 distinct names

		SpaceRequest.post("/1/data/city").auth(vince).bodyJson("name", "Paris").go(201);
		SpaceRequest.post("/1/data/city").auth(vince).bodyJson("name", "Bordeaux").go(201);
		SpaceRequest.post("/1/data/city").auth(vince).bodyJson("name", "Nice").go(201);
		SpaceRequest.post("/1/data/city").auth(vince).bodyJson("name", "Paris").go(201);
		SpaceRequest.post("/1/data/city").auth(vince).bodyJson("name", "Nice").go(201);

		// search with 'terms' aggregation to get
		// all 3 distinct city names Paris, Bordeaux and Nice

		ObjectNode query = Json7.objectBuilder()//
				.put("size", 0)//
				.object("aggs")//
				.object("distinctCities")//
				.object("terms")//
				.put("field", "name")//
				.build();

		SpaceRequest.post("/1/search").refresh().auth(vince).bodyJson(query).go(200)//
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
		SpaceDog test = resetTestBackend();

		test.schema().set(Schema.builder("number").integer("i").string("t").build());

		// creates 5 numbers
		for (int i = 0; i < 3; i++)
			SpaceRequest.post("/1/data/number").auth(test)//
					.bodyJson("i", i, "t", "" + i).go(201);

		// search with ascendent sorting
		ObjectNode query = Json7.objectBuilder()//
				.array("sort").add("i").end()//
				.object("query").object("match_all")//
				.build();

		SpaceRequest.post("/1/search").refresh().auth(test).bodyJson(query).go(200)//
				.assertEquals(0, "results.0.i")//
				.assertEquals(0, "results.0.meta.sort.0")//
				.assertEquals(1, "results.1.i")//
				.assertEquals(1, "results.1.meta.sort.0")//
				.assertEquals(2, "results.2.i")//
				.assertEquals(2, "results.2.meta.sort.0");

		// search with descendant sorting
		query = Json7.objectBuilder()//
				.array("sort").object().put("t", "desc").end().end()//
				.object("query").object("match_all")//
				.build();

		SpaceRequest.post("/1/search").refresh().auth(test).bodyJson(query).go(200)//
				.assertEquals(2, "results.0.i")//
				.assertEquals("2", "results.0.meta.sort.0")//
				.assertEquals(1, "results.1.i")//
				.assertEquals("1", "results.1.meta.sort.0")//
				.assertEquals(0, "results.2.i")//
				.assertEquals("0", "results.2.meta.sort.0");
	}
}
