package com.magiclabs.restapi;

import org.junit.BeforeClass;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class DataResourceTest extends AbstractTest {

	private static final String MAGIC_HOST = "localhost";

	@BeforeClass
	public static void resetTestAccount() throws UnirestException,
			InterruptedException {
		AccountResourceTest.resetTestAccount();
		SchemaResourceTest.resetCarSchema();
	}

	@Test
	public void shouldCreateFindUpdateAndDelete() throws Exception {

		JsonObject car = Json.builder() //
				.add("serialNumber", "1234567890") //
				.add("buyDate", "2015-01-09") //
				.add("buyTime", "15:37:00") //
				.add("buyTimestamp", "2015-01-09T15:37:00.123Z") //
				.add("color", "red") //
				.add("techChecked", false) //
				.stObj("model") //
				.add("description", "") //
				.add("fiscalPower", 8) //
				.add("size", 4.67) //
				.end() //
				.stObj("location") //
				.add("lat", -55.6765) //
				.add("lon", -54.6765) //
				.build();

		String id = checkCreate(car);
		checkFindById(car, id);
		checkUpdate(car, id);
		checkDeleteById(id);
	}

	private void checkDeleteById(String id) throws Exception {
		HttpRequestWithBody req = Unirest
				.delete("http://{host}:8080/v1/car/{id}")
				.routeParam("host", MAGIC_HOST).routeParam("id", id)
				.basicAuth("dave", "hi_dave")
				.header("x-magic-account-id", "test");

		delete(req, 200);
	}

	private String checkCreate(JsonObject car) throws Exception {
		RequestBodyEntity req = Unirest.post("http://{host}:8080/v1/car")
				.routeParam("host", MAGIC_HOST).basicAuth("dave", "hi_dave")
				.header("x-magic-account-id", "test").body(car.toString());

		JsonObject result = post(req, 201);

		assertEquals(true, result.get("done").asBoolean());
		assertEquals("car", result.get("type").asString());
		assertNotNull(result.get("id"));

		return result.get("id").asString();
	}

	private void checkFindById(JsonObject car, String id) throws Exception {
		GetRequest req = Unirest.get("http://{host}:8080/v1/car/{id}")
				.routeParam("host", MAGIC_HOST).routeParam("id", id)
				.basicAuth("dave", "hi_dave")
				.header("x-magic-account-id", "test");

		JsonObject res = get(req, 200);
		assertTrue(Json.equals(car, res));
	}

	private void checkUpdate(JsonValue car, String id) throws UnirestException {
		RequestBodyEntity req1 = Unirest.put("http://{host}:8080/v1/car/{id}")
				.routeParam("host", MAGIC_HOST).routeParam("id", id)
				.basicAuth("dave", "hi_dave")
				.header("x-magic-account-id", "test")
				.body(new JsonObject().add("color", "blue").toString());

		put(req1, 200);

		GetRequest req2 = Unirest.get("http://{host}:8080/v1/car/{id}")
				.routeParam("host", MAGIC_HOST).routeParam("id", id)
				.basicAuth("dave", "hi_dave")
				.header("x-magic-account-id", "test");

		JsonObject res2 = get(req2, 200);
		assertEquals("1234567890", res2.get("serialNumber").asString());
		assertEquals("blue", res2.get("color").asString());
	}
}
