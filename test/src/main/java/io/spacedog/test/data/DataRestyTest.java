/**
 * © David Attias 2015
 */
package io.spacedog.test.data;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataResults;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.schema.Schema;
import io.spacedog.test.Message;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Json;

public class DataRestyTest extends SpaceTest {

	@Test
	public void createFindUpdateAndDelete() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog vince = createTempDog(superadmin, "vince");

		// superadmin sets car schema
		superadmin.schemas().set(SchemaRestyTest.buildCarSchema());

		// superadmin sets acl of car schema
		DataSettings settings = new DataSettings();
		settings.acl().put("car", Roles.user, Permission.create, Permission.updateMine, //
				Permission.readMine, Permission.deleteMine, Permission.search);
		superadmin.data().settings(settings);

		ObjectNode car = Json.builder().object() //
				.add("serialNumber", "1234567890") //
				.add("buyDate", "2015-01-09") //
				.add("buyTime", "15:37:00") //
				.add("buyTimestamp", "2015-01-09T15:37:00.123Z") //
				.add("color", "red") //
				.add("techChecked", false) //
				.object("model") //
				.add("description", "Cette voiture sent bon la France. Elle est inventive et raffinée.") //
				.add("fiscalPower", 8) //
				.add("size", 4.67) //
				.end().object("location") //
				.add("lat", -55.6765) //
				.add("lon", -54.6765) //
				.build();

		// create
		DataWrap<ObjectNode> carWrap = vince.data().save("car", car);
		assertEquals("car", carWrap.type());
		assertNotNull(carWrap.id());

		// find by id
		DataWrap<ObjectNode> car1 = vince.data().getWrapped("car", carWrap.id());

		assertEquals(vince.id(), car1.owner());
		assertNotNull(car1.group());
		DateTime createdAt = assertDateIsRecent(car1.createdAt());
		DateTime updatedAt = assertDateIsRecent(car1.updatedAt());
		assertEquals(createdAt, updatedAt);
		assertAlmostEquals(car, car1.source());

		// find by full text search
		DataResults<ObjectNode> results = vince.data().prepareGetAll().refresh(true).q("inVENT*").go();
		assertEquals(1, results.total);
		assertEquals(carWrap.id(), results.objects.get(0).id());

		// update
		vince.data().patch("car", carWrap.id(), Json.object("color", "blue"));

		DataWrap<ObjectNode> car3 = vince.data().getWrapped("car", carWrap.id());
		assertEquals(vince.id(), car3.owner());
		assertNotNull(car3.group());
		assertEquals(createdAt, car3.createdAt());

		updatedAt = assertDateIsRecent(car3.updatedAt());
		assertTrue(updatedAt.isAfter(createdAt));

		assertEquals("1234567890", car3.source().get("serialNumber").asText());
		assertEquals("blue", car3.source().get("color").asText());

