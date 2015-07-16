package com.magiclabs.restapi;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;
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

		// check create

		RequestBodyEntity req = Unirest.post("http://{host}:8080/v1/data/car")
				.routeParam("host", MAGIC_HOST).basicAuth("dave", "hi_dave")
				.header("x-magic-app-id", "test").body(car.toString());

		DateTime beforeCreate = DateTime.now();
		JsonObject result = post(req, 201);

		assertEquals(true, result.get("success").asBoolean());
		assertEquals("car", result.get("type").asString());
		assertNotNull(result.get("id"));

		String id = result.get("id").asString();

		// check find by id

		GetRequest req1 = Unirest.get("http://{host}:8080/v1/data/car/{id}")
				.routeParam("host", MAGIC_HOST).routeParam("id", id)
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test");

		JsonObject res1 = get(req1, 200);

		JsonObject meta1 = res1.get("meta").asObject();
		assertEquals("dave", meta1.get("createdBy").asString());
		assertEquals("dave", meta1.get("updatedBy").asString());
		DateTime createdAt = DateTime.parse(meta1.get("createdAt").asString());
		assertTrue(createdAt.isAfter(beforeCreate.getMillis()));
		assertTrue(createdAt.isBeforeNow());
		assertEquals(meta1.get("updatedAt"), meta1.get("createdAt"));
		assertTrue(Json.equals(car, res1.remove("meta")));

		// create user vince

		RequestBodyEntity req1a = Unirest
				.post("http://localhost:8080/v1/user/")
				.basicAuth("dave", "hi_dave")
				.header("x-magic-app-id", "test")
				.body(Json.builder().add("username", "vince")
						.add("password", "hi_vince")
						.add("email", "vince@magic.com").build().toString());

		post(req1a, 201);

		refreshIndex("test");

		// update

		RequestBodyEntity req2 = Unirest
				.put("http://{host}:8080/v1/data/car/{id}")
				.routeParam("host", MAGIC_HOST).routeParam("id", id)
				.basicAuth("vince", "hi_vince")
				.header("x-magic-app-id", "test")
				.body(new JsonObject().add("color", "blue").toString());

		DateTime beforeUpdate = DateTime.now();
		put(req2, 200);

		// check update is correct

		GetRequest req3 = Unirest.get("http://{host}:8080/v1/data/car/{id}")
				.routeParam("host", MAGIC_HOST).routeParam("id", id)
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test");

		JsonObject res3 = get(req3, 200);

		JsonObject meta3 = res3.get("meta").asObject();
		assertEquals("dave", meta3.get("createdBy").asString());
		assertEquals("vince", meta3.get("updatedBy").asString());
		DateTime createdAtAfterUpdate = DateTime.parse(meta3.get("createdAt")
				.asString());
		assertEquals(createdAt, createdAtAfterUpdate);
		DateTime updatedAt = DateTime.parse(meta3.get("updatedAt").asString());
		assertTrue(updatedAt.isAfter(beforeUpdate.getMillis()));
		assertTrue(updatedAt.isBeforeNow());
		assertEquals("1234567890", res3.get("serialNumber").asString());
		assertEquals("blue", res3.get("color").asString());

		// delete

		HttpRequestWithBody req4 = Unirest
				.delete("http://{host}:8080/v1/data/car/{id}")
				.routeParam("host", MAGIC_HOST).routeParam("id", id)
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test");

		delete(req4, 200);

		// check delete is done

		JsonObject res5 = get(req1, 404);
		assertFalse(res5.get("success").asBoolean());
	}
}
