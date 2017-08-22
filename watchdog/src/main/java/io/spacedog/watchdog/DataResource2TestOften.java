/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.spacedog.model.Schema;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceResponse;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.DataEndpoint.SearchResults;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.sdk.elastic.ESSearchSourceBuilder;
import io.spacedog.utils.Json;

public class DataResource2TestOften extends SpaceTest {

	@Test
	public void createSearchUpdateAndDeleteSales() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		test.schema().set(SchemaResourceTestOften.buildSaleSchema());
		SpaceDog fred = signUp(test, "fred", "hi fred");

		// fred fails to create a sale with no body
		SpaceRequest.post("/1/data/sale").auth(fred).go(400);

		// fred creates a new sale object
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

		String id = SpaceRequest.post("/1/data/sale").auth(fred).bodyJson(sale).go(201)//
				.assertTrue("success")//
				.assertEquals("sale", "type")//
				.assertEquals(1, "version")//
				.assertNotNull("id")//
				.getString("id");

		// find by id

		SpaceResponse res1 = SpaceRequest.get("/1/data/sale/" + id).auth(fred).go(200)//
				.assertEquals("fred", "meta.createdBy")//
				.assertEquals("fred", "meta.updatedBy")//
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

		DateTime createdAt = DateTime.parse(res1.getString("meta.createdAt"));
		res1.assertDateIsRecent("meta.createdAt")//
				.assertEquals(createdAt, "meta.updatedAt");

		// TODO do we need to assert field by field and then full object with
		// full object?
		// copy to be able to reuse the res1 object in the following
		res1.assertEqualsWithoutMeta(sale);

		// find by simple text search

		SpaceResponse res1b = SpaceRequest.get("/1/search/sale")//
				.queryParam("q", "museum").refresh().auth(fred)//
				.go(200).assertEquals(1, "total");

		res1.assertEqualsWithoutMeta(Json.checkObject(res1b.get("results.0")));

		// find by advanced text search

		String query = Json.objectBuilder()//
				.object("query")//
				.object("query_string")//
				.put("query", "museum")//
				.build().toString();

		SpaceResponse res1c = SpaceRequest.post("/1/search/sale").auth(fred).bodyString(query).go(200).assertEquals(1,
				"total");

		res1.assertEqualsWithoutMeta(Json.checkObject(res1c.get("results.0")));

		// small update no version should succeed

		JsonNode updateJson2 = Json.objectBuilder().array("items").object().put("quantity", 7).build();

		SpaceRequest.put("/1/data/sale/" + id).auth(fred).bodyJson(updateJson2).go(200)//
				.assertTrue("success")//
				.assertEquals("sale", "type")//
				.assertEquals(2, "version")//
				.assertEquals(id, "id");

		// check update is correct

		SpaceResponse res3 = SpaceRequest.get("/1/data/sale/" + id).auth(fred).go(200)//
				.assertEquals("fred", "meta.createdBy")//
				.assertEquals("fred", "meta.updatedBy")//
				.assertEquals(createdAt, "meta.createdAt")//
				.assertDateIsRecent("meta.updatedAt")//
				.assertEquals(2, "meta.version")//
				.assertEquals("sale", "meta.type")//
				.assertEquals(id, "meta.id")//
				.assertEquals(7, "items.0.quantity");

		// check equality on what has not been updated
		assertEquals(sale.deepCopy().without("items"),
				res3.asJsonObject().deepCopy().without(Lists.newArrayList("meta", "items")));

		// update with invalid version should fail

		SpaceRequest.put("/1/data/sale/" + id).auth(fred).queryParam("version", "1")//
				.bodyJson("number", "0987654321").go(409).assertFalse("success");

		// update with invalid version should fail

		SpaceRequest.put("/1/data/sale/" + id)//
				.queryParam("version", "XXX").auth(fred)//
				.bodyJson("number", "0987654321").go(400)//
				.assertFalse("success");

		// update with correct version should succeed

		SpaceResponse req3d = SpaceRequest.put("/1/data/sale/" + id).auth(fred)//
				.queryParam("version", "2").bodyJson("number", "0987654321").go(200);

		req3d.assertTrue("success").assertEquals("sale", "type").assertEquals(3, "version").assertEquals(id, "id");

		// check update is correct

		SpaceResponse res3e = SpaceRequest.get("/1/data/sale/" + id).backend(test).go(200);

		res3e.assertEquals("fred", "meta.createdBy")//
				.assertEquals("fred", "meta.updatedBy")//
				.assertEquals(3, "meta.version")//
				.assertEquals("sale", "meta.type")//
				.assertEquals(id, "meta.id")//
				.assertEquals("0987654321", "number")//
				.assertEquals(createdAt, "meta.createdAt")//
				.assertDateIsRecent("meta.updatedAt");

		// check equality on what has not been updated
		assertEquals(res3.asJsonObject().deepCopy().without(Lists.newArrayList("meta", "number")),
				res3e.asJsonObject().deepCopy().without(Lists.newArrayList("meta", "number")));

		// vince fails to update nor delete this sale since not the owner
		SpaceDog vince = signUp(test, "vince", "hi vince");
		SpaceRequest.put("/1/data/sale/" + id).auth(vince)//
				.bodyJson("number", "0123456789").go(403);
		SpaceRequest.delete("/1/data/sale/" + id).auth(vince).go(403);