		// delete
		vince.data().delete(carWrap);
		assertHttpError(404, () -> vince.data().delete(carWrap));
	}

	@Test
	public void createFindUpdateAndDeleteWithSdk() {

		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog vince = createTempDog(superadmin, "vince");

		superadmin.schemas().set(SchemaRestyTest.buildCarSchema());

		DataSettings settings = new DataSettings();
		settings.acl().put("car", Roles.user, Permission.create, Permission.updateMine, //
				Permission.readMine, Permission.deleteMine, Permission.search);
		superadmin.data().settings(settings);

		ObjectNode car = Json.builder().object() //
				.add("serialNumber", "1234567890") //
				.add("buyDate", "2015-01-09") //
				.add("buyTime", "15:37:00") //
				.add("buyTimestamp", "2015-01-09T15:37:00.123Z") //
				.add("color", "red") //
				.add("techChecked", false) //
				.object("model") //
				.add("description", "Cette voiture sent bon la France. Elle est inventive et raffinée.") //
				.add("fiscalPower", 8) //
				.add("size", 4.67) //
				.end().object("location") //
				.add("lat", -55.6765) //
				.add("lon", -54.6765) //
				.build();

		// create
		String id = vince.data().save("car", car).id();

		// find by id
		DataWrap<ObjectNode> carWrap = vince.data().getWrapped("car", id);
		assertEquals(vince.id(), carWrap.owner());
		assertNotNull(carWrap.group());
		assertNotNull(carWrap.createdAt());
		assertAlmostEquals(car, carWrap.source());

		// find by full text search
		DataResults<ObjectNode> results = vince.data().prepareGetAll()//
				.refresh(true).q("inVENT*").go();
		assertEquals(1, results.total);
		assertEquals(id, results.objects.get(0).id());

		// update
		vince.data().patch("car", id, Json.object("color", "blue"));
		carWrap = vince.data().getWrapped("car", id);
		assertEquals("blue", carWrap.source().get("color").asText());

		// delete
		vince.data().delete("car", id);
		assertHttpError(404, () -> vince.data().delete("car", id));
	}

	@Test
	public void createDataObjectWithCustomMeta() {

		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog nath = createTempDog(superadmin, "nath");
		SpaceDog operator = createTempDog(superadmin, "operator", "operator");

		// superadmin creates message schema
		Schema schema = Message.schema();
		superadmin.schemas().set(schema);

		// superadmin sets data acl
		DataSettings settings = new DataSettings();
		settings.acl().put(schema.name(), Roles.user, Permission.create, Permission.readMine);
		settings.acl().put(schema.name(), "operator", Permission.create, Permission.update, //
				Permission.forceMeta, Permission.read);
		superadmin.data().settings(settings);

		// old message to insert again in database
		DateTime now = DateTime.now();
		Message message = new Message();
		message.text = "toto";
		message.owner(nath.id());
		// TODO with new group access control feature
		// forceMeta is not anymore capable of forcing group
		// refactor the force meta feature
		// message.group(nath.group());
		message.createdAt(now.minusDays(2));
		message.updatedAt(now.minusDays(2).plusHours(1));

		// vince creates a message object
		vince.data().save(message, "1");

		// but provided meta dates are not saved
		Message downloaded = vince.data().get(Message.TYPE, "1", Message.class);

		assertEquals(message.text, downloaded.text);
		assertEquals(vince.id(), downloaded.owner());
		assertEquals(vince.group(), downloaded.group());
		assertTrue(downloaded.createdAt().isAfter(now.minusHours(1)));
		assertTrue(downloaded.updatedAt().isAfter(now.minusHours(1)));
		assertTrue(downloaded.createdAt().isEqual(downloaded.updatedAt()));

		// vince is not allowed to force save custom meta
		// since he does'nt have 'updateMeta' permission
		vince.put("/2/data/message/2")//
				.queryParam(FORCE_META_PARAM, true)//
				.bodyPojo(message)//
				.go(403).asVoid();

		// operator can create a new message with custom meta
		operator.put("/2/data/message/2")//
				.queryParam(FORCE_META_PARAM, true)//
				.bodyPojo(message)//
				.go(200).asVoid();

		// provided custom meta are saved
		// and nath can access this object since the owner
		downloaded = nath.data().get(Message.TYPE, "2", Message.class);

		assertEquals(message.text, downloaded.text);
		assertEquals(nath.id(), downloaded.owner());
		// TODO idem
		// assertEquals(nath.group(), downloaded.group());
		assertTrue(message.createdAt().isEqual(downloaded.createdAt()));
		assertTrue(message.updatedAt().isEqual(downloaded.updatedAt()));

		// operator is allowed to force update vince's object
		operator.put("/2/data/message/1")//
				.queryParam(FORCE_META_PARAM, true)//
				.bodyPojo(message)//
				.go(200).asVoid();

		// vince can not access his object anymore
		// since owner has been updated to nath by operator
		vince.get("/2/data/message/1").go(403).asVoid();
	}
}
