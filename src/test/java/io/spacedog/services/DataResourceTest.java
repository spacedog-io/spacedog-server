package io.spacedog.services;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class DataResourceTest extends AbstractTest {

	@BeforeClass
	public static void resetTestAccount() throws UnirestException,
			InterruptedException {
		AdminResourceTest.resetTestAccount();
		SchemaResourceTest.resetCarSchema();
	}

	@Test
	public void shouldCreateFindUpdateAndDelete() throws Exception {

		JsonObject car = Json
				.builder()
				//
				.add("serialNumber", "1234567890")
				//
				.add("buyDate", "2015-01-09")
				//
				.add("buyTime", "15:37:00")
				//
				.add("buyTimestamp", "2015-01-09T15:37:00.123Z")
				//
				.add("color", "red")
				//
				.add("techChecked", false)
				//
				.stObj("model")
				//
				.add("description",
						"Cette voiture sent bon la France. Elle est inventive et raffinée.") //
				.add("fiscalPower", 8) //
				.add("size", 4.67) //
				.end() //
				.stObj("location") //
				.add("lat", -55.6765) //
				.add("lon", -54.6765) //
				.build();

		// create

		RequestBodyEntity req = preparePost("/v1/data/car",
				AdminResourceTest.testKey()).body(car.toString());

		DateTime beforeCreate = DateTime.now();
		JsonObject result = post(req, 201).json();

		assertEquals(true, result.get("success").asBoolean());
		assertEquals("car", result.get("type").asString());
		assertNotNull(result.get("id"));

		String id = result.get("id").asString();

		refreshIndex("test");

		// find by id

		GetRequest req1 = prepareGet("/v1/data/car/{id}",
				AdminResourceTest.testKey()).routeParam("id", id);
		JsonObject res1 = get(req1, 200).json();

		JsonObject meta1 = res1.get("meta").asObject();
		assertEquals(AdminResource.DEFAULT_API_KEY_ID, meta1.get("createdBy")
				.asString());
		assertEquals(AdminResource.DEFAULT_API_KEY_ID, meta1.get("updatedBy")
				.asString());
		DateTime createdAt = DateTime.parse(meta1.get("createdAt").asString());
		assertTrue(createdAt.isAfter(beforeCreate.getMillis()));
		assertTrue(createdAt.isBeforeNow());
		assertEquals(meta1.get("updatedAt"), meta1.get("createdAt"));
		assertTrue(Json.equals(car, res1.remove("meta")));

		// find by full text search

		GetRequest req1b = prepareGet("/v1/data/car?q={q}",
				AdminResourceTest.testKey()).routeParam("q", "inVENt*");

		JsonObject res1b = get(req1b, 200).json();
		assertEquals(id, Json.get(res1b, "results.0.meta.id").asString());

		// create user vince

		RequestBodyEntity req1a = preparePost("/v1/user/",
				AdminResourceTest.testKey()).body(
				Json.builder().add("username", "vince")
						.add("password", "hi vince")
						.add("email", "vince@spacedog.io").build().toString());

		post(req1a, 201);
		refreshIndex("test");

		// update

		RequestBodyEntity req2 = preparePut("/v1/data/car/{id}",
				AdminResourceTest.testKey()).routeParam("id", id)
				.basicAuth("vince", "hi vince")
				.body(new JsonObject().add("color", "blue").toString());

		DateTime beforeUpdate = DateTime.now();
		put(req2, 200);

		// check update is correct

		GetRequest req3 = prepareGet("/v1/data/car/{id}",
				AdminResourceTest.testKey()).routeParam("id", id);
		JsonObject res3 = get(req3, 200).json();

		JsonObject meta3 = res3.get("meta").asObject();
		assertEquals(AdminResource.DEFAULT_API_KEY_ID, meta3.get("createdBy")
				.asString());
		assertEquals("vince", meta3.get("updatedBy").asString());
		DateTime createdAtAfterUpdate = DateTime.parse(meta3.get("createdAt")
				.asString());
		assertEquals(createdAt, createdAtAfterUpdate);
		DateTime updatedAt = DateTime.parse(meta3.get("updatedAt").asString());
		assertTrue(updatedAt.isAfter(beforeUpdate.getMillis()));
		assertTrue(updatedAt.isBeforeNow());
		assertEquals("1234567890", res3.get("serialNumber").asString());
		assertEquals("blue", res3.get("color").asString());

		// delete

		HttpRequestWithBody req4 = prepareDelete("/v1/data/car/{id}",
				AdminResourceTest.testKey()).routeParam("id", id);
		delete(req4, 200);

		// check delete is done

		JsonObject res5 = get(req1, 404).json();
		assertFalse(res5.get("success").asBoolean());
	}
}
