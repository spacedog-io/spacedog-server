/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.Schema;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.DataEndpoint.SearchResults;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.sdk.elastic.ESSearchSourceBuilder;
import io.spacedog.sdk.elastic.ESSortOrder;
import io.spacedog.utils.Json;

public class SearchResourceTestOften extends SpaceTest {

	@Test
	public void searchAndDeleteObjects() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		superadmin.schema().set(Schema.builder("message").text("text").build());
		superadmin.schema().set(Schema.builder("rubric").text("name").build());

		// creates 4 messages and 1 rubric
		superadmin.post("/1/data/rubric")//
				.bodyJson("name", "riri, fifi and loulou").go(201);
		superadmin.post("/1/data/message")//
				.bodyJson("text", "what's up?").go(201);
		superadmin.post("/1/data/message")//
				.bodyJson("text", "wanna drink something?").go(201);
		superadmin.post("/1/data/message")//
				.bodyJson("text", "pretty cool something, hein?").go(201);
		superadmin.post("/1/data/message")//
				.bodyJson("text", "so long guys").go(201);

		superadmin.get("/1/data").refresh().go(200)//
				.assertEquals(5, "total");

		// search for messages with full text query
		ObjectNode query = Json.objectBuilder()//
				.object("query").object("match").put("text", "something to drink")//
				.build();

		ObjectNode results = SpaceRequest.post("/1/search")//
				.debugServer().auth(superadmin).bodyJson(query).go(200)//
				.assertEquals("wanna drink something?", "results.0.text")//
				.assertEquals("pretty cool something, hein?", "results.1.text")//
				.asJsonObject();

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
		SpaceRequest.delete("/1/search/message").auth(superadmin).bodyJson(query).go(200);
		SpaceRequest.get("/1/data/message").refresh().auth(superadmin).go(200)//
				.assertEquals(3, "total");

		// deletes data objects containing 'wanna' or 'riri' but not users

		query = Json.objectBuilder().object("query")//
				.object("match").put("_all", "wanna riri").build();
		SpaceRequest.delete("/1/search").auth(superadmin).bodyJson(query).go(200);
		SpaceRequest.get("/1/data").refresh().auth(superadmin).go(200)//
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

		ObjectNode query = Json.objectBuilder()//
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
		for (int i = 0; i < 5; i++)
			test.data().create("number", Json.object("i", i, "t", "" + i));

		// search with ascendent sorting
		ESSearchSourceBuilder builder = ESSearchSourceBuilder.searchSource().sort("i");
		SearchResults<ObjectNode> results = test.data().search(//
				builder, ObjectNode.class, true);
		assertEquals(5, results.total());

		List<ObjectNode> objects = results.objects();
		for (int i = 0; i < objects.size(); i++) {
			assertEquals(i, objects.get(i).get("i").asInt());
			assertEquals(i, Json.get(objects.get(i), "meta.sort.0").asInt());
		}

		// search with descendant sorting
		builder = ESSearchSourceBuilder.searchSource().sort("t", ESSortOrder.DESC);
		results = test.data().search(builder, ObjectNode.class, true);
		assertEquals(5, results.total());

		objects = results.objects();
		for (int i = 0; i < objects.size(); i++) {
			assertEquals(4 - i, objects.get(i).get("i").asInt());
			assertEquals(String.valueOf(4 - i), //
					Json.get(objects.get(i), "meta.sort.0").asText());
		}
	}
}
