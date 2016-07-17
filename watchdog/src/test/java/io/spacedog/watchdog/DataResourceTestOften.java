/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class DataResourceTestOften extends Assert {

	@Test
	public void createFindUpdateAndDelete() throws Exception {

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		SpaceClient.setSchema(SchemaResourceTestOften.buildCarSchema(), test);

		JsonNode car = Json.objectBuilder() //
				.put("serialNumber", "1234567890") //
				.put("buyDate", "2015-01-09") //
				.put("buyTime", "15:37:00") //
				.put("buyTimestamp", "2015-01-09T15:37:00.123Z") //
				.put("color", "red") //
				.put("techChecked", false) //
				.object("model") //
				.put("description", "Cette voiture sent bon la France. Elle est inventive et raffinée.") //
				.put("fiscalPower", 8) //
				.put("size", 4.67) //
				.end().object("location") //
				.put("lat", -55.6765) //
				.put("lon", -54.6765) //
				.build();

		// create

		String id = SpaceRequest.post("/1/data/car").backend(test).body(car).go(201)//
				.assertTrue("success").assertEquals("car", "type").assertNotNull("id")//
				.getFromJson("id").asText();

		// find by id

		SpaceResponse res1 = SpaceRequest.get("/1/data/car/" + id).backend(test).go(200)//
				.assertEquals(Backends.DEFAULT_USERNAME, "meta.createdBy")//
				.assertEquals(Backends.DEFAULT_USERNAME, "meta.updatedBy")//
				.assertDateIsRecent("meta.createdAt")//
				.assertEqualsWithoutMeta(car);

		DateTime createdAt = DateTime.parse(res1.getFromJson("meta.createdAt").asText());
		res1.assertEquals(createdAt, "meta.updatedAt");

		// find by full text search

		SpaceRequest.get("/1/search/car?q={q}").refresh().backend(test).routeParam("q", "inVENT*").go(200)
				.assertEquals(id, "results.0.meta.id");

		// create user vince

		SpaceClient.User vince = SpaceClient.newCredentials(test, "vince", "hi vince");

		// update

		SpaceRequest.put("/1/data/car/" + id).userAuth(vince).body("color", "blue").go(200);

		SpaceResponse res3 = SpaceRequest.get("/1/data/car/" + id).backend(test).go(200)//
				.assertEquals(Backends.DEFAULT_USERNAME, "meta.createdBy")//
				.assertEquals("vince", "meta.updatedBy")//
				.assertEquals(createdAt, "meta.createdAt")//
				.assertDateIsRecent("meta.updatedAt")//
				.assertEquals("1234567890", "serialNumber")//
				.assertEquals("blue", "color");

		DateTime updatedAt = DateTime.parse(res3.findValue("updatedAt").asText());
		assertTrue(updatedAt.isAfter(createdAt));

		// delete

		SpaceRequest.delete("/1/data/car/" + id).backend(test).go(200);
		SpaceRequest.get("/1/data/car/" + id).backend(test).go(404);
	}
}
