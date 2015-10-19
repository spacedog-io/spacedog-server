/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class DataResourceTest2 extends AbstractTest {

	@BeforeClass
	public static void resetTestAccount() throws UnirestException, InterruptedException, IOException {
		AdminResourceTest.resetTestAccount();
		SchemaResourceTest.resetSaleSchema();
	}

	@Test
	public void shouldCreateSearchUpdateAndDeleteSales() throws Exception {

		ObjectNode sale = Json.startObject().put("number", "1234567890").startObject("where").put("lat", -55.6765)
				.put("lon", -54.6765).end().put("when", "2015-01-09T15:37:00.123Z").put("online", false)
				.put("deliveryDate", "2015-09-09").put("deliveryTime", "15:30:00").startArray("items").startObject()
				.put("ref", "JDM").put("description", "2 rooms appartment in the heart of montmartre")
				.put("quantity", 8)
				// .put("price", "EUR230")
				.put("type", "appartment").end().startObject().put("ref", "LOUVRE")
				.put("description", "Louvre museum 2 days visit with a personal guide") //
				.put("quantity", 2) //
				// .put("price", "EUR54.25") //
				.put("type", "visit").end() //
				.build();

		// create

		RequestBodyEntity req = preparePost("/v1/data/sale", AdminResourceTest.testClientKey()).body(sale.toString());

		DateTime beforeCreate = DateTime.now();
		JsonNode result = post(req, 201).jsonNode();

		assertEquals(true, result.get("success").asBoolean());
		assertEquals("sale", result.get("type").asText());
		assertEquals(1, result.get("version").asLong());
		assertNotNull(result.get("id"));

		String id = result.get("id").asText();

		refreshIndex("test");

		// find by id

		GetRequest req1 = prepareGet("/v1/data/sale/{id}", AdminResourceTest.testClientKey()).routeParam("id", id);
		ObjectNode res1 = get(req1, 200).objectNode();

		JsonNode meta1 = res1.get("meta");
		assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, meta1.get("createdBy").asText());
		assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, meta1.get("updatedBy").asText());
		DateTime createdAt = DateTime.parse(meta1.get("createdAt").asText());
		assertTrue(createdAt.isAfter(beforeCreate.getMillis()));
		assertTrue(createdAt.isBeforeNow());
		assertEquals(meta1.get("updatedAt"), meta1.get("createdAt"));
		assertEquals(1, meta1.get("version").asLong());
		assertEquals("sale", meta1.get("type").asText());
		assertEquals(id, meta1.get("id").asText());

		// copy to be able to reuse the res1 object in the following
		ObjectNode res1Copy = res1.deepCopy();
		res1Copy.remove("meta");
		assertEquals(sale, res1Copy);

		assertEquals("1234567890", res1.get("number").asText());
		assertEquals(-55.6765, Json.get(res1, "where.lat").doubleValue(), 0.00002);
		assertEquals(-54.6765, Json.get(res1, "where.lon").doubleValue(), 0.00002);
		assertEquals("2015-01-09T15:37:00.123Z", res1.get("when").asText());
		assertEquals(false, res1.get("online").asBoolean());
		assertEquals("2015-09-09", res1.get("deliveryDate").asText());
		assertEquals("15:30:00", res1.get("deliveryTime").asText());
		assertEquals(2, res1.get("items").size());
		assertEquals("JDM", Json.get(res1, "items.0.ref").asText());
		assertEquals("2 rooms appartment in the heart of montmartre", Json.get(res1, "items.0.description").asText());
		assertEquals(8, Json.get(res1, "items.0.quantity").asInt());
		// assertEquals("EUR230", Json.get(res1, "items.0.price").asText());
		assertEquals("appartment", Json.get(res1, "items.0.type").asText());
		assertEquals("LOUVRE", Json.get(res1, "items.1.ref").asText());
		assertEquals("Louvre museum 2 days visit with a personal guide",
				Json.get(res1, "items.1.description").asText());
		assertEquals(2, Json.get(res1, "items.1.quantity").asInt());
		// assertEquals("EUR54.25", Json.get(res1, "items.1.price").asText());
		assertEquals("visit", Json.get(res1, "items.1.type").asText());

		// find by full text search

		GetRequest req1b = prepareGet("/v1/data/sale?q=museum", AdminResourceTest.testClientKey());
		JsonNode res1b = get(req1b, 200).jsonNode();
		assertEquals(1, Json.get(res1b, "total").asLong());
		assertEquals(res1, res1b.get("results").get(0));

		// create user vince

		RequestBodyEntity req1a = preparePost("/v1/user/", AdminResourceTest.testClientKey()).body(Json.startObject()
				.put("username", "vince").put("password", "hi vince").put("email", "vince@dog.com").toString());

		post(req1a, 201);
		refreshIndex("test");

		// small update no version should succeed

		JsonNode updateJson2 = Json.startObject().startArray("items").startObject().put("quantity", 7).build();

		RequestBodyEntity req2 = preparePut("/v1/data/sale/{id}", AdminResourceTest.testClientKey())
				.routeParam("id", id).basicAuth("vince", "hi vince").body(updateJson2.toString());

		DateTime beforeUpdate = DateTime.now();
		JsonNode res2 = put(req2, 200).jsonNode();

		assertEquals(true, res2.get("success").asBoolean());
		assertEquals("sale", res2.get("type").asText());
		assertEquals(2, res2.get("version").asLong());
		assertEquals(id, res2.get("id").asText());

		// check update is correct

		GetRequest req3 = prepareGet("/v1/data/sale/{id}", AdminResourceTest.testClientKey()).routeParam("id", id);
		ObjectNode res3 = get(req3, 200).objectNode();

		JsonNode meta3 = res3.get("meta");
		assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, meta3.get("createdBy").asText());
		assertEquals("vince", meta3.get("updatedBy").asText());
		DateTime createdAt3 = DateTime.parse(meta3.get("createdAt").asText());
		assertEquals(createdAt, createdAt3);
		DateTime updatedAt = DateTime.parse(meta3.get("updatedAt").asText());
		assertTrue(updatedAt.isAfter(beforeUpdate.getMillis()));
		assertTrue(updatedAt.isBeforeNow());
		assertEquals(2, meta3.get("version").asLong());
		assertEquals("sale", meta3.get("type").asText());
		assertEquals(id, meta3.get("id").asText());
		assertEquals(7, Json.get(res3, "items.0.quantity").asInt());

		// check equality on what has not been updated
		assertEquals(sale.deepCopy().without("items"), res3.deepCopy().without(Lists.newArrayList("meta", "items")));

		// update with invalid version should fail

		ObjectNode updateJson3b = Json.startObject().put("number", "0987654321").build();

		RequestBodyEntity req3b = preparePut("/v1/data/sale/{id}", AdminResourceTest.testClientKey())
				.routeParam("id", id).queryString("version", "1").body(updateJson3b.toString());

		JsonNode res3b = put(req3b, 409).jsonNode();

		assertEquals(false, res3b.get("success").asBoolean());

		// update with invalid version should fail

		RequestBodyEntity req3c = preparePut("/v1/data/sale/{id}", AdminResourceTest.testClientKey())
				.routeParam("id", id).queryString("version", "XXX").body(updateJson3b.toString());

		JsonNode res3c = put(req3c, 400).jsonNode();

		assertEquals(false, res3c.get("success").asBoolean());
		assertEquals("XXX", Json.get(res3c, "invalidParameters.version.value").asText());

		// update with correct version should succeed

		RequestBodyEntity req3d = preparePut("/v1/data/sale/{id}", AdminResourceTest.testClientKey())
				.routeParam("id", id).queryString("version", "2").body(updateJson3b.toString());

		JsonNode res3d = put(req3d, 200).jsonNode();

		assertEquals(true, res3d.get("success").asBoolean());
		assertEquals("sale", res3d.get("type").asText());
		assertEquals(3, res3d.get("version").asLong());
		assertEquals(id, res3d.get("id").asText());

		// check update is correct

		GetRequest req3e = prepareGet("/v1/data/sale/{id}", AdminResourceTest.testClientKey()).routeParam("id", id);
		ObjectNode res3e = get(req3e, 200).objectNode();

		JsonNode meta3e = res3e.get("meta");
		assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, meta3e.get("createdBy").asText());
		assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, meta3e.get("updatedBy").asText());
		DateTime createdAt3e = DateTime.parse(meta3.get("createdAt").asText());
		assertEquals(createdAt, createdAt3e);
		DateTime updatedAt3e = DateTime.parse(meta3e.get("updatedAt").asText());
		assertTrue(updatedAt3e.isAfter(beforeUpdate.getMillis()));
		assertTrue(updatedAt3e.isBeforeNow());
		assertEquals(3, meta3e.get("version").asLong());
		assertEquals("sale", meta3e.get("type").asText());
		assertEquals(id, meta3e.get("id").asText());
		assertEquals("0987654321", Json.get(res3e, "number").asText());
		// check equality on what has not been updated
		assertEquals(res3.deepCopy().without(Lists.newArrayList("meta", "number")),
				res3e.deepCopy().without(Lists.newArrayList("meta", "number")));

		// delete

		HttpRequestWithBody req4 = prepareDelete("/v1/data/sale/{id}", AdminResourceTest.testClientKey())
				.routeParam("id", id);
		delete(req4, 200);

		// check delete is done

		JsonNode res5 = get(req1, 404).jsonNode();
		assertFalse(res5.get("success").asBoolean());
	}
}
