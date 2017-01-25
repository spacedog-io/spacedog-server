/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTest;
import io.spacedog.utils.Json;
import io.spacedog.utils.Schema;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class SchemaResourceTestOften extends SpaceTest {

	@Test
	public void deletePutAndGetSchemas() {

		// prepare
		prepareTest();
		Backend test = resetTestBackend();

		// anonymous gets all backend schema
		// if no schema returns empty object
		SpaceRequest.get("/1/schema").backend(test).go(200)//
				.assertEquals(Json.merger().get());

		// bob signs up
		User bob = signUp(test, "bob", "hi bob", "bob@dog.com");

		// admin creates car, home and sale schemas
		setSchema(buildCarSchema(), test);
		setSchema(buildHomeSchema(), test);
		setSchema(buildSaleSchema(), test);

		// anonymous gets car, home and sale schemas
		SpaceRequest.get("/1/schema/car").backend(test).go(200)//
				.assertEquals(buildCarSchema().node());
		SpaceRequest.get("/1/schema/home").backend(test).go(200)//
				.assertEquals(buildHomeSchema().node());
		SpaceRequest.get("/1/schema/sale").backend(test).go(200)//
				.assertEquals(buildSaleSchema().node());

		// anonymous gets all schemas
		SpaceRequest.get("/1/schema").backend(test).go(200)//
				.assertEquals(Json.merger() //
						.merge(buildHomeSchema().node()) //
						.merge(buildCarSchema().node()) //
						.merge(buildSaleSchema().node()) //
						.get());

		// anonymous is not allowed to delete schema
		SpaceRequest.delete("/1/schema/sale").backend(test).go(403);

		// user is not allowed to delete schema
		SpaceRequest.delete("/1/schema/sale").userAuth(bob).go(403);

		// admin fails to delete a non existing schema
		SpaceRequest.delete("/1/schema/XXX").adminAuth(test).go(404);

		// admin deletes a schema and all its objects
		SpaceRequest.delete("/1/schema/sale").adminAuth(test).go(200);

		// admin fails to create an invalid schema
		SpaceRequest.put("/1/schema/toto").adminAuth(test)//
				.body("{\"toto\":{\"_type\":\"XXX\"}}").go(400);

		// admin fails to update car schema color property type
		Schema carSchema = buildCarSchema();
		carSchema.node().with("car").with("color").put("_type", "date");
		SpaceRequest.put("/1/schema/car").adminAuth(test).bodySchema(carSchema).go(400);

		// fails to remove the car schema color property
		// json = buildCarSchema();
		// json.with("car").remove("color");
		// SpaceRequest.put("/1/schema/car").adminAuth(testBackend).body(json).go(400);
	}

	private static Schema buildHomeSchema() {
		return Schema.builder("home") //
				.enumm("type")//
				.string("phone")//
				.geopoint("location")//

				.object("address") //
				.integer("number")//
				.text("street")//
				.string("city")//
				.string("country")//
				.close() //

				.build();
	}

	public static Schema buildSaleSchema() {
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
				.close() //

				.build();
	}

	public static Schema buildCarSchema() {
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
				.close() //

				.build();
	}

	@Test
	public void saveCustomerExtraDataInSchema() {

		// prepare
		prepareTest();
		Backend test = resetTestBackend();

		Schema schema = Schema.builder("home") //
				.extra("global-scope", true)//
				.enumm("type").required().extra("global-scope", false)//
				.object("address").required().extra("global-scope", false)//
				.text("street").required().extra("global-scope", false) //
				.build();

		setSchema(schema, test);

		SpaceRequest.get("/1/schema/home").backend(test).go(200)//
				.assertEquals(schema.node());
	}

	@Test
	public void schemaNameSettingsIsReserved() {

		// prepare
		prepareTest();
		Backend test = resetTestBackend();

		// settings is a reserved schema name
		SpaceRequest.get("/1/schema/settings").backend(test).go(400);
		SpaceRequest.put("/1/schema/settings").adminAuth(test).go(400);
	}

}
