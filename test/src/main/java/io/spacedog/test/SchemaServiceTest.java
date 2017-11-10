/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.model.Permission;
import io.spacedog.model.Schema;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder;

public class SchemaServiceTest extends SpaceTest {

	@Test
	public void deletePutAndGetSchemas() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin.backend());
		SpaceDog bob = createTempDog(superadmin, "bob");

		// anonymous gets all backend schema
		// if no schema returns empty object
		guest.schema().getAll().isEmpty();

		// admin creates car, home and sale schemas
		Schema carSchema = buildCarSchema().build();
		superadmin.schema().set(carSchema);
		Schema homeSchema = buildHomeSchema().build();
		superadmin.schema().set(homeSchema);
		Schema saleSchema = buildSaleSchema().build();
		superadmin.schema().set(saleSchema);

		// anonymous gets car, home and sale schemas
		assertEquals(carSchema, guest.schema().get(carSchema.name()));
		assertEquals(homeSchema, guest.schema().get(homeSchema.name()));
		assertEquals(saleSchema, guest.schema().get(saleSchema.name()));

		// anonymous gets all schemas
		assertEquals(Sets.newHashSet(carSchema, homeSchema, saleSchema), //
				guest.schema().getAll());

		// anonymous is not allowed to delete schema
		guest.delete("/1/schemas/sale").go(403);

		// user is not allowed to delete schema
		bob.delete("/1/schemas/sale").go(403);

		// admin fails to delete a non existing schema
		superadmin.delete("/1/schemas/XXX").go(404);

		// admin deletes a schema and all its objects
		superadmin.delete(saleSchema.name());

		// admin fails to create an invalid schema
		superadmin.put("/1/schemas/toto")//
				.bodyString("{\"toto\":{\"_type\":\"XXX\"}}").go(400);

		// admin fails to update car schema color property type
		carSchema.node().with("car").with("color").put("_type", "date");
		superadmin.put("/1/schemas/car").bodySchema(carSchema).go(400);

		// fails to remove the car schema color property
		// json = buildCarSchema();
		// json.with("car").remove("color");
		// SpaceRequest.put("/1/schema/car").adminAuth(testBackend).body(json).go(400);
	}

	private static SchemaBuilder buildHomeSchema() {
		return Schema.builder("home") //
				.enumm("type")//
				.string("phone")//
				.geopoint("location")//

				.object("address") //
				.integer("number")//
				.text("street")//
				.string("city")//
				.string("country")//
				.close();

	}

	public static SchemaBuilder buildSaleSchema() {
		return Schema.builder("sale") //
				.string("number") //
				.timestamp("when") //
				.geopoint("where") //
				.bool("online")//
				.date("deliveryDate") //
				.time("deliveryTime")//

				.object("items").array() //
				.string("ref")//
				.text("description").english()//
				.integer("quantity")//
				.enumm("type")//
				.close();
	}

	public static SchemaBuilder buildCarSchema() {
		return Schema.builder("car") //
				.string("serialNumber")//
				.date("buyDate")//
				.time("buyTime")//
				.timestamp("buyTimestamp") //
				.enumm("color")//
				.bool("techChecked") //
				.geopoint("location") //

				.object("model")//
				.text("description").french()//
				.integer("fiscalPower")//
				.floatt("size")//
				.close();
	}

	@Test
	public void saveMetaDataInSchema() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		Schema schemaClient = Schema.builder("home") //
				.extra("global-scope", true)//
				.enumm("type").required().extra("global-scope", false)//
				.object("address").required().extra("global-scope", false)//
				.text("street").required().extra("global-scope", false) //
				.string("owner").required().refType("user")//
				.build();

		superadmin.schema().set(schemaClient);

		// superadmin gets the schema from backend
		// and check it is unchanged
		Schema schemaServer = superadmin.schema().get(schemaClient.name());
		assertEquals(schemaClient, schemaServer);
	}

	@Test
	public void schemaNameSettingsIsReserved() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin.backend());

		// settings is a reserved schema name
		guest.get("/1/schemas/settings").go(400);
		superadmin.put("/1/schemas/settings").go(400);
	}

	@Test
	public void testStashField() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin.backend());

		// superadmin creates document schema
		Schema schema = Schema.builder("document").stash("data")//
				.acl("all", Permission.create).build();
		superadmin.schema().set(schema);

		// guest saves a first document with data as an object
		guest.data().save("document", Json.object("data", //
				Json.object("a", "aaa", "b", Json.object("b", "bbb"))));

		// guest saves a second document with data as an array
		guest.data().save("document", Json.object("data", //
				Json.array("a", Json.array(1, 2, 3))));

		// guest saves a thirst document with data as a boolean
		guest.data().save("document", Json.object("data", true));

		// document schema contains no fields but "data" stash field
		guest.post("/1/data/document").bodyJson("a", "aaa").go(400);
	}
}
