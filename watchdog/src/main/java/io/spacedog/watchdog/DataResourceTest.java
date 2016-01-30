/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.utils.BackendKey;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class DataResourceTest extends Assert {

	@Test
	public void createFindUpdateAndDelete() throws Exception {

		SpaceDogHelper.prepareTest();
		Account testAccount = SpaceDogHelper.resetTestAccount();
		SpaceDogHelper.resetSchema(SchemaResourceTest.buildCarSchema(), testAccount);

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

		SpaceResponse create = SpaceRequest.post("/v1/data/car").backendKey(testAccount).body(car.toString()).go(201);

		create.assertTrue("success").assertEquals("car", "type").assertNotNull("id");

		String id = create.getFromJson("id").asText();

		// find by id

		SpaceResponse res1 = SpaceRequest.get("/v1/data/car/" + id).backendKey(testAccount).go(200)//
				.assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, "meta.createdBy")//
				.assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, "meta.updatedBy")//
				.assertDateIsRecent("meta.createdAt")//
				.assertEqualsWithoutMeta(car);

		DateTime createdAt = DateTime.parse(res1.getFromJson("meta.createdAt").asText());
		res1.assertEquals(createdAt, "meta.updatedAt");

		// find by full text search

		SpaceRequest.get("/v1/search/car?q={q}&refresh=true").backendKey(testAccount).routeParam("q", "inVENT*").go(200)
				.assertEquals(id, "results.0.meta.id");

		// create user vince

		SpaceDogHelper.User vince = SpaceDogHelper.createUser(testAccount.backendKey, "vince", "hi vince",
				"vince@spacedog.io");

		// update

		SpaceRequest.put("/v1/data/car/" + id).backendKey(testAccount).basicAuth(vince)
				.body(Json.objectBuilder().put("color", "blue").toString()).go(200);

		// check update is correct

		SpaceResponse res3 = SpaceRequest.get("/v1/data/car/" + id).backendKey(testAccount).go(200)//
				.assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, "meta.createdBy")//
				.assertEquals("vince", "meta.updatedBy")//
				.assertEquals(createdAt, "meta.createdAt")//
				.assertDateIsRecent("meta.updatedAt")//
				.assertEquals("1234567890", "serialNumber")//
				.assertEquals("blue", "color");

		DateTime updatedAt = DateTime.parse(res3.findValue("updatedAt").asText());
		assertTrue(updatedAt.isAfter(createdAt));

		// delete

		SpaceRequest.delete("/v1/data/car/" + id).backendKey(testAccount).go(200);

		// check delete is done

		assertFalse(SpaceRequest.get("/v1/data/car/" + id).backendKey(testAccount).go(404).jsonNode().get("success")
				.asBoolean());
	}
}
