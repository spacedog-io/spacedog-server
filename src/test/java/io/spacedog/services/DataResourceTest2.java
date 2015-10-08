package io.spacedog.services;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class DataResourceTest2 extends AbstractTest {

	@BeforeClass
	public static void resetTestAccount() throws UnirestException,
			InterruptedException {
		AdminResourceTest.resetTestAccount();
		SchemaResourceTest.resetSaleSchema();
	}

	@Test
	public void shouldCreateSearchUpdateAndDeleteSales() throws Exception {

		JsonObject sale = Json
				.builder()
				.add("number", "1234567890")
				.stObj("where")
				.add("lat", -55.6765)
				.add("lon", -54.6765)
				.end()
				.add("when", "2015-01-09T15:37:00.123Z")
				.add("online", false)
				.add("deliveryDate", "2015-09-09")
				.add("deliveryTime", "15:30:00")
				.stArr("items")
				.addObj()
				.add("ref", "JDM")
				.add("description",
						"2 rooms appartment in the heart of montmartre")
				.add("quantity", 8)
				// .add("price", "EUR230")
				.add("type", "appartment")
				.end()
				.addObj()
				.add("ref", "LOUVRE")
				.add("description",
						"Louvre museum 2 days visit with a personal guide") //
				.add("quantity", 2) //
				// .add("price", "EUR54.25") //
				.add("type", "visit").end() //
				.build();

		// create

		RequestBodyEntity req = preparePost("/v1/data/sale",
				AdminResourceTest.testClientKey()).body(sale.toString());

		DateTime beforeCreate = DateTime.now();
		JsonObject result = post(req, 201).json();

		assertEquals(true, result.get("success").asBoolean());
		assertEquals("sale", result.get("type").asString());
		assertNotNull(result.get("id"));

		String id = result.get("id").asString();

		refreshIndex("test");

		// find by id

		GetRequest req1 = prepareGet("/v1/data/sale/{id}",
				AdminResourceTest.testClientKey()).routeParam("id", id);
		JsonObject res1 = get(req1, 200).json();

		JsonObject meta1 = res1.get("meta").asObject();
		assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, meta1.get("createdBy")
				.asString());
		assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, meta1.get("updatedBy")
				.asString());
		DateTime createdAt = DateTime.parse(meta1.get("createdAt").asString());
		assertTrue(createdAt.isAfter(beforeCreate.getMillis()));
		assertTrue(createdAt.isBeforeNow());
		assertEquals(meta1.get("updatedAt"), meta1.get("createdAt"));
		assertTrue(Json.equals(sale, res1.remove("meta")));

		assertEquals("1234567890", res1.get("number").asString());
		assertEquals(-55.6765, Json.get(res1, "where.lat").asFloat(), 0.00002);
		assertEquals(-54.6765, Json.get(res1, "where.lon").asFloat(), 0.00002);
		assertEquals("2015-01-09T15:37:00.123Z", res1.get("when").asString());
		assertEquals(false, res1.get("online").asBoolean());
		assertEquals("2015-09-09", res1.get("deliveryDate").asString());
		assertEquals("15:30:00", res1.get("deliveryTime").asString());
		assertEquals(2, res1.get("items").asArray().size());
		assertEquals("JDM", Json.get(res1, "items.0.ref").asString());
		assertEquals("2 rooms appartment in the heart of montmartre",
				Json.get(res1, "items.0.description").asString());
		assertEquals(8, Json.get(res1, "items.0.quantity").asInt());
		// assertEquals("EUR230", Json.get(res1, "items.0.price").asString());
		assertEquals("appartment", Json.get(res1, "items.0.type").asString());
		assertEquals("LOUVRE", Json.get(res1, "items.1.ref").asString());
		assertEquals("Louvre museum 2 days visit with a personal guide", Json
				.get(res1, "items.1.description").asString());
		assertEquals(2, Json.get(res1, "items.1.quantity").asInt());
		// assertEquals("EUR54.25", Json.get(res1, "items.1.price").asString());
		assertEquals("visit", Json.get(res1, "items.1.type").asString());

		// find by full text search

		GetRequest req1b = prepareGet("/v1/data/sale?q=museum",
				AdminResourceTest.testClientKey());
		JsonObject res1b = get(req1b, 200).json();
		assertEquals(id, Json.get(res1b, "results.0.meta.id").asString());

		// create user vince

		RequestBodyEntity req1a = preparePost("/v1/user/",
				AdminResourceTest.testClientKey()).body(
				Json.builder().add("username", "vince")
						.add("password", "hi vince")
						.add("email", "vince@dog.com").build().toString());

		post(req1a, 201);
		refreshIndex("test");

		// update

		JsonObject updateJson = Json.builder().stArr("items").addObj()
				.add("quantity", 7).build();

		RequestBodyEntity req2 = preparePut("/v1/data/sale/{id}",
				AdminResourceTest.testClientKey()).routeParam("id", id)
				.basicAuth("vince", "hi vince").body(updateJson.toString());

		DateTime beforeUpdate = DateTime.now();
		put(req2, 200);

		// check update is correct

		GetRequest req3 = prepareGet("/v1/data/sale/{id}",
				AdminResourceTest.testClientKey()).routeParam("id", id);
		JsonObject res3 = get(req3, 200).json();

		JsonObject meta3 = res3.get("meta").asObject();
		assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, meta3.get("createdBy")
				.asString());
		assertEquals("vince", meta3.get("updatedBy").asString());
		DateTime createdAtAfterUpdate = DateTime.parse(meta3.get("createdAt")
				.asString());
		assertEquals(createdAt, createdAtAfterUpdate);
		DateTime updatedAt = DateTime.parse(meta3.get("updatedAt").asString());
		assertTrue(updatedAt.isAfter(beforeUpdate.getMillis()));
		assertTrue(updatedAt.isBeforeNow());
		assertEquals("1234567890", res3.get("number").asString());
		assertEquals(7, Json.get(res3, "items.0.quantity").asInt());

		// delete

		HttpRequestWithBody req4 = prepareDelete("/v1/data/sale/{id}",
				AdminResourceTest.testClientKey()).routeParam("id", id);
		delete(req4, 200);

		// check delete is done

		JsonObject res5 = get(req1, 404).json();
		assertFalse(res5.get("success").asBoolean());
	}
}
