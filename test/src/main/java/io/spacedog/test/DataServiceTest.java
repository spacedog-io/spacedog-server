/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequestException;
import io.spacedog.model.JsonDataObject;
import io.spacedog.model.Permission;
import io.spacedog.model.Roles;
import io.spacedog.model.Schema;
import io.spacedog.utils.Json;

public class DataServiceTest extends SpaceTest {

	@Test
	public void createFindUpdateAndDelete() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
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
		SpaceDog superadmin = resetTestBackend();
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
}
