/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceTest;
import io.spacedog.model.DataObject;
import io.spacedog.model.DataObjectAbstract;
import io.spacedog.model.GeoPoint;
import io.spacedog.model.Meta;
import io.spacedog.model.Metadata;
import io.spacedog.model.ObjectNodeDataObject;
import io.spacedog.model.Schema;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.DataResource2TestOften.Sale.Item;

public class DataResource2TestOften extends SpaceTest {

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class SaleDataObject extends DataObjectAbstract<Sale> {

		private Sale source;

		@Override
		public Class<Sale> sourceClass() {
			return Sale.class;
		}

		@Override
		public Sale source() {
			return source;
		}

		@Override
		public DataObject<Sale> source(Sale source) {
			this.source = source;
			return this;
		}

		@Override
		public String type() {
			return "sale";
		}
	}

	public static class Sale implements Metadata {

		public String number;
		public DateTime when;
		public GeoPoint where;
		public boolean online;
		public LocalDate deliveryDate;
		public LocalTime deliveryTime;
		public List<Item> items;
		public Meta meta;

		public static class Item {
			public String ref;
			public String description;
			public int quantity;
			public String type;
		}

		@Override
		public Meta meta() {
			return meta;
		}

		@Override
		public void meta(Meta meta) {
			this.meta = meta;
		}

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class SaleSearchResults {

		public long total;
		public List<SaleDataObject> results;
		public JsonNode aggregations;
	}

	@Test
	public void createSearchUpdateAndDeleteSales() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		test.schema().set(SchemaResourceTestOften.buildSaleSchema());
		SpaceDog fred = signUp(test, "fred", "hi fred");

		// fred fails to create a sale with no body
		fred.post("/1/data/sale").go(400);

		// fred creates a new sale object
		Sale sale = new Sale();
		sale.number = "1234567890";
		sale.where = new GeoPoint(-55.6765, -54.6765);
		sale.when = DateTime.parse("2015-01-09T15:37:00.123Z");
		sale.online = false;
		sale.deliveryDate = LocalDate.parse("2015-09-09");
		sale.deliveryTime = LocalTime.parse("15:30:00");

		Item item1 = new Sale.Item();
		item1.ref = "JDM";
		item1.description = "2 rooms appartment in the heart of montmartre";
		item1.quantity = 8;
		item1.type = "appartment";

		Item item2 = new Sale.Item();
		item2.ref = "LOUVRE";
		item2.description = "Louvre museum 2 days visit with a personal guide";
		item2.quantity = 2;
		item2.type = "visit";

		sale.items = Lists.newArrayList(item1, item2);
		DataObject<Sale> saleDO1 = new SaleDataObject().source(sale);
		ObjectNode saleNode = Json.checkObject(Json.toNode(sale));

		DataObject<Sale> saleDO2 = fred.data().save(saleDO1);
		assertEquals("sale", saleDO2.type());
		assertEquals(1, saleDO2.version());
		assertNotNull(saleDO2.id());

		ObjectNode saleNode2 = Json.checkObject(Json.toNode(saleDO2.source()));
		assertEquals(saleNode.deepCopy().without("meta"), //
				saleNode2.deepCopy().without("meta"));

		// find by id
		DataObject<Sale> saleDO3 = fred.data().fetch(new SaleDataObject().id(saleDO2.id()));

		assertEquals("fred", saleDO3.source().meta().createdBy());
		assertEquals("fred", saleDO3.source().meta().updatedBy());
		assertEquals(1, saleDO3.version());
		assertEquals("sale", saleDO3.type());
		assertEquals(saleDO2.id(), saleDO3.id());
		assertEquals("1234567890", saleDO3.source().number);
		assertEquals(-55.6765, saleDO3.source().where.lat, 0.00002);
		assertEquals(-54.6765, saleDO3.source().where.lon, 0.00002);
		assertEquals(sale.when, saleDO3.source().when);
		assertFalse(saleDO3.source().online);
		assertEquals(sale.deliveryDate, saleDO3.source().deliveryDate);
		assertEquals(sale.deliveryTime, saleDO3.source().deliveryTime);
		assertEquals(2, saleDO3.source().items.size());
		assertEquals("JDM", saleDO3.source().items.get(0).ref);
		assertEquals("2 rooms appartment in the heart of montmartre", //
				saleDO3.source().items.get(0).description);
		assertEquals(8, saleDO3.source().items.get(0).quantity);
		assertEquals("appartment", saleDO3.source().items.get(0).type);
		assertEquals("LOUVRE", saleDO3.source().items.get(1).ref);
		assertEquals("Louvre museum 2 days visit with a personal guide", //
				saleDO3.source().items.get(1).description);
		assertEquals(2, saleDO3.source().items.get(1).quantity);
		assertEquals("visit", saleDO3.source().items.get(1).type);

		assertEquals(saleDO3.source().meta().createdAt(), saleDO3.source().meta().updatedAt());
		assertDateIsRecent(saleDO3.source().meta().createdAt());

		ObjectNode saleNode3 = Json.checkObject(Json.toNode(saleDO3.source()));
		assertSourceAlmostEquals(saleNode, saleNode3);

		// find by simple text search
		new SaleSearchResults();
		SaleSearchResults sale4 = fred.data()//
				.search("sale", "museum", SaleSearchResults.class, true);

		assertEquals(1, sale4.total);

		ObjectNode saleNode4 = Json.checkObject(Json.toNode(sale4.results.get(0).source()));
		assertSourceAlmostEquals(saleNode, saleNode4);

