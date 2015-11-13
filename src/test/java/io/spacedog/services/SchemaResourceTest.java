/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

import io.spacedog.services.AdminResourceTest.ClientAccount;

public class SchemaResourceTest extends AbstractTest {

	private static ClientAccount testAccount;

	@BeforeClass
	public static void resetTestAccount() throws UnirestException, InterruptedException, IOException {
		testAccount = AdminResourceTest.resetTestAccount();
	}

	@Test
	public void shouldDeletePutAndGetCarSchema() throws UnirestException, IOException {

		resetCarSchema();

		GetRequest req = prepareGet("/v1/schema/car", testAccount.backendKey);
		JsonNode res = get(req, 200).jsonNode();
		assertEquals(buildCarSchema(), res);
	}

	public static void resetCarSchema() throws UnirestException, IOException {
		resetSchema("car", buildCarSchema().toString(), "test", "hi test");
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

	public static void resetSaleSchema() throws UnirestException, IOException {
		resetSchema("sale", buildSaleSchema().toString(), "test", "hi test");
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
	public void shouldDeletePutAndGetHomeSchema() throws UnirestException, IOException {

		resetHomeSchema();

		GetRequest req = prepareGet("/v1/schema/home", testAccount.backendKey);
		JsonNode res = get(req, 200).jsonNode();
		assertEquals(buildHomeSchema(), res);
	}

	private static void resetHomeSchema() throws UnirestException, IOException {
		resetSchema("home", buildHomeSchema().toString(), "test", "hi test");
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
	public void shouldGetAllSchemas() throws UnirestException, IOException {
		resetCarSchema();
		resetHomeSchema();
		resetSaleSchema();

		GetRequest req = prepareGet("/v1/schema", testAccount.backendKey);
		JsonNode result = get(req, 200).jsonNode();

		// user, car, sale and home
		assertEquals(4, result.size());
		JsonNode expected = Json.merger() //
				.merge(buildHomeSchema()) //
				.merge(buildCarSchema()) //
				.merge(buildSaleSchema()) //
				.merge(UserResource.USER_DEFAULT_SCHEMA) //
				.get();

		assertEquals(expected, result);
	}

	@Test
	public void shouldFailDueToInvalidSchema() throws UnirestException, IOException {
		shouldFailDueToInvalidSchema("{\"toto\":{\"_type\":\"XXX\"}}");
	}

	private void shouldFailDueToInvalidSchema(String jsonSchema) throws UnirestException, IOException {
		HttpRequestWithBody req1 = prepareDelete("/v1/schema/toto").basicAuth("test", "hi test");
		delete(req1, 200);

		RequestBodyEntity req2 = preparePut("/v1/schema/toto").basicAuth("test", "hi test").body(jsonSchema);
		put(req2, 400);
	}

	@Test
	public void shouldFailToChangeCarSchema() throws UnirestException, IOException {

		resetCarSchema();

		ObjectNode json = buildCarSchema();
		json.with("car").with("color").put("_type", "date");
		shouldFailToChangeSchema(json);
	}

	private void shouldFailToChangeSchema(JsonNode json) throws UnirestException, IOException {
		RequestBodyEntity req2 = preparePut("/v1/schema/car").basicAuth("test", "hi test").body(json.toString());
		put(req2, 400);
	}

	public static void resetSchema(String schemaName, String schema, String username, String password)
			throws UnirestException, IOException {
		HttpRequestWithBody req1 = prepareDelete("/v1/schema/{name}").routeParam("name", schemaName).basicAuth(username,
				password);
		delete(req1, 200, 404);

		RequestBodyEntity req2 = preparePost("/v1/schema/{name}").routeParam("name", schemaName)
				.basicAuth(username, password).body(schema);
		post(req2, 201);
	}

}
