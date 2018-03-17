/**
 * © David Attias 2015
 */
package io.spacedog.test;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataObjectBase;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.data.DataWrapAbstract;
import io.spacedog.client.data.ObjectNodeWrap;
import io.spacedog.client.elastic.ESQueryBuilders;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.schema.GeoPoint;
import io.spacedog.client.schema.Schema;
import io.spacedog.utils.Json;

public class DataServiceTest2 extends SpaceTest {

	public static class Sale extends DataObjectBase {

		public String number;
		public DateTime when;
		public GeoPoint where;
		public boolean online;
		public LocalDate deliveryDate;
		public LocalTime deliveryTime;
		public List<Item> items;

		public static class Item {
			public String ref;
			public String description;
			public int quantity;
			public String type;
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Wrap extends DataWrapAbstract<Sale> {

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
			public DataWrap<Sale> source(Sale source) {
				this.source = source;
				return this;
			}

		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Results {
			public long total;
			public List<Sale.Wrap> results;
			public JsonNode aggregations;
		}

	}

	@Test
	public void createSearchUpdateAndDeleteSales() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// superadmin sets sale schema
		superadmin.schemas().set(SchemaServiceTest.buildSaleSchema().build());

		// superadmin sets acl of sale schema
		DataSettings settings = new DataSettings();
		settings.acl().put("sale", Roles.user, Permission.create, Permission.updateMine, //
				Permission.readMine, Permission.deleteMine, Permission.search);
		superadmin.settings().save(settings);

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

		Sale.Item item1 = new Sale.Item();
		item1.ref = "JDM";
		item1.description = "2 rooms appartment in the heart of montmartre";
		item1.quantity = 8;
		item1.type = "appartment";

		Sale.Item item2 = new Sale.Item();
		item2.ref = "LOUVRE";
		item2.description = "Louvre museum 2 days visit with a personal guide";
		item2.quantity = 2;
		item2.type = "visit";

		sale.items = Lists.newArrayList(item1, item2);
		DataWrap<Sale> saleWrap1 = new Sale.Wrap().source(sale);
		ObjectNode saleNode = Json.checkObject(Json.toJsonNode(sale));

		DataWrap<Sale> saleWrap2 = fred.data().save(saleWrap1);
		assertEquals("sale", saleWrap2.type());
		assertEquals(1, saleWrap2.version());
		assertNotNull(saleWrap2.id());

		ObjectNode saleNode2 = Json.checkObject(Json.toJsonNode(saleWrap2.source()));
		assertEquals(saleNode, saleNode2);

		// find by id
		DataWrap<Sale> saleWrap3 = fred.data().fetch(new Sale.Wrap().id(saleWrap2.id()));

		assertEquals(fred.id(), saleWrap3.owner());
		assertNotNull(saleWrap3.group());
		assertEquals(1, saleWrap3.version());
		assertEquals("sale", saleWrap3.type());
		assertEquals(saleWrap2.id(), saleWrap3.id());
		assertEquals("1234567890", saleWrap3.source().number);
		assertEquals(-55.6765, saleWrap3.source().where.lat, 0.00002);
		assertEquals(-54.6765, saleWrap3.source().where.lon, 0.00002);
		assertEquals(sale.when, saleWrap3.source().when);
		assertFalse(saleWrap3.source().online);
		assertEquals(sale.deliveryDate, saleWrap3.source().deliveryDate);
		assertEquals(sale.deliveryTime, saleWrap3.source().deliveryTime);
		assertEquals(2, saleWrap3.source().items.size());
		assertEquals("JDM", saleWrap3.source().items.get(0).ref);
		assertEquals("2 rooms appartment in the heart of montmartre", //
				saleWrap3.source().items.get(0).description);
		assertEquals(8, saleWrap3.source().items.get(0).quantity);
		assertEquals("appartment", saleWrap3.source().items.get(0).type);
		assertEquals("LOUVRE", saleWrap3.source().items.get(1).ref);
		assertEquals("Louvre museum 2 days visit with a personal guide", //
				saleWrap3.source().items.get(1).description);
		assertEquals(2, saleWrap3.source().items.get(1).quantity);
		assertEquals("visit", saleWrap3.source().items.get(1).type);

