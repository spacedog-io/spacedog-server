/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;

public class DataResourceTest2 extends Assert {

	@Test
	public void shouldCreateSearchUpdateAndDeleteSales() throws Exception {

		Account testAccount = SpaceDogHelper.resetTestAccount();
		SpaceDogHelper.resetSchema(SchemaResourceTest.buildSaleSchema(), testAccount);

		ObjectNode sale = Json.objectBuilder()//
				.put("number", "1234567890")//
				.object("where")//
				.put("lat", -55.6765).put("lon", -54.6765)//
				.end()//
				.put("when", "2015-01-09T15:37:00.123Z")//
				.put("online", false)//
				.put("deliveryDate", "2015-09-09")//
				.put("deliveryTime", "15:30:00")//
				.array("items")//
				.object()//
				.put("ref", "JDM")//
				.put("description", "2 rooms appartment in the heart of montmartre")//
				.put("quantity", 8)//
				// .put("price", "EUR230")
				.put("type", "appartment")//
				.end()//
				.object()//
				.put("ref", "LOUVRE")//
				.put("description", "Louvre museum 2 days visit with a personal guide") //
				.put("quantity", 2) //
				// .put("price", "EUR54.25") //
				.put("type", "visit")//
				.end() //
				.build();

		// create

		SpaceResponse create = SpaceRequest.post("/v1/data/sale").backendKey(testAccount).body(sale.toString()).go(201)
				.assertTrue("success").assertEquals("sale", "type").assertEquals(1, "version").assertNotNull("id");

		String id = create.jsonNode().get("id").asText();

		SpaceDogHelper.refresh("test");

		// find by id

		SpaceResponse res1 = SpaceRequest.get("/v1/data/sale/{id}").backendKey(testAccount).routeParam("id", id)
				.go(200);

		DateTime createdAt = DateTime.parse(res1.getFromJson("meta.createdAt").asText());
		assertTrue(createdAt.isAfter(create.before().getMillis()));
		assertTrue(createdAt.isBeforeNow());

		res1.assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, "meta.createdBy")//
				.assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, "meta.updatedBy")//
				.assertEquals(createdAt, "meta.updatedAt")//
				.assertEquals(1, "meta.version")//
				.assertEquals("sale", "meta.type")//
				.assertEquals(id, "meta.id")//
				.assertEquals("1234567890", "number")//
				.assertEquals(-55.6765, "where.lat", 0.00002)//
				.assertEquals(-54.6765, "where.lon", 0.00002)//
				.assertEquals("2015-01-09T15:37:00.123Z", "when")//
				.assertFalse("online")//
				.assertEquals("2015-09-09", "deliveryDate")//
				.assertEquals("15:30:00", "deliveryTime")//
				.assertEquals(2, "items")//
				.assertEquals("JDM", "items.0.ref")//
				.assertEquals("2 rooms appartment in the heart of montmartre", "items.0.description")//
				.assertEquals(8, "items.0.quantity")//
				// .assertEquals("EUR230", "items.0.price")
				.assertEquals("appartment", "items.0.type")//
				.assertEquals("LOUVRE", "items.1.ref")//
				.assertEquals("Louvre museum 2 days visit with a personal guide", "items.1.description")//
				.assertEquals(2, "items.1.quantity")//
				// assertEquals("EUR54.25", "items.1.price")
				.assertEquals("visit", "items.1.type");

		// TODO do we need to assert field by field and then full object with
		// full object?
		// copy to be able to reuse the res1 object in the following
		res1.assertEqualsWithoutMeta(sale);

		// find by simple text search

		SpaceResponse res1b = SpaceRequest.get("/v1/data/sale?q=museum").backendKey(testAccount).go(200).assertEquals(1,
				"total");

		res1.assertEquals(res1b.getFromJson("results.0"));

		// find by advanced text search

		String query = Json.objectBuilder()//
				.object("query")//
				.object("query_string")//
				.put("query", "museum")//
				.build().toString();

		SpaceResponse res1c = SpaceRequest.post("/v1/data/sale/search").backendKey(testAccount).body(query).go(200)
				.assertEquals(1, "total");

		res1.assertEquals(res1c.getFromJson("results.0"));

		// create user vince

		SpaceDogHelper.User vince = SpaceDogHelper.createUser(testAccount.backendKey, "vince", "hi vince",
				"vince@dog.com");

		SpaceDogHelper.refresh(testAccount.backendId);

		// small update no version should succeed

		JsonNode updateJson2 = Json.objectBuilder().array("items").object().put("quantity", 7).build();

		SpaceResponse req2 = SpaceRequest.put("/v1/data/sale/{id}").backendKey(testAccount).routeParam("id", id)
				.basicAuth(vince).body(updateJson2.toString()).go(200).assertTrue("success")
				.assertEquals("sale", "type").assertEquals(2, "version").assertEquals(id, "id");

		// check update is correct

		SpaceResponse res3 = SpaceRequest.get("/v1/data/sale/{id}").backendKey(testAccount).routeParam("id", id)
				.go(200);

		res3.assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, "meta.createdBy").assertEquals("vince", "meta.updatedBy")
				.assertEquals(createdAt, "meta.createdAt").assertEquals(2, "meta.version")
				.assertEquals("sale", "meta.type").assertEquals(id, "meta.id").assertEquals(7, "items.0.quantity");

		DateTime updatedAt = DateTime.parse(res3.getFromJson("meta.updatedAt").asText());
		assertTrue(updatedAt.isAfter(req2.before().getMillis()));
		assertTrue(updatedAt.isBeforeNow());

		// check equality on what has not been updated
		assertEquals(sale.deepCopy().without("items"),
				res3.objectNode().deepCopy().without(Lists.newArrayList("meta", "items")));

		// update with invalid version should fail

		ObjectNode updateJson3b = Json.objectBuilder().put("number", "0987654321").build();

		SpaceRequest.put("/v1/data/sale/{id}").backendKey(testAccount).routeParam("id", id).queryString("version", "1")
				.body(updateJson3b.toString()).go(409).assertFalse("success");

		// update with invalid version should fail

		SpaceRequest.put("/v1/data/sale/{id}").backendKey(testAccount).routeParam("id", id)
				.queryString("version", "XXX").body(updateJson3b.toString()).go(400).assertFalse("success");

		// update with correct version should succeed

		SpaceResponse req3d = SpaceRequest.put("/v1/data/sale/{id}").backendKey(testAccount).routeParam("id", id)
				.queryString("version", "2").body(updateJson3b.toString()).go(200);

		req3d.assertTrue("success").assertEquals("sale", "type").assertEquals(3, "version").assertEquals(id, "id");

		// check update is correct

		SpaceResponse res3e = SpaceRequest.get("/v1/data/sale/{id}").backendKey(testAccount).routeParam("id", id)
				.go(200);

		res3e.assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, "meta.createdBy")//
				.assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, "meta.updatedBy")//
				.assertEquals(3, "meta.version").assertEquals("sale", "meta.type").assertEquals(id, "meta.id")
				.assertEquals("0987654321", "number");

		DateTime createdAt3e = DateTime.parse(res3e.getFromJson("meta.createdAt").asText());
		assertEquals(createdAt, createdAt3e);
		DateTime updatedAt3e = DateTime.parse(res3e.getFromJson("meta.updatedAt").asText());
		assertTrue(updatedAt3e.isAfter(req3d.before().getMillis()));
		assertTrue(updatedAt3e.isBeforeNow());

		// check equality on what has not been updated
		assertEquals(res3.objectNode().deepCopy().without(Lists.newArrayList("meta", "number")),
				res3e.objectNode().deepCopy().without(Lists.newArrayList("meta", "number")));

		// delete

		SpaceRequest.delete("/v1/data/sale/{id}").backendKey(testAccount).routeParam("id", id).go(200);

		// check delete is done

		assertFalse(SpaceRequest.get("/v1/data/sale/{id}").backendKey(testAccount).routeParam("id", id).go(404)
				.jsonNode().get("success").asBoolean());
	}

	@Test
	public void shouldSucceedToDeleteObjects() throws Exception {

		Account testAccount = SpaceDogHelper.resetTestAccount();
		SpaceDogHelper.setSchema(SchemaBuilder2.builder("message").textProperty("text", "english", true).build(),
				testAccount);

		// should successfully create 4 messages and 1 user

		SpaceDogHelper.createUser(testAccount, "riri", "hi riri", "riri@dog.com");

		SpaceRequest.post("/v1/data/message").basicAuth(testAccount)
				.body(Json.objectBuilder().put("text", "what's up?").build()).go(201);
		SpaceRequest.post("/v1/data/message").basicAuth(testAccount)
				.body(Json.objectBuilder().put("text", "wanna drink something?").build()).go(201);
		SpaceRequest.post("/v1/data/message").basicAuth(testAccount)
				.body(Json.objectBuilder().put("text", "pretty cool, hein?").build()).go(201);
		String id = SpaceRequest.post("/v1/data/message").basicAuth(testAccount)
				.body(Json.objectBuilder().put("text", "so long guys").build()).go(201).getFromJson("id").asText();

		SpaceDogHelper.refresh(testAccount);

		SpaceRequest.get("/v1/data").basicAuth(testAccount).go(200).assertEquals(5, "total");

		// should succeed to delete all users
		SpaceRequest.delete("/v1/data/user").basicAuth(testAccount).go(200);
		SpaceDogHelper.refresh(testAccount);
		SpaceRequest.get("/v1/data").basicAuth(testAccount).go(200).assertEquals(4, "total");

		// should succeed to delete all objects
		SpaceRequest.delete("/v1/data").basicAuth(testAccount).go(200);
		SpaceDogHelper.refresh(testAccount);
		SpaceRequest.get("/v1/data").basicAuth(testAccount).go(200).assertEquals(0, "total");
	}
}