		// find by advanced text search
		String query = Json.objectBuilder()//
				.object("query")//
				.object("query_string")//
				.put("query", "museum")//
				.build().toString();

		SaleSearchResults sale5 = fred.post("/1/search/sale").bodyString(query).go(200)//
				.assertEquals(1, "total").toPojo(SaleSearchResults.class);

		ObjectNode saleNode5 = Json.checkObject(Json.toNode(sale5.results.get(0).source()));
		assertSourceAlmostEquals(saleNode, saleNode5);

		// small update no version should succeed
		JsonNode updateJson2 = Json.objectBuilder().array("items").object().put("quantity", 7).build();
		fred.data().patch("sale", saleDO2.id(), updateJson2);

		// check update is correct
		DataObject<Sale> saleDO6 = fred.data().fetch(new SaleDataObject().id(saleDO2.id()));
		assertEquals("fred", saleDO6.source().meta().createdBy());
		assertEquals("fred", saleDO6.source().meta().updatedBy());
		// assertEquals(sale2.source().meta().createdAt,
		// sale6.source().meta().createdAt);
		// assertDateIsRecent(sale6.source().meta().updatedAt);
		assertEquals(2, saleDO6.version());
		assertEquals("sale", saleDO6.type());
		assertEquals(saleDO2.id(), saleDO6.id());
		assertEquals(7, saleDO6.source().items.get(0).quantity);

		// check equality on what has not been updated
		ObjectNode saleNode6 = Json.checkObject(Json.toNode(saleDO6.source()));
		assertEquals(saleNode.deepCopy().without(Lists.newArrayList("meta", "items")), //
				saleNode6.deepCopy().without(Lists.newArrayList("meta", "items")));

		// update with invalid version should fail
		fred.put("/1/data/sale/" + saleDO2.id()).queryParam("version", "1")//
				.bodyJson("number", "0987654321").go(409);

		// update with invalid version should fail
		fred.put("/1/data/sale/" + saleDO2.id()).queryParam("version", "XXX")//
				.bodyJson("number", "0987654321").go(400);

		// update with correct version should succeed
		saleDO6.source().number = "0987654321";
		fred.data().save(saleDO6);
		assertEquals(3, saleDO6.version());

		// get sale to check update did fine
		DataObject<Sale> saleDO7 = fred.data().fetch(new SaleDataObject().id(saleDO6.id()));
		assertEquals("0987654321", saleDO7.source().number);
		assertEquals(3, saleDO7.version());

		saleNode6 = Json.checkObject(Json.toNode(saleDO6.source()));
		ObjectNode saleNode7 = Json.checkObject(Json.toNode(saleDO7.source()));
		assertEquals(saleNode6.deepCopy().without(Lists.newArrayList("meta", "items")), //
				saleNode7.deepCopy().without(Lists.newArrayList("meta", "items")));

		// vince fails to update nor delete this sale since not the owner
		SpaceDog vince = signUp(test, "vince", "hi vince");
		vince.put("/1/data/sale/" + saleDO7.id())//
				.bodyJson("number", "0123456789").go(403);
		vince.delete("/1/data/sale/" + saleDO7.id()).go(403);

		// fred deletes this sale since he is the owner
		fred.data().delete(saleDO7);
		fred.get("/1/data/sale/" + saleDO7.id()).go(404);
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
		DataObject<ObjectNode> createObject = superadmin.data().save("message", message);
		DataObject<ObjectNode> getObject = superadmin.data().get("message", createObject.id());
		assertEquals(message, getObject.source().without("meta"));

		// creates a message object with self provided id
		message = Json.object("text", "id=1");
		superadmin.data().save("message", "1", message);
		getObject = superadmin.data().get("message", "1");
		assertEquals(message, getObject.source().without("meta"));

		// an id field does not force the id field
		superadmin.post("/1/data/message").bodyJson("text", "id=2", "id", 2).go(400);

		// an id param does not force the object id
		String id = superadmin.post("/1/data/message").queryParam("id", "23")//
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
			vince.data().save("message", Json.object("text", message));

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
		ObjectNodeDataObject.Results results = user.data()//
				.search("message", builder, ObjectNodeDataObject.Results.class, true);

		assertEquals(4, results.total);
		assertEquals(size, results.results.size());

		return results.results.stream()//
				.map(object -> object.source().get("text").asText())//
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
		DataObject<ObjectNode> home = superadmin.data().get("home", "1");
		ObjectNode homeNode = Json.object("name", "dupont");
		assertEquals(homeNode, home.source().without(META_FIELD));

		// guest is forbidden to update home 1 name
		guest.put("/1/data/home/1/name")//
				.bodyJson(TextNode.valueOf("meudon")).go(403);

		// superadmin sets home 1 garage places to 6
		superadmin.put("/1/data/home/1/garage.places")//
				.bodyJson(IntNode.valueOf(6)).go(200);
		home = superadmin.data().get("home", "1");
		homeNode.set("garage", Json.object("places", 6));
		assertEquals(homeNode, home.source().without(META_FIELD));

		// superadmin removes home 1 garage
		superadmin.delete("/1/data/home/1/garage").go(200);
		home = superadmin.data().get("home", "1");
		homeNode.remove("garage");
		assertEquals(homeNode, home.source().without(META_FIELD));

		// guest is forbidden to remove home 1 name
		guest.delete("/1/data/home/1/name").go(403);
	}
}
