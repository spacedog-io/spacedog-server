/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.client.SpaceTest;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class DataResourceTestOften extends SpaceTest {

	@Test
	public void createFindUpdateAndDelete() {

		prepareTest();
		Backend test = resetTestBackend();
		setSchema(SchemaResourceTestOften.buildCarSchema(), test);
		User vince = signUp(test, "vince", "hi vince");

		ObjectNode car = Json.objectBuilder() //
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

		String id = SpaceRequest.post("/1/data/car").userAuth(vince).body(car).go(201)//
				.assertTrue("success").assertEquals("car", "type").assertNotNull("id")//
				.getString("id");

		// find by id

		SpaceResponse res1 = SpaceRequest.get("/1/data/car/" + id).userAuth(vince).go(200)//
				.assertEquals("vince", "meta.createdBy")//
				.assertEquals("vince", "meta.updatedBy")//
				.assertDateIsRecent("meta.createdAt")//
				.assertEqualsWithoutMeta(car);

		DateTime createdAt = DateTime.parse(res1.getString("meta.createdAt"));
		res1.assertEquals(createdAt, "meta.updatedAt");

		// find by full text search

		SpaceRequest.get("/1/search/car").refresh()//
				.userAuth(vince).queryParam("q", "inVENT*").go(200)//
				.assertEquals(id, "results.0.meta.id");

		// update

		SpaceRequest.put("/1/data/car/" + id).userAuth(vince).body("color", "blue").go(200);

		SpaceResponse res3 = SpaceRequest.get("/1/data/car/" + id).backend(test).go(200)//
				.assertEquals("vince", "meta.createdBy")//
				.assertEquals("vince", "meta.updatedBy")//
				.assertEquals(createdAt, "meta.createdAt")//
				.assertDateIsRecent("meta.updatedAt")//
				.assertEquals("1234567890", "serialNumber")//
				.assertEquals("blue", "color");

		DateTime updatedAt = DateTime.parse(res3.findValue("updatedAt").asText());
		assertTrue(updatedAt.isAfter(createdAt));

		// delete

		SpaceRequest.delete("/1/data/car/" + id).userAuth(vince).go(200);
		SpaceRequest.get("/1/data/car/" + id).userAuth(vince).go(404);
	}
}
