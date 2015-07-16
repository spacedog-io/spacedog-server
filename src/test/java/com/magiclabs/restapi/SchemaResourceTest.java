package com.magiclabs.restapi;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class SchemaResourceTest extends AbstractTest {

	@Test
	public void shouldDeletePutAndGetCarSchema() throws UnirestException {

		resetCarSchema();

		GetRequest req = Unirest.get("http://localhost:8080/v1/schema/car")
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test");

		JsonObject res = get(req, 200);
		assertTrue(Json.equals(buildCarSchema(), res));
	}

	public static void resetCarSchema() throws UnirestException {
		HttpRequestWithBody req1 = Unirest
				.delete("http://localhost:8080/v1/schema/car")
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test");

		delete(req1, 200, 404);

		RequestBodyEntity req2 = Unirest
				.post("http://localhost:8080/v1/schema/car")
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test")
				.body(buildCarSchema().toString());

		post(req2, 201);
	}

	public static JsonObject buildCarSchema() {
		return SchemaBuilder.builder("car") //
				.add("serialNumber", "string", true) //
				.add("buyDate", "date", true) //
				.add("buyTime", "time", true) //
				.add("buyTimestamp", "timestamp", true) //
				.add("color", "enum", true) //
				.add("techChecked", "boolean", true) //
				.startObject("model", true) //
				.add("description", "text", true) //
				.add("fiscalPower", "integer", true) //
				.add("size", "float", true) //
				.end() //
				.add("location", "geopoint", true) //
				.build();
	}

	@Test
	public void shouldDeletePutAndGetHomeSchema() throws UnirestException {

		resetHomeSchema();

		GetRequest req = Unirest.get("http://localhost:8080/v1/schema/home")
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test");

		JsonObject res = get(req, 200);
		assertTrue(Json.equals(buildHomeSchema(), res));
	}

	private static void resetHomeSchema() throws UnirestException {
		HttpRequestWithBody req1 = Unirest
				.delete("http://localhost:8080/v1/schema/home")
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test");

		delete(req1, 200);

		RequestBodyEntity req2 = Unirest
				.post("http://localhost:8080/v1/schema/home")
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test")
				.body(buildHomeSchema().toString());

		post(req2, 201);
	}

	private static JsonObject buildHomeSchema() {
		return SchemaBuilder.builder("home") //
				.add("type", "enum", true) //
				.startObject("address", true) //
				.add("number", "integer", false)//
				.add("street", "text", true) //
				.add("city", "string", true) //
				.add("country", "string", true) //
				.end() //
				.add("phone", "string", false)//
				.add("location", "geopoint", true) //
				.build();
	}

	@Test
	public void shouldGetAllSchemas() throws UnirestException {
		resetCarSchema();
		resetHomeSchema();

		GetRequest req = Unirest.get("http://localhost:8080/v1/schema")
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test");

		JsonObject result = get(req, 200);

		// user, car and home
		assertEquals(3, result.size());
		JsonObject expected = Json.merger() //
				.add(buildHomeSchema()) //
				.add(buildCarSchema()) //
				.add(UserResource.USER_DEFAULT_SCHEMA) //
				.get();

		assertTrue(Json.equals(expected, result));
	}

	@Test
	public void shouldFailDueToInvalidSchema() throws UnirestException {
		HttpResponse<String> response = shouldFailDueToInvalidSchema("{\"toto\":{\"_type\":\"XXX\"}}");
		assertEquals(400, response.getStatus());
	}

	private HttpResponse<String> shouldFailDueToInvalidSchema(String jsonSchema)
			throws UnirestException {
		HttpRequestWithBody req1 = Unirest
				.delete("http://localhost:8080/v1/schema/toto")
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test");

		delete(req1, 200);

		RequestBodyEntity request = Unirest
				.put("http://localhost:8080/v1/schema/toto")
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test")
				.body(jsonSchema);

		HttpResponse<String> response = request.asString();
		print(request, response);
		return response;
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
		RequestBodyEntity req2 = Unirest
				.put("http://localhost:8080/v1/schema/car")
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test")
				.body(json.toString());

		put(req2, 400);
	}

}
