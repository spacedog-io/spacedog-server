/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.Schema;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class SchemaResourceTestOften extends Assert {

	@Test
	public void deletePutAndGetSchemas() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// anonymous gets all backend schema
		// if no schema returns empty object
		SpaceRequest.get("/1/schema").backend(test).go(200)//
				.assertEquals(Json.merger().get());

		// bob signs up
		User bob = SpaceClient.newCredentials(test, "bob", "hi bob", "bob@dog.com");

		// admin creates car, home and sale schemas
		SpaceClient.setSchema(buildCarSchema(), test);
		SpaceClient.setSchema(buildHomeSchema(), test);
		SpaceClient.setSchema(buildSaleSchema(), test);

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
		SpaceRequest.delete("/1/schema/sale").backend(test).go(401);

		// user is not allowed to delete schema
		SpaceRequest.delete("/1/schema/sale").userAuth(bob).go(401);

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
		SpaceRequest.put("/1/schema/car").adminAuth(test).body(carSchema).go(400);

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
	public void saveCustomerExtraDataInSchema() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		Schema schema = Schema.builder("home") //
				.extra("global-scope", true)//
				.enumm("type").required().extra("global-scope", false)//
				.object("address").required().extra("global-scope", false)//
				.text("street").required().extra("global-scope", false) //
				.build();

		SpaceClient.setSchema(schema, test);

		SpaceRequest.get("/1/schema/home").backend(test).go(200)//
				.assertEquals(schema.node());
	}

}
