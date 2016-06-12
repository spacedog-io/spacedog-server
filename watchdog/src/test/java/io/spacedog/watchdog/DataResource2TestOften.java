/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
		Backend test = SpaceClient.resetTestBackend();
		SpaceClient.setSchema(SchemaResourceTestOften.buildSaleSchema(), test);

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

		SpaceResponse create = SpaceRequest.post("/1/data/sale").backend(test).body(sale).go(201).assertTrue("success")
				.assertEquals("sale", "type").assertEquals(1, "version").assertNotNull("id");

		String id = create.jsonNode().get("id").asText();

		// find by id

		SpaceResponse res1 = SpaceRequest.get("/1/data/sale/" + id).backend(test).go(200)//
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

		SpaceResponse res1b = SpaceRequest.get("/1/search/sale?q=museum").refresh(true).backend(test).go(200)
				.assertEquals(1, "total");

		res1.assertEquals(res1b.getFromJson("results.0"));

		// find by advanced text search

		String query = Json.objectBuilder()//
				.object("query")//
				.object("query_string")//
				.put("query", "museum")//
				.build().toString();

		SpaceResponse res1c = SpaceRequest.post("/1/data/sale/search").backend(test).body(query).go(200).assertEquals(1,
				"total");

		res1.assertEquals(res1c.getFromJson("results.0"));

		// create user vince

		SpaceClient.initUserDefaultSchema(test);
		User vince = SpaceClient.createUser(test, "vince", "hi vince", "vince@dog.com");

		// small update no version should succeed

		JsonNode updateJson2 = Json.objectBuilder().array("items").object().put("quantity", 7).build();

		SpaceRequest.put("/1/data/sale/" + id).userAuth(vince).body(updateJson2).go(200)//
				.assertTrue("success")//
				.assertEquals("sale", "type")//
				.assertEquals(2, "version")//
				.assertEquals(id, "id");

		// check update is correct

		SpaceResponse res3 = SpaceRequest.get("/1/data/sale/" + id).backend(test).go(200)//
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

		SpaceRequest.put("/1/data/sale/" + id + "?version=1").backend(test)//
				.body("number", "0987654321").go(409).assertFalse("success");

		// update with invalid version should fail

		SpaceRequest.put("/1/data/sale/" + id + "?version=XXX").backend(test)//
				.body("number", "0987654321").go(400).assertFalse("success");

		// update with correct version should succeed

		SpaceResponse req3d = SpaceRequest.put("/1/data/sale/" + id + "?version=2").backend(test)
				.body("number", "0987654321").go(200);

		req3d.assertTrue("success").assertEquals("sale", "type").assertEquals(3, "version").assertEquals(id, "id");

		// check update is correct

		SpaceResponse res3e = SpaceRequest.get("/1/data/sale/" + id).backend(test).go(200);

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

		SpaceRequest.delete("/1/data/sale/" + id).backend(test).go(200);
		SpaceRequest.get("/1/data/sale/" + id).backend(test).go(404);
	}

	@Test
	public void deleteObjects() throws Exception {

		// prepare

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		SpaceClient.initUserDefaultSchema(test);
		ObjectNode schema = SchemaBuilder2.builder("message")//
				.textProperty("text", "english", true).build();
		SpaceClient.setSchema(schema, test);

		// should successfully create 4 messages and 1 user

		SpaceClient.createUser(test, "riri", "hi riri");

		SpaceRequest.post("/1/data/message").adminAuth(test)//
				.body("text", "what's up?").go(201);
		SpaceRequest.post("/1/data/message").adminAuth(test)//
				.body("text", "wanna drink something?").go(201);
		SpaceRequest.post("/1/data/message").adminAuth(test)//
				.body("text", "pretty cool, hein?").go(201);
		SpaceRequest.post("/1/data/message").adminAuth(test)//
				.body("text", "so long guys").go(201);

		SpaceRequest.get("/1/data").refresh(true).adminAuth(test).go(200)//
				.assertEquals(5, "total");

		// should succeed to delete all users
		SpaceRequest.delete("/1/user").adminAuth(test).go(200)//
				.assertEquals(1, "totalDeleted");
		SpaceRequest.get("/1/data").refresh(true).adminAuth(test).go(200)//
				.assertEquals(4, "total");

		// should succeed to delete all messages
		SpaceRequest.delete("/1/data/message").adminAuth(test).go(200)//
				.assertEquals(4, "totalDeleted");
		SpaceRequest.get("/1/data").refresh(true).adminAuth(test).go(200)//
				.assertEquals(0, "total");
	}

	@Test
	public void testAllObjectIdStrategies() throws Exception {

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// creates message schema with auto generated id strategy

		SpaceClient.setSchema(SchemaBuilder2.builder("message")//
				.stringProperty("text", true).build(), test);

		// creates a message object with auto generated id

		String id = SpaceRequest.post("/1/data/message").adminAuth(test)//
				.body("text", "id=?").go(201)//
				.getFromJson("id").asText();

		SpaceRequest.get("/1/data/message/" + id).adminAuth(test).go(200)//
				.assertEquals("id=?", "text");

		// creates a message object with self provided id

		SpaceRequest.post("/1/data/message?id=1").adminAuth(test)//
				.body("text", "id=1").go(201);

		SpaceRequest.get("/1/data/message/1").adminAuth(test).go(200)//
				.assertEquals("id=1", "text");

		// fails to create a message object with id subkey
		// since not compliant with schema

		SpaceRequest.post("/1/data/message").adminAuth(test)//
				.body("text", "id=2", "id", 2).go(400);

		// creates message2 schema with code property as id

		SpaceClient.setSchema(SchemaBuilder2.builder("message2", "code")//
				.stringProperty("code", true)//
				.stringProperty("text", true).build(), test);

		// creates a message2 object with code = 2

		SpaceRequest.post("/1/data/message2").adminAuth(test)//
				.body("text", "id=code=2", "code", "2").go(201);

		SpaceRequest.get("/1/data/message2/2").adminAuth(test).go(200)//
				.assertEquals("id=code=2", "text")//
				.assertEquals("2", "code");

		// creates a message2 object with code = 3
		// and the id param is transparent

		SpaceRequest.post("/1/data/message2?id=XXX").adminAuth(test)//
				.body("text", "id=code=3", "code", "3").go(201);

		SpaceRequest.get("/1/data/message2/3").adminAuth(test).go(200)//
				.assertEquals("id=code=3", "text")//
				.assertEquals("3", "code");

		// fails to create a message2 object without any code

		SpaceRequest.post("/1/data/message2").adminAuth(test)//
				.body("text", "no code").go(400);

		// fails to create a message2 object without any code
		// and the id param is still transparent

		SpaceRequest.post("/1/data/message2?id=XXX").adminAuth(test)//
				.body("text", "no code").go(400);
	}

	@Test
	public void testFromAndSizeParameters() throws Exception {

		// prepare

		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		SpaceClient.setSchema(SchemaBuilder2.builder("message")//
				.stringProperty("text", true).build(), test);

		SpaceRequest.post("/1/data/message").backend(test)//
				.body("text", "hello").go(201);
		SpaceRequest.post("/1/data/message").backend(test)//
				.body("text", "bonjour").go(201);
		SpaceRequest.post("/1/data/message").backend(test)//
				.body("text", "guttentag").go(201);
		SpaceRequest.post("/1/data/message").backend(test)//
				.body("text", "hola").go(201);

		// fetches messages by 4 pages of 1 object

		Set<String> messages = Sets.newHashSet();
		messages.addAll(fetchMessages(test, 0, 1));
		messages.addAll(fetchMessages(test, 1, 1));
		messages.addAll(fetchMessages(test, 2, 1));
		messages.addAll(fetchMessages(test, 3, 1));

		assertEquals(Sets.newHashSet("hello", "bonjour", "guttentag", "hola"), messages);

		// fetches messages by 2 pages of 2 objects

		messages.clear();
		messages.addAll(fetchMessages(test, 0, 2));
		messages.addAll(fetchMessages(test, 2, 2));

		assertEquals(Sets.newHashSet("hello", "bonjour", "guttentag", "hola"), messages);

		// fetches messages by a single page of 4 objects

		messages.clear();
		messages.addAll(fetchMessages(test, 0, 4));

		assertEquals(Sets.newHashSet("hello", "bonjour", "guttentag", "hola"), messages);

		// fails to fetch messages if from + size > 10000

		SpaceRequest.get("/1/data/message").from(9999).size(10).backend(test).go(400);

	}

	private Collection<String> fetchMessages(Backend backend, int from, int size) throws Exception {
		JsonNode results = SpaceRequest.get("/1/data/message")//
				.refresh(true).from(from).size(size)//
				.backend(backend).go(200)//
				.assertEquals(4, "total")//
				.assertSizeEquals(size, "results")//
				.getFromJson("results");

		List<String> messages = Lists.newArrayList();
		Iterator<JsonNode> elements = results.elements();
		while (elements.hasNext())
			messages.add(elements.next().get("text").asText());

		return messages;
	}
}
