/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder2;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class DataResource2TestOften extends Assert {

	@Test
	public void createSearchUpdateAndDeleteSales() throws Exception {

		SpaceClient.prepareTest();
		Backend testBackend = SpaceClient.resetTestBackend();
		SpaceClient.setSchema(SchemaResourceTestOften.buildSaleSchema(), testBackend);

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

		SpaceResponse create = SpaceRequest.post("/1/data/sale").backend(testBackend).body(sale).go(201)
				.assertTrue("success").assertEquals("sale", "type").assertEquals(1, "version").assertNotNull("id");

		String id = create.jsonNode().get("id").asText();

		// find by id

		SpaceResponse res1 = SpaceRequest.get("/1/data/sale/" + id).backend(testBackend).go(200)//
				.assertEquals(Backends.DEFAULT_USERNAME, "meta.createdBy")//
				.assertEquals(Backends.DEFAULT_USERNAME, "meta.updatedBy")//
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

		DateTime createdAt = DateTime.parse(res1.getFromJson("meta.createdAt").asText());
		res1.assertDateIsRecent("meta.createdAt")//
				.assertEquals(createdAt, "meta.updatedAt");

		// TODO do we need to assert field by field and then full object with
		// full object?
		// copy to be able to reuse the res1 object in the following
		res1.assertEqualsWithoutMeta(sale);

		// find by simple text search

		SpaceResponse res1b = SpaceRequest.get("/1/search/sale?q=museum&refresh=true").backend(testBackend).go(200)
				.assertEquals(1, "total");

		res1.assertEquals(res1b.getFromJson("results.0"));

		// find by advanced text search

		String query = Json.objectBuilder()//
				.object("query")//
				.object("query_string")//
				.put("query", "museum")//
				.build().toString();

		SpaceResponse res1c = SpaceRequest.post("/1/data/sale/search").backend(testBackend).body(query).go(200)
				.assertEquals(1, "total");

		res1.assertEquals(res1c.getFromJson("results.0"));

		// create user vince

		SpaceClient.initUserDefaultSchema(testBackend);
		User vince = SpaceClient.createUser(testBackend, "vince", "hi vince", "vince@dog.com");

		// small update no version should succeed

		JsonNode updateJson2 = Json.objectBuilder().array("items").object().put("quantity", 7).build();

		SpaceRequest.put("/1/data/sale/" + id).userAuth(vince).body(updateJson2).go(200)//
				.assertTrue("success")//
				.assertEquals("sale", "type")//
				.assertEquals(2, "version")//
				.assertEquals(id, "id");

		// check update is correct

		SpaceResponse res3 = SpaceRequest.get("/1/data/sale/" + id).backend(testBackend).go(200)//
				.assertEquals(Backends.DEFAULT_USERNAME, "meta.createdBy")//
				.assertEquals("vince", "meta.updatedBy")//
				.assertEquals(createdAt, "meta.createdAt")//
				.assertDateIsRecent("meta.updatedAt")//
				.assertEquals(2, "meta.version")//
				.assertEquals("sale", "meta.type")//
				.assertEquals(id, "meta.id")//
				.assertEquals(7, "items.0.quantity");

		// check equality on what has not been updated
		assertEquals(sale.deepCopy().without("items"),
				res3.objectNode().deepCopy().without(Lists.newArrayList("meta", "items")));

		// update with invalid version should fail

		ObjectNode updateJson3b = Json.object("number", "0987654321");

		SpaceRequest.put("/1/data/sale/" + id + "?version=1").backend(testBackend)//
				.body(updateJson3b).go(409).assertFalse("success");

		// update with invalid version should fail

		SpaceRequest.put("/1/data/sale/" + id + "?version=XXX").backend(testBackend)//
				.body(updateJson3b).go(400).assertFalse("success");

		// update with correct version should succeed

		SpaceResponse req3d = SpaceRequest.put("/1/data/sale/" + id + "?version=2").backend(testBackend)
				.body(updateJson3b).go(200);

		req3d.assertTrue("success").assertEquals("sale", "type").assertEquals(3, "version").assertEquals(id, "id");

		// check update is correct

		SpaceResponse res3e = SpaceRequest.get("/1/data/sale/" + id).backend(testBackend).go(200);

		res3e.assertEquals(Backends.DEFAULT_USERNAME, "meta.createdBy")//
				.assertEquals(Backends.DEFAULT_USERNAME, "meta.updatedBy")//
				.assertEquals(3, "meta.version")//
				.assertEquals("sale", "meta.type")//
				.assertEquals(id, "meta.id")//
				.assertEquals("0987654321", "number")//
				.assertEquals(createdAt, "meta.createdAt")//
				.assertDateIsRecent("meta.updatedAt");

		// check equality on what has not been updated
		assertEquals(res3.objectNode().deepCopy().without(Lists.newArrayList("meta", "number")),
				res3e.objectNode().deepCopy().without(Lists.newArrayList("meta", "number")));

		// fails to delete this sale since vince not the owner

		SpaceRequest.delete("/1/data/sale/" + id).userAuth(vince).go(403);

		// deletes this sale since 'default' is the owner

		SpaceRequest.delete("/1/data/sale/" + id).backend(testBackend).go(200);
		SpaceRequest.get("/1/data/sale/" + id).backend(testBackend).go(404);
	}

	@Test
	public void deleteObjects() throws Exception {

		// prepare

		SpaceClient.prepareTest();
		Backend testBackend = SpaceClient.resetTestBackend();
		SpaceClient.initUserDefaultSchema(testBackend);
		ObjectNode schema = SchemaBuilder2.builder("message")//
				.textProperty("text", "english", true).build();
		SpaceClient.setSchema(schema, testBackend);

		// should successfully create 4 messages and 1 user

		SpaceClient.createUser(testBackend, "riri", "hi riri", "riri@dog.com");

		SpaceRequest.post("/1/data/message").adminAuth(testBackend)//
				.body(Json.object("text", "what's up?")).go(201);
		SpaceRequest.post("/1/data/message").adminAuth(testBackend)//
				.body(Json.object("text", "wanna drink something?")).go(201);
		SpaceRequest.post("/1/data/message").adminAuth(testBackend)//
				.body(Json.object("text", "pretty cool, hein?")).go(201);
		SpaceRequest.post("/1/data/message").adminAuth(testBackend)//
				.body(Json.object("text", "so long guys")).go(201);

		SpaceRequest.get("/1/data?refresh=true").adminAuth(testBackend).go(200)//
				.assertEquals(5, "total");

		// should succeed to delete all users
		SpaceRequest.delete("/1/user").adminAuth(testBackend).go(200)//
				.assertEquals(1, "totalDeleted");
		SpaceRequest.get("/1/data?refresh=true").adminAuth(testBackend).go(200)//
				.assertEquals(4, "total");

		// should succeed to delete all messages
		SpaceRequest.delete("/1/data/message").adminAuth(testBackend).go(200)//
				.assertEquals(4, "totalDeleted");
		SpaceRequest.get("/1/data?refresh=true").adminAuth(testBackend).go(200)//
				.assertEquals(0, "total");
	}

	@Test
	public void testAllObjectIdStrategies() throws Exception {

		SpaceClient.prepareTest();
		Backend testBackend = SpaceClient.resetTestBackend();

		// creates msg1 schema with auto generated id strategy

		SpaceClient.setSchema(SchemaBuilder2.builder("msg1")//
				.stringProperty("text", true).build(), testBackend);

		// creates a msg1 object with auto generated id

		String id = SpaceRequest.post("/1/data/msg1").adminAuth(testBackend)//
				.body(Json.object("text", "id=?")).go(201)//
				.getFromJson("id").asText();

		SpaceRequest.get("/1/data/msg1/" + id).adminAuth(testBackend).go(200)//
				.assertEquals("id=?", "text");

		// creates a msg1 object with self provided id

		SpaceRequest.post("/1/data/msg1?id=1").adminAuth(testBackend)//
				.body(Json.object("text", "id=1")).go(201);

		SpaceRequest.get("/1/data/msg1/1").adminAuth(testBackend).go(200)//
				.assertEquals("id=1", "text");

		// fails to create a msg1 object with id subkey
		// since not compliant with schema

		SpaceRequest.post("/1/data/msg1").adminAuth(testBackend)//
				.body(Json.object("text", "id=2", "id", 2)).go(400);

		// creates msg2 schema with code property as id

		SpaceClient.setSchema(SchemaBuilder2.builder("msg2", "code")//
				.stringProperty("code", true)//
				.stringProperty("text", true).build(), testBackend);

		// creates a msg2 object with code = 2

		SpaceRequest.post("/1/data/msg2").adminAuth(testBackend)//
				.body(Json.object("text", "id=code=2", "code", "2")).go(201);

		SpaceRequest.get("/1/data/msg2/2").adminAuth(testBackend).go(200)//
				.assertEquals("id=code=2", "text")//
				.assertEquals("2", "code");

		// creates a msg2 object with code = 3
		// and the id param is transparent

		SpaceRequest.post("/1/data/msg2?id=XXX").adminAuth(testBackend)//
				.body(Json.object("text", "id=code=3", "code", "3")).go(201);

		SpaceRequest.get("/1/data/msg2/3").adminAuth(testBackend).go(200)//
				.assertEquals("id=code=3", "text")//
				.assertEquals("3", "code");

		// fails to create a msg2 object without any code

		SpaceRequest.post("/1/data/msg2").adminAuth(testBackend)//
				.body(Json.object("text", "no code")).go(400);

		// fails to create a msg2 object without any code
		// and the id param is still transparent

		SpaceRequest.post("/1/data/msg2?id=XXX").adminAuth(testBackend)//
				.body(Json.object("text", "no code")).go(400);
	}
}
