/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.services.AdminResourceTest.ClientAccount;

public class SchemaResourceTest extends Assert {

	private static ClientAccount testAccount;

	@BeforeClass
	public static void resetTestAccount() throws Exception {
		testAccount = AdminResourceTest.resetTestAccount();
	}

	@Test
	public void shouldDeletePutAndGetCarSchema() throws Exception {

		resetCarSchema();
		assertEquals(buildCarSchema(), //
				SpaceRequest.get("/v1/schema/car").backendKey(testAccount).go(200).jsonNode());
	}

	public static void resetCarSchema() throws Exception {
		resetSchema("car", buildCarSchema(), "test", "hi test");
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

	public static void resetSaleSchema() throws Exception {
		resetSchema("sale", buildSaleSchema(), "test", "hi test");
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

		resetHomeSchema();
		assertEquals(buildHomeSchema(), //
				SpaceRequest.get("/v1/schema/home").backendKey(testAccount).go(200).jsonNode());
	}

	private static void resetHomeSchema() throws Exception {
		resetSchema("home", buildHomeSchema(), "test", "hi test");
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
		resetCarSchema();
		resetHomeSchema();
		resetSaleSchema();

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

		resetCarSchema();

		ObjectNode json = buildCarSchema();
		json.with("car").with("color").put("_type", "date");
		shouldFailToChangeSchema(json);
	}

	private void shouldFailToChangeSchema(JsonNode json) throws Exception {
		SpaceRequest.put("/v1/schema/car").basicAuth(testAccount).body(json.toString()).go(400);
	}

	public static void resetSchema(String schemaName, JsonNode schema, ClientAccount account) throws Exception {
		resetSchema(schemaName, schema, account.username, account.password);
	}

	public static void resetSchema(String schemaName, JsonNode schema, String username, String password)
			throws Exception {

		SpaceRequest.delete("/v1/schema/{name}").routeParam("name", schemaName).basicAuth(username, password).go(200,
				404);

		SpaceRequest.post("/v1/schema/{name}").routeParam("name", schemaName).basicAuth(username, password).body(schema)
				.go(201);
	}

}
