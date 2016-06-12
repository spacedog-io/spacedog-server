/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder;
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

		// admin creates user default schema
		SpaceClient.initUserDefaultSchema(test);

		// bob signs up
		User bob = SpaceClient.createUser(test, "bob", "hi bob", "bob@dog.com");

		// admin creates car, home and sale schemas
		SpaceClient.setSchema(buildCarSchema(), test);
		SpaceClient.setSchema(buildHomeSchema(), test);
		SpaceClient.setSchema(buildSaleSchema(), test);

		// anonymous gets car, home and sale schemas
		SpaceRequest.get("/1/schema/car").backend(test).go(200)//
				.assertEquals(buildCarSchema());
		SpaceRequest.get("/1/schema/home").backend(test).go(200)//
				.assertEquals(buildHomeSchema());
		SpaceRequest.get("/1/schema/sale").backend(test).go(200)//
				.assertEquals(buildSaleSchema());

		// admin gets the default user schema
		ObjectNode userSchema = SpaceRequest.get("/1/schema/user")//
				.adminAuth(test).go(200).objectNode();

		// anonymous gets all schemas
		SpaceRequest.get("/1/schema").backend(test).go(200)//
				.assertEquals(Json.merger() //
						.merge(buildHomeSchema()) //
						.merge(buildCarSchema()) //
						.merge(buildSaleSchema()) //
						.merge(userSchema) //
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
		ObjectNode json = buildCarSchema();
		json.with("car").with("color").put("_type", "date");
		SpaceRequest.put("/1/schema/car").adminAuth(test).body(json).go(400);

		// fails to remove the car schema color property
		// json = buildCarSchema();
		// json.with("car").remove("color");
		// SpaceRequest.put("/1/schema/car").adminAuth(testBackend).body(json).go(400);
	}

	private static ObjectNode buildHomeSchema() {
		return SchemaBuilder.builder("home") //
				.property("type", "enum").required().end() //
				.objectProperty("address").required() //
				.property("number", "integer").end() //
				.property("street", "text").required().end() //
				.property("city", "string").required().end() //
				.property("country", "string").required().end() // /
				.end() //
				.property("phone", "string").end() //
				.property("location", "geopoint").required().end() //
				.build();
	}

	public static ObjectNode buildSaleSchema() {
		return SchemaBuilder.builder("sale") //
				.property("number", "string").required().end() //
				.property("when", "timestamp").required().end() //
				.property("where", "geopoint").end() //
				.property("online", "boolean").required().end() //
				.property("deliveryDate", "date").required().end() //
				.property("deliveryTime", "time").required().end() //
				.objectProperty("items").array().required() //
				.property("ref", "string").required().end() //
				.property("description", "text").language("english").required().end() //
				.property("quantity", "integer").end() //
				// .property("price", "amount").required().end() //
				.property("type", "enum").required().end() //
				.end() //
				.build();
	}

	public static ObjectNode buildCarSchema() {
		return SchemaBuilder.builder("car") //
				.property("serialNumber", "string").required().end() //
				.property("buyDate", "date").required().end() //
				.property("buyTime", "time").required().end() //
				.property("buyTimestamp", "timestamp").required().end() //
				.property("color", "enum").required().end() //
				.property("techChecked", "boolean").required().end() //
				.objectProperty("model").required() //
				.property("description", "text").language("french").required().end() //
				.property("fiscalPower", "integer").required().end() //
				.property("size", "float").required().end() //
				.end() //
				.property("location", "geopoint").required().end() //
				.build();
	}

}