		assertEquals(saleWrap3.createdAt(), saleWrap3.updatedAt());
		assertDateIsRecent(saleWrap3.createdAt());

		ObjectNode saleNode3 = Json.checkObject(Json.toJsonNode(saleWrap3.source()));
		assertSourceAlmostEquals(saleNode, saleNode3);

		// find by simple text search
		Sale.Results saleResults4 = fred.data().getAllRequest().type("sale")//
				.q("museum").refresh().go(Sale.Results.class);

		assertEquals(1, saleResults4.total);
		ObjectNode saleNode4 = Json.checkObject(Json.toJsonNode(saleResults4.results.get(0).source()));
		assertSourceAlmostEquals(saleNode, saleNode4);

		// find by advanced text search
		ESSearchSourceBuilder source = ESSearchSourceBuilder.searchSource()
				.query(ESQueryBuilders.queryStringQuery("museum"));
		Sale.Results saleResults5 = fred.data().searchRequest()//
				.type("sale").source(source).go(Sale.Results.class);

		assertEquals(1, saleResults5.total);
		ObjectNode saleNode5 = Json.checkObject(Json.toJsonNode(saleResults5.results.get(0).source()));
		assertSourceAlmostEquals(saleNode, saleNode5);

		// small update no version should succeed
		JsonNode updateJson2 = Json.builder().object().array("items").object().add("quantity", 7).build();
		fred.data().patch("sale", saleWrap2.id(), updateJson2);

		// check update is correct
		DataWrap<Sale> saleDO6 = fred.data().fetch(new Sale.Wrap().id(saleWrap2.id()));
		assertEquals(fred.id(), saleDO6.owner());
		assertNotNull(saleDO6.group());
		// assertEquals(sale2.source().meta().createdAt,
		// sale6.source().meta().createdAt);
		// assertDateIsRecent(sale6.source().meta().updatedAt);
		assertEquals(2, saleDO6.version());
		assertEquals("sale", saleDO6.type());
		assertEquals(saleWrap2.id(), saleDO6.id());
		assertEquals(7, saleDO6.source().items.get(0).quantity);

		// check equality on what has not been updated
		ObjectNode saleNode6 = Json.checkObject(Json.toJsonNode(saleDO6.source()));
		assertSourceAlmostEquals(saleNode, saleNode6, "items");

		// update with invalid version should fail
		assertHttpError(409, () -> fred.data().patch("sale", //
				saleWrap2.id(), Json.object("number", "0987654321"), 1l));

		// update with invalid version should fail
		assertHttpError(400, () -> fred.put("/1/data/sale/" + saleWrap2.id())//
				.queryParam("version", "XXX").bodyJson("number", "0987654321").go(200));

		// update with correct version should succeed
		saleDO6.source().number = "0987654321";
		fred.data().save(saleDO6);
		assertEquals(3, saleDO6.version());

		// get sale to check update did fine
		DataWrap<Sale> saleDO7 = fred.data().fetch(new Sale.Wrap().id(saleDO6.id()));
		assertEquals("0987654321", saleDO7.source().number);
		assertEquals(3, saleDO7.version());

		saleNode6 = Json.checkObject(Json.toJsonNode(saleDO6.source()));
		ObjectNode saleNode7 = Json.checkObject(Json.toJsonNode(saleDO7.source()));
		assertSourceAlmostEquals(saleNode6, saleNode7, "items");

		// vince fails to update nor delete this sale since not the owner
		SpaceDog vince = createTempDog(superadmin, "vince");
		assertHttpError(403, () -> vince.data().patch("sale", //
				saleDO7.id(), Json.object("number", "0123456789")));
		assertHttpError(403, () -> vince.data().delete(saleDO7));

		// fred deletes this sale since he is the owner
		fred.data().delete(saleDO7);
		assertHttpError(404, () -> fred.data().fetch(saleDO7));
	}

