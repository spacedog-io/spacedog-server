/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceRequest;

public class SchemaResourceTest extends Assert {

	private static SpaceDogHelper.Account testAccount;

	@BeforeClass
	public static void resetTestAccount() throws Exception {
		testAccount = SpaceDogHelper.resetTestAccount();
	}

	@Test
	public void shouldDeletePutAndGetCarSchema() throws Exception {

		SpaceDogHelper.resetSchema(buildCarSchema(), testAccount);
		assertEquals(buildCarSchema(), //
				SpaceRequest.get("/v1/schema/car").backendKey(testAccount).go(200).jsonNode());
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

	@Test
	public void shouldDeletePutAndGetHomeSchema() throws Exception {

		SpaceDogHelper.resetSchema(buildHomeSchema(), testAccount);
		assertEquals(buildHomeSchema(), //
				SpaceRequest.get("/v1/schema/home").backendKey(testAccount).go(200).jsonNode());
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

	@Test
	public void shouldGetAllSchemas() throws Exception {
		SpaceDogHelper.resetSchema(buildCarSchema(), testAccount);
		SpaceDogHelper.resetSchema(buildHomeSchema(), testAccount);
		SpaceDogHelper.resetSchema(buildSaleSchema(), testAccount);

		JsonNode result = SpaceRequest.get("/v1/schema").backendKey(testAccount).go(200).jsonNode();

		// user, car, sale and home
		assertEquals(4, result.size());
		JsonNode expected = Json.merger() //
				.merge(buildHomeSchema()) //
				.merge(buildCarSchema()) //
				.merge(buildSaleSchema()) //
				.merge(UserResource.getDefaultUserSchema()) //
				.get();

		assertEquals(expected, result);
	}

	@Test
	public void shouldFailDueToInvalidSchema() throws Exception {
		shouldFailDueToInvalidSchema("{\"toto\":{\"_type\":\"XXX\"}}");
	}

	private void shouldFailDueToInvalidSchema(String jsonSchema) throws Exception {

		SpaceRequest.delete("/v1/schema/toto").basicAuth(testAccount).go(200);
		SpaceRequest.put("/v1/schema/toto").basicAuth(testAccount).body(jsonSchema).go(400);
	}

	@Test
	public void shouldFailToChangeCarSchema() throws Exception {

		SpaceDogHelper.resetSchema(buildCarSchema(), testAccount);

		ObjectNode json = buildCarSchema();
		json.with("car").with("color").put("_type", "date");
		SpaceRequest.put("/v1/schema/car").basicAuth(testAccount).body(json).go(400);
	}

}
