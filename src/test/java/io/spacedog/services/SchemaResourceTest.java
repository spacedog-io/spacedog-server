package io.spacedog.services;

import org.junit.BeforeClass;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class SchemaResourceTest extends AbstractTest {

	@BeforeClass
	public static void resetTestAccount() throws UnirestException,
			InterruptedException {
		AccountResourceTest.resetTestAccount();
	}

	@Test
	public void shouldDeletePutAndGetCarSchema() throws UnirestException {

		resetCarSchema();

		GetRequest req = prepareGet("/v1/schema/car",
				AccountResourceTest.testKey());
		JsonObject res = get(req, 200).json();
		assertTrue(Json.equals(buildCarSchema(), res));
	}

	public static void resetCarSchema() throws UnirestException {

		HttpRequestWithBody req1 = prepareDelete("/v1/schema/car",
				AccountResourceTest.testKey());
		delete(req1, 200, 404);

		RequestBodyEntity req2 = preparePost("/v1/schema/car",
				AccountResourceTest.testKey())
				.body(buildCarSchema().toString());
		post(req2, 201);
	}

	public static JsonObject buildCarSchema() {
		return SchemaBuilder.builder("car") //
				.add("serialNumber", "string").required() //
				.add("buyDate", "date").required() //
				.add("buyTime", "time").required() //
				.add("buyTimestamp", "timestamp").required() //
				.add("color", "enum").required() //
				.add("techChecked", "boolean").required() //
				.startObject("model").required() //
				.add("description", "text").language("french").required() //
				.add("fiscalPower", "integer").required() //
				.add("size", "float").required() //
				.end() //
				.add("location", "geopoint").required() //
				.build();
	}

	public static void resetSaleSchema() throws UnirestException {
		HttpRequestWithBody req1 = prepareDelete("/v1/schema/sale",
				AccountResourceTest.testKey());
		delete(req1, 200, 404);

		RequestBodyEntity req2 = preparePost("/v1/schema/sale",
				AccountResourceTest.testKey()).body(
				buildSaleSchema().toString());
		post(req2, 201);
	}

	public static JsonObject buildSaleSchema() {
		return SchemaBuilder.builder("sale") //
				.add("number", "string").required() //
				.add("when", "timestamp").required() //
				.add("where", "geopoint") //
				.add("online", "boolean").required() //
				.add("deliveryDate", "date").required() //
				.add("deliveryTime", "time").required() //
				.startObject("items").required() //
				.array() //
				.add("ref", "string").required() //
				.add("description", "text").required() //
				.language("english") //
				.add("quantity", "integer") //
				// .add("price", "amount").required() //
				.add("type", "enum").required() //
				.end() //
				.build();
	}

	@Test
	public void shouldDeletePutAndGetHomeSchema() throws UnirestException {

		resetHomeSchema();

		GetRequest req = prepareGet("/v1/schema/home",
				AccountResourceTest.testKey());
		JsonObject res = get(req, 200).json();
		assertTrue(Json.equals(buildHomeSchema(), res));
	}

	private static void resetHomeSchema() throws UnirestException {
		HttpRequestWithBody req1 = prepareDelete("/v1/schema/home",
				AccountResourceTest.testKey());
		delete(req1, 200);

		RequestBodyEntity req2 = preparePost("/v1/schema/home",
				AccountResourceTest.testKey()).body(
				buildHomeSchema().toString());
		post(req2, 201);
	}

	private static JsonObject buildHomeSchema() {
		return SchemaBuilder.builder("home") //
				.add("type", "enum").required() //
				.startObject("address").required() //
				.add("number", "integer")//
				.add("street", "text").required() //
				.add("city", "string").required() //
				.add("country", "string").required() // /
				.end() //
				.add("phone", "string")//
				.add("location", "geopoint").required() //
				.build();
	}

	@Test
	public void shouldGetAllSchemas() throws UnirestException {
		resetCarSchema();
		resetHomeSchema();
		resetSaleSchema();

		GetRequest req = prepareGet("/v1/schema", AccountResourceTest.testKey());
		JsonObject result = get(req, 200).json();

		// user, car, sale and home
		assertEquals(4, result.size());
		JsonObject expected = Json.merger() //
				.add(buildHomeSchema()) //
				.add(buildCarSchema()) //
				.add(buildSaleSchema()) //
				.add(UserResource.USER_DEFAULT_SCHEMA) //
				.get();

		assertTrue(Json.equals(expected, result));
	}

	@Test
	public void shouldFailDueToInvalidSchema() throws UnirestException {
		shouldFailDueToInvalidSchema("{\"toto\":{\"_type\":\"XXX\"}}");
	}

	private void shouldFailDueToInvalidSchema(String jsonSchema)
			throws UnirestException {
		HttpRequestWithBody req1 = prepareDelete("/v1/schema/toto",
				AccountResourceTest.testKey());
		delete(req1, 200);

		RequestBodyEntity req2 = preparePut("/v1/schema/toto",
				AccountResourceTest.testKey()).body(jsonSchema);
		put(req2, 400);
	}

	@Test
	public void shouldFailToChangeCarSchema() throws UnirestException {

		resetCarSchema();

		JsonObject json = buildCarSchema();
		json.get("car").asObject().get("color").asObject().set("_type", "date");
		shouldFailToChangeSchema(json);
	}

	private void shouldFailToChangeSchema(JsonObject json)
			throws UnirestException {
		RequestBodyEntity req2 = preparePut("/v1/schema/car",
				AccountResourceTest.testKey()).body(json.toString());
		put(req2, 400);
	}

}
