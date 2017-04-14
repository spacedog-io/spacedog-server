/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceRequestException;
import io.spacedog.rest.SpaceResponse;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json7;

public class DataResourceTestOften extends SpaceTest {

	@Test
	public void createFindUpdateAndDelete() {

		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		superadmin.schema().set(SchemaResourceTestOften.buildCarSchema());
		SpaceDog vince = signUp(superadmin, "vince", "hi vince");

		ObjectNode car = Json7.objectBuilder() //
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
		String id = vince.post("/1/data/car").bodyJson(car).go(201)//
				.assertTrue("success").assertEquals("car", "type").assertNotNull("id")//
				.getString("id");

		// find by id
		SpaceResponse res1 = vince.get("/1/data/car/" + id).go(200)//
				.assertEquals("vince", "meta.createdBy")//
				.assertEquals("vince", "meta.updatedBy")//
				.assertDateIsRecent("meta.createdAt")//
				.assertEqualsWithoutMeta(car);

		DateTime createdAt = DateTime.parse(res1.getString("meta.createdAt"));
		res1.assertEquals(createdAt, "meta.updatedAt");

		// find by full text search

		vince.get("/1/search/car").refresh().queryParam("q", "inVENT*").go(200)//
				.assertEquals(id, "results.0.meta.id");

		// update

		vince.put("/1/data/car/" + id).bodyJson("color", "blue").go(200);

		SpaceResponse res3 = SpaceRequest.get("/1/data/car/" + id)//
				.backend(superadmin).go(200)//
				.assertEquals("vince", "meta.createdBy")//
				.assertEquals("vince", "meta.updatedBy")//
				.assertEquals(createdAt, "meta.createdAt")//
				.assertDateIsRecent("meta.updatedAt")//
				.assertEquals("1234567890", "serialNumber")//
				.assertEquals("blue", "color");

		DateTime updatedAt = DateTime.parse(res3.findValue("updatedAt").asText());
		assertTrue(updatedAt.isAfter(createdAt));

		// delete

		vince.delete("/1/data/car/" + id).go(200);
		vince.get("/1/data/car/" + id).go(404);
	}

	@Test
	public void createFindUpdateAndDeleteWithSdk() {

		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		superadmin.schema().set(SchemaResourceTestOften.buildCarSchema());
		SpaceDog vince = signUp(superadmin, "vince", "hi vince");

		ObjectNode car = Json7.objectBuilder() //
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
		String id = vince.data2().create("car", car);

		// find by id
		ObjectNode carbis = vince.data2().get("car", id);
		assertEquals("vince", Json7.checkString(carbis, "meta.createdBy").get());
		assertEquals("vince", Json7.checkString(carbis, "meta.updatedBy").get());
		assertNotNull(Json7.checkString(carbis, "meta.createdAt"));
		carbis.remove("meta");
		assertEquals(car, carbis);

		// find by full text search

		SpaceRequest.get("/1/search/car").refresh().auth(vince).queryParam("q", "inVENT*").go(200)//
				.assertEquals(id, "results.0.meta.id");

		// update

		vince.data2().save("car", id, Json7.object("color", "blue"), false);
		carbis = vince.data2().get("car", id);
		assertEquals("blue", carbis.get("color").asText());

		// delete

		vince.data2().delete("car", id);
		try {
			vince.data2().delete("car", id);
		} catch (SpaceRequestException e) {
			assertEquals(404, e.httpStatus());
		}
	}
}
