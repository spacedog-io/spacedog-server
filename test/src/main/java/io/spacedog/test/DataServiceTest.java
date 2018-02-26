/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.http.SpaceRequestException;
import io.spacedog.model.JsonDataObject;
import io.spacedog.model.Schema;
import io.spacedog.utils.Json;

public class DataServiceTest extends SpaceTest {

	@Test
	public void createFindUpdateAndDelete() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");

		Schema carSchema = SchemaServiceTest.buildCarSchema()//
				.acl(Roles.user, Permission.create, Permission.updateMine, //
						Permission.readMine, Permission.deleteMine, Permission.search)//
				.build();
		superadmin.schemas().set(carSchema);

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
		JsonDataObject carDO = vince.data().save("car", car);
		assertEquals("car", carDO.type());
		assertNotNull(carDO.id());

		// find by id
		JsonDataObject car1 = vince.data().get("car", carDO.id());

		assertEquals(vince.id(), car1.owner());
		assertNotNull(car1.group());
		DateTime createdAt = assertDateIsRecent(car1.createdAt());
		DateTime updatedAt = assertDateIsRecent(car1.updatedAt());
		assertEquals(createdAt, updatedAt);
		assertSourceAlmostEquals(car, car1.source());

		// find by full text search
		vince.get("/1/search/car").refresh().queryParam("q", "inVENT*").go(200)//
				.assertEquals(carDO.id(), "results.0.id");

		// update
		vince.data().patch("car", carDO.id(), Json.object("color", "blue"));

		JsonDataObject car3 = vince.data().get("car", carDO.id());
		assertEquals(vince.id(), car3.owner());
		assertNotNull(car3.group());
		assertEquals(createdAt, car3.createdAt());

		updatedAt = assertDateIsRecent(car3.updatedAt());
		assertTrue(updatedAt.isAfter(createdAt));

		assertEquals("1234567890", car3.source().get("serialNumber").asText());
		assertEquals("blue", car3.source().get("color").asText());

		// delete
		vince.data().delete(carDO);
		vince.get("/1/data/car/" + carDO.id()).go(404);
	}

	@Test
	public void createFindUpdateAndDeleteWithSdk() {

		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");

		Schema carSchema = SchemaServiceTest.buildCarSchema()//
				.acl(Roles.user, Permission.create, Permission.updateMine, //
						Permission.readMine, Permission.deleteMine, Permission.search)//
				.build();
		superadmin.schemas().set(carSchema);

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
		JsonDataObject carbis = vince.data().get("car", id);
		assertEquals(vince.id(), carbis.owner());
		assertNotNull(carbis.group());
		assertNotNull(carbis.createdAt());
		assertSourceAlmostEquals(car, carbis.source());

		// find by full text search
		vince.get("/1/search/car").refresh().queryParam("q", "inVENT*").go(200)//
				.assertEquals(id, "results.0.id");

		// update
		vince.data().patch("car", id, Json.object("color", "blue"));
		carbis = vince.data().get("car", id);
		assertEquals("blue", carbis.source().get("color").asText());

		// delete
		vince.data().delete("car", id);
		try {
			vince.data().delete("car", id);
		} catch (SpaceRequestException e) {
			assertEquals(404, e.httpStatus());
		}
	}

	@Test
	public void createDataObjectWithCustomMeta() {

		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog nath = createTempDog(superadmin, "nath");
		SpaceDog operator = createTempDog(superadmin, "operator", "operator");

		// superadmin creates message schema
		Schema schema = Message.schema()//
				.acl(Roles.user, Permission.create, Permission.readMine)//
				.acl("operator", Permission.create, Permission.update, //
						Permission.updateMeta, Permission.read);
		superadmin.schemas().set(schema);

		// old message to insert again in database
		DateTime now = DateTime.now();
		Message message = new Message();
		message.text = "toto";
		message.owner(nath.id());
		message.group(nath.group());
		message.createdAt(now.minusDays(2));
		message.updatedAt(now.minusDays(2).plusHours(1));

		// vince creates a message object
		vince.data().save(Message.TYPE, "1", message);

		// but provided meta dates are not saved
		Message downloaded = vince.data()//
				.get(Message.TYPE, "1", MessageDataObject.class)//
				.source();

		assertEquals(message.text, downloaded.text);
		assertEquals(vince.id(), downloaded.owner());
		assertEquals(vince.group(), downloaded.group());
		assertTrue(downloaded.createdAt().isAfter(now.minusHours(1)));
		assertTrue(downloaded.updatedAt().isAfter(now.minusHours(1)));
		assertTrue(downloaded.createdAt().isEqual(downloaded.updatedAt()));

		// vince is not allowed to force save custom meta
		// since he does'nt have 'updateMeta' permission
		vince.put("/1/data/message/2")//
				.queryParam(FORCE_META_PARAM, true)//
				.bodyPojo(message)//
				.go(403);

		// operator can create a new message with custom metadata
		operator.put("/1/data/message/2")//
				.queryParam(FORCE_META_PARAM, true)//
				.bodyPojo(message)//
				.go(201);

		// provided custom meta are saved
		// and nath can access this object since the owner
		downloaded = nath.data()//
				.fetch(new MessageDataObject().id("2"))//
				.source();

		assertEquals(message.text, downloaded.text);
		assertEquals(nath.id(), downloaded.owner());
		assertEquals(nath.group(), downloaded.group());
		assertTrue(message.createdAt().isEqual(downloaded.createdAt()));
		assertTrue(message.updatedAt().isEqual(downloaded.updatedAt()));

		// operator is allowed to force update vince's object
		operator.put("/1/data/message/1")//
				.queryParam(FORCE_META_PARAM, true)//
				.bodyPojo(message)//
				.go(200);

		// vince can not access his object anymore
		// since owner has been updated to nath by operator
		vince.get("/1/data/message/1").go(403);
	}
}
