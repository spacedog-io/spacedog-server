/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

import io.spacedog.services.AdminResourceTest.ClientAccount;

public class DataResourceTest extends AbstractTest {

	private static ClientAccount testAccount;

	@BeforeClass
	public static void resetTestAccount() throws UnirestException, InterruptedException, IOException {
		testAccount = AdminResourceTest.resetTestAccount();
		SchemaResourceTest.resetCarSchema();
	}

	@Test
	public void shouldCreateFindUpdateAndDelete() throws Exception {

		JsonNode car = Json.startObject() //
				.put("serialNumber", "1234567890") //
				.put("buyDate", "2015-01-09") //
				.put("buyTime", "15:37:00") //
				.put("buyTimestamp", "2015-01-09T15:37:00.123Z") //
				.put("color", "red") //
				.put("techChecked", false) //
				.startObject("model") //
				.put("description", "Cette voiture sent bon la France. Elle est inventive et raffinée.") //
				.put("fiscalPower", 8) //
				.put("size", 4.67) //
				.end().startObject("location") //
				.put("lat", -55.6765) //
				.put("lon", -54.6765) //
				.build();

		// create

		RequestBodyEntity req = preparePost("/v1/data/car", testAccount.backendKey).body(car.toString());

		DateTime beforeCreate = DateTime.now();
		JsonNode result = post(req, 201).jsonNode();

		assertEquals(true, result.get("success").asBoolean());
		assertEquals("car", result.get("type").asText());
		assertNotNull(result.get("id"));

		String id = result.get("id").asText();

		refreshIndex("test");

		// find by id

		GetRequest req1 = prepareGet("/v1/data/car/{id}", testAccount.backendKey).routeParam("id", id);
		ObjectNode res1 = (ObjectNode) get(req1, 200).jsonNode();

		assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, res1.findValue("createdBy").asText());
		assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, res1.findValue("updatedBy").asText());
		DateTime createdAt = DateTime.parse(res1.findValue("createdAt").asText());
		assertTrue(createdAt.isAfter(beforeCreate.getMillis()));
		assertTrue(createdAt.isBeforeNow());
		assertEquals(res1.findValue("updatedAt"), res1.findValue("createdAt"));

		res1.remove("meta");
		assertTrue(car.equals(res1));

		// find by full text search

		GetRequest req1b = prepareGet("/v1/data/car?q={q}", testAccount.backendKey).routeParam("q", "inVENt*");

		JsonNode res1b = get(req1b, 200).jsonNode();
		assertEquals(id, res1b.get("results").get(0).get("meta").get("id").asText());

		// create user vince

		RequestBodyEntity req1a = preparePost("/v1/user/", testAccount.backendKey).body(Json.startObject()
				.put("username", "vince").put("password", "hi vince").put("email", "vince@spacedog.io").toString());

		post(req1a, 201);
		refreshIndex("test");

		// update

		RequestBodyEntity req2 = preparePut("/v1/data/car/{id}", testAccount.backendKey).routeParam("id", id)
				.basicAuth("vince", "hi vince").body(Json.startObject().put("color", "blue").toString());

		DateTime beforeUpdate = DateTime.now();
		put(req2, 200);

		// check update is correct

		GetRequest req3 = prepareGet("/v1/data/car/{id}", testAccount.backendKey).routeParam("id", id);
		JsonNode res3 = get(req3, 200).jsonNode();

		assertEquals(BackendKey.DEFAULT_BACKEND_KEY_NAME, res3.findValue("createdBy").asText());
		assertEquals("vince", res3.findValue("updatedBy").asText());
		DateTime createdAtAfterUpdate = DateTime.parse(res3.findValue("createdAt").asText());
		assertEquals(createdAt, createdAtAfterUpdate);
		DateTime updatedAt = DateTime.parse(res3.findValue("updatedAt").asText());
		assertTrue(updatedAt.isAfter(beforeUpdate.getMillis()));
		assertTrue(updatedAt.isBeforeNow());
		assertEquals("1234567890", res3.get("serialNumber").asText());
		assertEquals("blue", res3.get("color").asText());

		// delete

		HttpRequestWithBody req4 = prepareDelete("/v1/data/car/{id}", testAccount.backendKey).routeParam("id", id);
		delete(req4, 200);

		// check delete is done

		JsonNode res5 = get(req1, 404).jsonNode();
		assertFalse(res5.get("success").asBoolean());
	}
}