		// fred deletes this sale since he is the owner
		SpaceRequest.delete("/1/data/sale/" + id).auth(fred).go(200);
		SpaceRequest.get("/1/data/sale/" + id).auth(fred).go(404);
	}

	@Test
	public void deleteObjects() {

		// prepare

		prepareTest();
		SpaceDog test = resetTestBackend();
		test.schema().set(Schema.builder("message").text("text").build());

		// should successfully create 4 messages

		SpaceRequest.post("/1/data/message").auth(test)//
				.bodyJson("text", "what's up?").go(201);
		SpaceRequest.post("/1/data/message").auth(test)//
				.bodyJson("text", "wanna drink something?").go(201);
		SpaceRequest.post("/1/data/message").auth(test)//
				.bodyJson("text", "pretty cool, hein?").go(201);
		SpaceRequest.post("/1/data/message").auth(test)//
				.bodyJson("text", "so long guys").go(201);

		SpaceRequest.get("/1/data").refresh().auth(test).go(200)//
				.assertEquals(4, "total");

		// should succeed to delete all messages
		SpaceRequest.delete("/1/data/message").auth(test).go(200)//
				.assertEquals(4, "totalDeleted");
		SpaceRequest.get("/1/data").refresh().auth(test).go(200)//
				.assertEquals(0, "total");
	}

	@Test
	public void testAllObjectIdStrategies() {

		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// creates message schema with auto generated id strategy
		superadmin.schema().set(Schema.builder("message").string("text").build());

		// creates a message object with auto generated id
		ObjectNode message = Json.object("text", "id=?");
		String id = superadmin.data().create("message", message);
		ObjectNode node = superadmin.data().get("message", id);
		assertEquals(message, node.without("meta"));

		// creates a message object with self provided id
		message = Json.object("text", "id=1");
		superadmin.data().create("message", "1", message);
		node = superadmin.data().get("message", "1");
		assertEquals(message, node.without("meta"));

		// an id field does not force the id field
		superadmin.post("/1/data/message").bodyJson("text", "id=2", "id", 2).go(400);

		// an id param does not force the object id
		id = superadmin.post("/1/data/message").queryParam("id", "23")//
				.bodyJson("text", "hello").go(201).getString("id");
		assertNotEquals("23", id);
	}

	@Test
	public void testFromAndSizeParameters() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog vince = signUp(superadmin, "vince", "hi vince");
		superadmin.schema().set(Schema.builder("message").string("text").build());

		// superadmins creates 4 messages
		HashSet<String> originalMessages = Sets.newHashSet(//
				"hello", "bonjour", "guttentag", "hola");
		for (String message : originalMessages)
			vince.data().create("message", Json.object("text", message));

		// fetches messages by 4 pages of 1 object
		Set<String> messages = Sets.newHashSet();
		messages.addAll(fetchMessages(vince, 0, 1));
		messages.addAll(fetchMessages(vince, 1, 1));
		messages.addAll(fetchMessages(vince, 2, 1));
		messages.addAll(fetchMessages(vince, 3, 1));
		assertEquals(originalMessages, messages);

		// fetches messages by 2 pages of 2 objects
		messages.clear();
		messages.addAll(fetchMessages(vince, 0, 2));
		messages.addAll(fetchMessages(vince, 2, 2));
		assertEquals(originalMessages, messages);

		// fetches messages by a single page of 4 objects
		messages.clear();
		messages.addAll(fetchMessages(vince, 0, 4));
		assertEquals(originalMessages, messages);

		// fails to fetch messages if from + size > 10000
		vince.get("/1/data/message").from(9999).size(10).go(400);
	}

	private Collection<String> fetchMessages(SpaceDog user, int from, int size) {
		ESSearchSourceBuilder builder = ESSearchSourceBuilder.searchSource()//
				.from(from).size(size);
		SearchResults<ObjectNode> results = user.data()//
				.search("message", builder, ObjectNode.class, true);

		assertEquals(4, results.total());
		assertEquals(size, results.objects().size());

		return results.objects().stream()//
				.map(node -> node.get("text").asText())//
				.collect(Collectors.toList());
	}

	@Test
	public void testFieldManagement() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		Schema schema = Schema.builder("home").text("name")//
				.object("garage").integer("places").build();
		superadmin.schema().set(schema);

		// home XXX does not exist
		superadmin.get("/1/data/home/XXX/name").go(404);

		// superadmin creates home 1 with name dupont
		superadmin.put("/1/data/home/1/name")//
				.bodyJson(TextNode.valueOf("dupont")).go(201);
		ObjectNode node = superadmin.data().get("home", "1");
		ObjectNode home1 = Json.object("name", "dupont");
		assertEquals(home1, node.without(META_FIELD));

		// guest is forbidden to update home 1 name
		guest.put("/1/data/home/1/name")//
				.bodyJson(TextNode.valueOf("meudon")).go(403);

		// superadmin sets home 1 garage places to 6
		superadmin.put("/1/data/home/1/garage.places")//
				.bodyJson(IntNode.valueOf(6)).go(200);
		node = superadmin.data().get("home", "1");
		home1.set("garage", Json.object("places", 6));
		assertEquals(home1, node.without(META_FIELD));

		// superadmin removes home 1 garage
		superadmin.delete("/1/data/home/1/garage").go(200);
		node = superadmin.data().get("home", "1");
		home1.remove("garage");
		assertEquals(home1, node.without(META_FIELD));

		// guest is forbidden to remove home 1 name
		guest.delete("/1/data/home/1/name").go(403);

	}
}
