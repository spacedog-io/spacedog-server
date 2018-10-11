/**
 * © David Attias 2015
 */
package io.spacedog.test.data;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataObjectBase;
import io.spacedog.client.data.DataResults;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.elastic.ESQueryBuilders;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.schema.GeoPoint;
import io.spacedog.client.schema.Schema;
import io.spacedog.test.Message;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Json;

public class DataRestyTest2 extends SpaceTest {

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

			@Override
			public boolean equals(Object obj) {
				if (obj instanceof Item == false)
					return false;

				Item item = (Item) obj;
				return Objects.equals(this.ref, item.ref)//
						&& Objects.equals(this.description, item.description)//
						&& Objects.equals(this.quantity, item.quantity)//
						&& Objects.equals(this.type, item.type);
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Sale == false)
				return false;

			Sale sale = (Sale) obj;
			return super.equals(sale)//
					&& Objects.equals(this.number, sale.number)//
					&& Objects.equals(this.when, sale.when)//
					&& Objects.equals(this.where, sale.where)//
					&& Objects.equals(this.online, sale.online)//
					&& Objects.equals(this.deliveryDate, sale.deliveryDate)//
					&& Objects.equals(this.deliveryTime, sale.deliveryTime)//
					&& Objects.equals(this.items, sale.items);
		}
	}

	@Test
	public void createSearchUpdateAndDeleteSales() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog fred = createTempDog(superadmin, "fred");

		// superadmin sets sale schema
		superadmin.schemas().set(SchemaRestyTest.buildSaleSchema());

		// superadmin sets acl of sale schema
		DataSettings settings = new DataSettings();
		settings.acl().put("sale", Roles.user, Permission.create, Permission.updateMine, //
				Permission.readMine, Permission.deleteMine, Permission.search);
		superadmin.data().settings(settings);

		// fred fails to create a sale with no body
		fred.post("/2/data/sale").go(400);

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
		DataWrap<Sale> wrap1 = fred.data().save(DataWrap.wrap(sale));

		assertEquals("sale", wrap1.type());
		assertEquals(1, wrap1.version());
		assertNotNull(wrap1.id());
		assertEquals(fred.id(), wrap1.owner());
		assertNotNull(wrap1.group());
		assertDateIsRecent(wrap1.createdAt());
		assertDateIsRecent(wrap1.updatedAt());
		assertEquals(wrap1.createdAt(), wrap1.updatedAt());

		assertAlmostEquals(sale, wrap1.source());

		// find by id
		DataWrap<Sale> wrap2 = fred.data().getWrapped("sale", wrap1.id(), Sale.class);

		assertEquals(fred.id(), wrap2.owner());
		assertNotNull(wrap2.group());
		assertEquals(1, wrap2.version());
		assertEquals("sale", wrap2.type());
		assertEquals(wrap1.id(), wrap2.id());
		assertEquals("1234567890", wrap2.source().number);
		assertEquals(-55.6765, wrap2.source().where.lat, 0.00002);
		assertEquals(-54.6765, wrap2.source().where.lon, 0.00002);
		assertEquals(sale.when, wrap2.source().when);
		assertFalse(wrap2.source().online);
		assertEquals(sale.deliveryDate, wrap2.source().deliveryDate);
		assertEquals(sale.deliveryTime, wrap2.source().deliveryTime);
		assertEquals(2, wrap2.source().items.size());
		assertEquals("JDM", wrap2.source().items.get(0).ref);
		assertEquals("2 rooms appartment in the heart of montmartre", //
				wrap2.source().items.get(0).description);
		assertEquals(8, wrap2.source().items.get(0).quantity);
		assertEquals("appartment", wrap2.source().items.get(0).type);
		assertEquals("LOUVRE", wrap2.source().items.get(1).ref);
		assertEquals("Louvre museum 2 days visit with a personal guide", //
				wrap2.source().items.get(1).description);
		assertEquals(2, wrap2.source().items.get(1).quantity);
		assertEquals("visit", wrap2.source().items.get(1).type);

		assertAlmostEquals(sale, wrap2.source());

		// find by simple text search
		DataResults<Sale> results = fred.data().prepareGetAll().type("sale")//
				.q("museum").refresh(true).go(Sale.class);

		assertEquals(1, results.total);
		assertAlmostEquals(sale, results.objects.get(0).source());

		// find by advanced text search
		ESSearchSourceBuilder source = ESSearchSourceBuilder.searchSource()
				.query(ESQueryBuilders.queryStringQuery("museum"));
		results = fred.data().prepareSearch()//
				.type("sale").source(source.toString()).go(Sale.class);

		assertEquals(1, results.total);
		assertAlmostEquals(sale, results.objects.get(0).source());

		// small update no version should succeed
		fred.data().patch("sale", wrap1.id(), //
				Json.object("items", Json.array(Json.object("quantity", 7))));

		// check update is correct
		DataWrap<Sale> wrap3 = fred.data().getWrapped("sale", wrap1.id(), Sale.class);
		assertEquals(fred.id(), wrap3.owner());
		assertNotNull(wrap3.group());
		assertEquals(wrap1.createdAt(), wrap3.createdAt());
		assertDateIsRecent(wrap3.updatedAt());
		assertTrue(wrap1.updatedAt().isBefore(wrap3.updatedAt()));
		assertEquals(2, wrap3.version());
		assertEquals("sale", wrap3.type());
		assertEquals(wrap1.id(), wrap3.id());
		assertEquals(7, wrap3.source().items.get(0).quantity);

		// check equality on what has not been updated
		assertAlmostEquals(sale, wrap3.source(), "items");

		// update with invalid version should fail
		assertHttpError(409, () -> fred.data().patch("sale", //
				wrap1.id(), Json.object("number", "0987654321"), 1));

		// update with invalid version should fail
		fred.put("/2/data/sale/" + wrap1.id())//
				.queryParam("version", "XXX").bodyJson("number", "0987654321").go(400);

		// update with correct version should succeed
		wrap3.source().number = "0987654321";
		fred.data().save(wrap3);
		assertEquals(3, wrap3.version());

		// get sale to check update did fine
		DataWrap<Sale> wrap4 = fred.data().getWrapped("sale", wrap1.id(), Sale.class);
		assertEquals("0987654321", wrap4.source().number);
		assertEquals(3, wrap4.version());

		assertAlmostEquals(wrap3.source(), wrap4.source(), "items");

		// vince fails to update nor delete this sale since not the owner
		SpaceDog vince = createTempDog(superadmin, "vince");
		assertHttpError(403, () -> vince.data().patch("sale", //
				wrap4.id(), Json.object("number", "0123456789")));
		assertHttpError(403, () -> vince.data().delete(wrap4));

		// fred deletes this sale since he is the owner
		fred.data().delete(wrap4);
		assertHttpError(404, () -> fred.data().fetch(wrap4));
	}

	@Test
	public void deleteObjects() {

		// prepare

		prepareTest();
		SpaceDog superadmin = clearServer();
		superadmin.schemas().set(Message.schema());

		// should successfully create 4 messages

		superadmin.data().save(Message.TYPE, Json.object("text", "what's up?"));
		superadmin.data().save(Message.TYPE, Json.object("text", "wanna drink something?"));
		superadmin.data().save(Message.TYPE, Json.object("text", "pretty cool, hein?"));
		superadmin.data().save(Message.TYPE, Json.object("text", "so long guys"));

		long total = superadmin.data().prepareGetAll().refresh(true).go().total;
		assertEquals(4, total);

		// should succeed to delete all messages
		total = superadmin.data().deleteAll("message");
		assertEquals(4, total);

		// check no data anymore
		total = superadmin.data().prepareGetAll().refresh(true).go().total;
		assertEquals(0, total);
	}

	@Test
	public void testAllObjectIdStrategies() {

		prepareTest();
		SpaceDog superadmin = clearServer();

		// creates message schema with auto generated id strategy
		superadmin.schemas().set(Message.schema());

		// creates a message object with auto generated id
		ObjectNode message = Json.object("text", "id=?");
		DataWrap<ObjectNode> createObject = superadmin.data().save(Message.TYPE, message);
		DataWrap<ObjectNode> getObject = superadmin.data().getWrapped(Message.TYPE, createObject.id());
		assertAlmostEquals(message, getObject.source());

		// creates a message object with self provided id
		message = Json.object("text", "id=1");
		superadmin.data().save(Message.TYPE, message, "1");
		getObject = superadmin.data().getWrapped(Message.TYPE, "1");
		assertAlmostEquals(message, getObject.source());

		// an id field does not force the id field
		superadmin.post("/2/data/message").bodyJson("text", "id=2", "id", 2).go(400);

		// an id param does not force the object id
		String id = superadmin.post("/2/data/message").queryParam("id", 23)//
				.bodyJson("text", "hello").go(201).getString("id");
		assertNotEquals("23", id);
	}

	@Test
	public void testFromAndSizeParameters() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog vince = createTempDog(superadmin, "vince");

		// superadmin sets message schema
		Schema schema = Message.schema();
		superadmin.schemas().set(schema);

		// superadmin sets data acl
		DataSettings dataSettings = new DataSettings();
		dataSettings.acl().put(schema.name(), Roles.user, Permission.create, Permission.search);
		superadmin.data().settings(dataSettings);

		// superadmins creates 4 messages
		Set<String> originalMessages = Sets.newHashSet(//
				"hello", "bonjour", "guttentag", "hola");
		for (String message : originalMessages)
			vince.data().save(new Message(message));

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
		vince.get("/2/data/message").from(9999).size(10).go(400);
	}

	private Collection<String> fetchMessages(SpaceDog user, int from, int size) {
		ESSearchSourceBuilder builder = ESSearchSourceBuilder.searchSource()//
				.from(from).size(size);
		DataResults<Message> results = user.data().prepareSearch()//
				.type(Message.TYPE).source(builder.toString()).refresh(true).go(Message.class);

		assertEquals(4, results.total);
		assertEquals(size, results.objects.size());

		return results.objects.stream()//
				.map(object -> object.source().text)//
				.collect(Collectors.toList());
	}

	@Test
	public void testFieldManagement() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();
		Schema schema = Schema.builder("home").text("name")//
				.object("garage").integer("places").build();
		superadmin.schemas().set(schema);

		// home XXX does not exist
		superadmin.get("/2/data/home/XXX/name").go(404);

		// superadmin creates home 1 with name dupont
		superadmin.put("/2/data/home/1/name")//
				.bodyJson(TextNode.valueOf("dupont")).go(201);
		DataWrap<ObjectNode> home = superadmin.data().getWrapped("home", "1");
		ObjectNode homeNode = Json.object("name", "dupont");
		assertAlmostEquals(homeNode, home.source());

		// guest is forbidden to update home 1 name
		guest.put("/2/data/home/1/name")//
				.bodyJson(TextNode.valueOf("meudon")).go(403);

		// superadmin sets home 1 garage places to 6
		superadmin.put("/2/data/home/1/garage.places")//
				.bodyJson(IntNode.valueOf(6)).go(200);
		home = superadmin.data().getWrapped("home", "1");
		homeNode.set("garage", Json.object("places", 6));
		assertAlmostEquals(homeNode, home.source());

		// superadmin removes home 1 garage
		superadmin.delete("/2/data/home/1/garage").go(200);
		home = superadmin.data().getWrapped("home", "1");
		homeNode.remove("garage");
		assertAlmostEquals(homeNode, home.source());

		// guest is forbidden to remove home 1 name
		guest.delete("/2/data/home/1/name").go(403);
	}
}
