/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.elastic.ESSortOrder;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceTest;
import io.spacedog.model.JsonDataObject;
import io.spacedog.model.Schema;
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
		ObjectNode query = Json.builder().object()//
				.object("query").object("match").add("text", "something to drink")//
				.build();

		ObjectNode results = SpaceRequest.post("/1/search")//
				.debugServer().auth(superadmin).bodyJson(query).go(200)//
				.assertEquals("wanna drink something?", "results.0.source.text")//
				.assertEquals("pretty cool something, hein?", "results.1.source.text")//
				.asJsonObject();

		// check search scores
		assertTrue(Json.checkDouble(Json.get(results, "results.0.score")) > 1);
		assertTrue(Json.checkDouble(Json.get(results, "results.1.score")) < 1);

		// check all meta are there
		assertNotNull(Json.get(results, "results.0.id"));
		assertNotNull(Json.get(results, "results.0.type"));
		assertNotNull(Json.get(results, "results.0.version"));
		assertNotNull(Json.get(results, "results.0.source.owner"));
		assertNotNull(Json.get(results, "results.0.source.createdAt"));
		assertNotNull(Json.get(results, "results.0.source.updatedAt"));

		// deletes messages containing 'up' by query

		query = Json.builder().object().object("query")//
				.object("match").add("text", "up").build();
		SpaceRequest.delete("/1/search/message").auth(superadmin).bodyJson(query).go(200);
		SpaceRequest.get("/1/data/message").refresh().auth(superadmin).go(200)//
				.assertEquals(3, "total");

		// deletes data objects containing 'wanna' or 'riri' but not users

		query = Json.builder().object().object("query")//
				.object("match").add("_all", "wanna riri").build();
		SpaceRequest.delete("/1/search").auth(superadmin).bodyJson(query).go(200);
		SpaceRequest.get("/1/data").refresh().auth(superadmin).go(200)//
				.assertEquals(2, "total");
	}

	@Test
	public void aggregateToGetDistinctCityNames() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog vince = signUp(superadmin, "vince", "hi vince");

		superadmin.schema().set(Schema.builder("city").string("name").build());

		// creates 5 cities but whith only 3 distinct names
		vince.data().save("city", Json.object("name", "Paris"));
		vince.data().save("city", Json.object("name", "Bordeaux"));
		vince.data().save("city", Json.object("name", "Nice"));
		vince.data().save("city", Json.object("name", "Paris"));
		vince.data().save("city", Json.object("name", "Nice"));

		// search with 'terms' aggregation to get
		// all 3 distinct city names Paris, Bordeaux and Nice
		ObjectNode query = Json.builder().object()//
				.add("size", 0)//
				.object("aggs")//
				.object("distinctCities")//
				.object("terms")//
				.add("field", "name")//
				.build();

		vince.post("/1/search").refresh().bodyJson(query).go(200)//
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
			test.data().save("number", Json.object("i", i, "t", "" + i));

		// search with ascendent sorting
		ESSearchSourceBuilder builder = ESSearchSourceBuilder.searchSource().sort("i");
		JsonDataObject.Results results = test.data().search(//
				builder, JsonDataObject.Results.class, true);
		assertEquals(5, results.total);

		List<JsonDataObject> objects = results.results;
		for (int i = 0; i < objects.size(); i++) {
			assertEquals(i, objects.get(i).source().get("i").asInt());
			assertEquals(i, objects.get(i).sort()[0]);
		}

		// search with descendant sorting
		builder = ESSearchSourceBuilder.searchSource().sort("t", ESSortOrder.DESC);
		results = test.data().search(builder, JsonDataObject.Results.class, true);
		assertEquals(5, results.total);

		objects = results.results;
		for (int i = 0; i < objects.size(); i++) {
			assertEquals(4 - i, objects.get(i).source().get("i").asInt());
			assertEquals(String.valueOf(4 - i), objects.get(i).sort()[0]);
		}
	}
}