	@Test
	public void deleteObjects() {

		// prepare

		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		superadmin.schemas().set(Message.schema());

		// should successfully create 4 messages

		superadmin.data().save(Message.TYPE, //
				Json.object("text", "what's up?"));
		superadmin.data().save(Message.TYPE, //
				Json.object("text", "wanna drink something?"));
		superadmin.data().save(Message.TYPE, //
				Json.object("text", "pretty cool, hein?"));
		superadmin.data().save(Message.TYPE, //
				Json.object("text", "so long guys"));

		long total = superadmin.data().getAllRequest().refresh().go().total;
		assertEquals(4, total);

		// should succeed to delete all messages
		total = superadmin.data().deleteAll("message");
		assertEquals(4, total);

		// check no data anymore
		total = superadmin.data().getAllRequest().refresh().go().total;
		assertEquals(0, total);
	}

	@Test
	public void testAllObjectIdStrategies() {

		prepareTest();
		SpaceDog superadmin = clearRootBackend();

		// creates message schema with auto generated id strategy
		superadmin.schemas().set(Message.schema());

		// creates a message object with auto generated id
		ObjectNode message = Json.object("text", "id=?");
		ObjectNodeWrap createObject = superadmin.data().save(Message.TYPE, message);
		DataWrap<ObjectNode> getObject = superadmin.data().get(Message.TYPE, createObject.id());
		assertSourceAlmostEquals(message, getObject.source());

		// creates a message object with self provided id
		message = Json.object("text", "id=1");
		superadmin.data().save(Message.TYPE, "1", message);
		getObject = superadmin.data().get(Message.TYPE, "1");
		assertSourceAlmostEquals(message, getObject.source());

		// an id field does not force the id field
		superadmin.post("/1/data/message").bodyJson("text", "id=2", "id", 2).go(400);

		// an id param does not force the object id
		String id = superadmin.post("/1/data/message").queryParam("id", 23)//
				.bodyJson("text", "hello").go(201).getString("id");
		assertNotEquals("23", id);
	}

	@Test
	public void testFromAndSizeParameters() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");

		// superadmin sets message schema
		Schema schema = Message.schema();
		superadmin.schemas().set(schema);

		// superadmin sets data acl
		DataSettings dataSettings = new DataSettings();
		dataSettings.acl().put(schema.name(), Roles.user, Permission.create, Permission.search);
		superadmin.settings().save(dataSettings);

		// superadmins creates 4 messages
		Set<String> originalMessages = Sets.newHashSet(//
				"hello", "bonjour", "guttentag", "hola");
		for (String message : originalMessages)
			vince.data().save(Message.TYPE, new Message(message));

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
		Message.Results results = user.data().searchRequest()//
				.type(Message.TYPE).source(builder).refresh().go(Message.Results.class);

		assertEquals(4, results.total);
		assertEquals(size, results.results.size());

		return results.results.stream()//
				.map(object -> object.source().text)//
				.collect(Collectors.toList());
	}

	@Test
	public void testFieldManagement() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		Schema schema = Schema.builder("home").text("name")//
				.object("garage").integer("places").build();
		superadmin.schemas().set(schema);

		// home XXX does not exist
		superadmin.get("/1/data/home/XXX/name").go(404);

		// superadmin creates home 1 with name dupont
		superadmin.put("/1/data/home/1/name")//
				.bodyJson(TextNode.valueOf("dupont")).go(201);
		DataWrap<ObjectNode> home = superadmin.data().get("home", "1");
		ObjectNode homeNode = Json.object("name", "dupont");
		assertSourceAlmostEquals(homeNode, home.source());

		// guest is forbidden to update home 1 name
		guest.put("/1/data/home/1/name")//
				.bodyJson(TextNode.valueOf("meudon")).go(403);

		// superadmin sets home 1 garage places to 6
		superadmin.put("/1/data/home/1/garage.places")//
				.bodyJson(IntNode.valueOf(6)).go(200);
		home = superadmin.data().get("home", "1");
		homeNode.set("garage", Json.object("places", 6));
		assertSourceAlmostEquals(homeNode, home.source());

		// superadmin removes home 1 garage
		superadmin.delete("/1/data/home/1/garage").go(200);
		home = superadmin.data().get("home", "1");
		homeNode.remove("garage");
		assertSourceAlmostEquals(homeNode, home.source());

		// guest is forbidden to remove home 1 name
		guest.delete("/1/data/home/1/name").go(403);
	}
}
