/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceDogHelper.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.SchemaBuilder;
import io.spacedog.watchdog.SpaceSuite.TestAlways;

@TestAlways
public class SchemaResourceTest extends Assert {

	@Test
	public void deletePutAndGetSchemas() throws Exception {

		SpaceDogHelper.printTestHeader();
		Account testAccount = SpaceDogHelper.resetTestAccount();

		// should succeed to create a user

		User bob = SpaceDogHelper.createUser(testAccount, "bob", "hi bob", "bob@dog.com");

		// should succeed to reset all schemas
		SpaceDogHelper.resetSchema(buildCarSchema(), testAccount);
		SpaceDogHelper.resetSchema(buildHomeSchema(), testAccount);
		SpaceDogHelper.resetSchema(buildSaleSchema(), testAccount);

		// should succeed to get schemas with simple backend key credentials
		assertEquals(buildCarSchema(), //
				SpaceRequest.get("/v1/schema/car").backendKey(testAccount).go(200).jsonNode());
		assertEquals(buildHomeSchema(), //
				SpaceRequest.get("/v1/schema/home").backendKey(testAccount).go(200).jsonNode());
		assertEquals(buildSaleSchema(), //
				SpaceRequest.get("/v1/schema/sale").backendKey(testAccount).go(200).jsonNode());

		// should succeed to get all schemas with simple backend key credentials
		SpaceRequest.get("/v1/schema").basicAuth(testAccount).go(200)//
				.assertEquals(Json.merger() //
						.merge(buildHomeSchema()) //
						.merge(buildCarSchema()) //
						.merge(buildSaleSchema()) //
						.merge(UserResourceTest.getDefaultUserSchema()) //
						.get());

		// should fail to delete schema with simple backend key credentials
		SpaceRequest.delete("/v1/schema/toto").backendKey(testAccount).go(401);

		// should fail to delete schema with simple user credentials
		SpaceRequest.delete("/v1/schema/toto").basicAuth(bob).go(401);

		// should succeed to delete a non existent schema
		SpaceRequest.delete("/v1/schema/toto").basicAuth(testAccount).go(200);

		// should fail to create an invalid schema
		SpaceRequest.put("/v1/schema/toto").basicAuth(testAccount).body("{\"toto\":{\"_type\":\"XXX\"}}").go(400);

		// should fail to change the car schema
		ObjectNode json = buildCarSchema();
		json.with("car").with("color").put("_type", "date");
		SpaceRequest.put("/v1/schema/car").basicAuth(testAccount).body(json).go(400);
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
