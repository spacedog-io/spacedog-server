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
import io.spacedog.utils.BackendKey;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class DataResourceTestOften extends Assert {

	@Test
	public void createFindUpdateAndDelete() throws Exception {

		SpaceClient.prepareTest();
		Backend testBackend = SpaceClient.resetTestBackend();
		SpaceClient.initUserDefaultSchema(testBackend);
		SpaceClient.setSchema(SchemaResourceTestOften.buildCarSchema(), testBackend);

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

		SpaceResponse create = SpaceRequest.post("/1/data/car").backend(testBackend).body(car.toString()).go(201);

		create.assertTrue("success").assertEquals("car", "type").assertNotNull("id");

		String id = create.getFromJson("id").asText();

		// find by id

		SpaceResponse res1 = SpaceRequest.get("/1/data/car/" + id).backend(testBackend).go(200)//
				.assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, "meta.createdBy")//
				.assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, "meta.updatedBy")//
				.assertDateIsRecent("meta.createdAt")//
				.assertEqualsWithoutMeta(car);

		DateTime createdAt = DateTime.parse(res1.getFromJson("meta.createdAt").asText());
		res1.assertEquals(createdAt, "meta.updatedAt");

		// find by full text search

		SpaceRequest.get("/1/search/car?q={q}&refresh=true").backend(testBackend).routeParam("q", "inVENT*").go(200)
				.assertEquals(id, "results.0.meta.id");

		// create user vince

		SpaceClient.User vince = SpaceClient.createUser(testBackend, "vince", "hi vince", "vince@spacedog.io");

		// update

		SpaceRequest.put("/1/data/car/" + id).backend(testBackend).userAuth(vince)
				.body(Json.objectBuilder().put("color", "blue").toString()).go(200);

		// check update is correct

		SpaceResponse res3 = SpaceRequest.get("/1/data/car/" + id).backend(testBackend).go(200)//
				.assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, "meta.createdBy")//
				.assertEquals("vince", "meta.updatedBy")//
				.assertEquals(createdAt, "meta.createdAt")//
				.assertDateIsRecent("meta.updatedAt")//
				.assertEquals("1234567890", "serialNumber")//
				.assertEquals("blue", "color");

		DateTime updatedAt = DateTime.parse(res3.findValue("updatedAt").asText());
		assertTrue(updatedAt.isAfter(createdAt));

		// delete

		SpaceRequest.delete("/1/data/car/" + id).backend(testBackend).go(200);

		// check delete is done

		assertFalse(SpaceRequest.get("/1/data/car/" + id).backend(testBackend).go(404).jsonNode().get("success")
				.asBoolean());
	}
}
