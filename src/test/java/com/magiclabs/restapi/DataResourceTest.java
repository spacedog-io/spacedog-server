package com.magiclabs.restapi;

import org.elasticsearch.common.base.Charsets;
import org.junit.BeforeClass;
import org.junit.Test;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.common.io.Resources;
import com.mashape.unirest.http.HttpResponse;
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
	}

	@Test
	public void shouldCreateFindUpdateAndDelete() throws Exception {

		JsonArray contacts = JsonArray.readFrom(loadDataSet());
		for (JsonValue contact : contacts) {
			String magicId = checkCreate(contact.asObject());
			checkFindById(contact.asObject(), magicId);
			checkUpdate(contact.asObject(), magicId);
			checkDeleteById(magicId);
		}
	}

	private void checkDeleteById(String contactId) throws Exception {
		HttpRequestWithBody req = Unirest
				.delete("http://{host}:8080/v1/contact/{id}")
				.routeParam("host", MAGIC_HOST).routeParam("id", contactId)
				.basicAuth("dave", "hi_dave")
				.header("x-magic-account-id", "test");

		delete(req, 200);
	}

	private String checkCreate(JsonObject contact) throws Exception {
		RequestBodyEntity req = Unirest.post("http://{host}:8080/v1/contact")
				.routeParam("host", MAGIC_HOST).basicAuth("dave", "hi_dave")
				.header("x-magic-account-id", "test").body(contact.toString());

		HttpResponse<String> resp = req.asString();
		AbstractTest.print(req, resp);
		assertEquals(201, resp.getStatus());

		String magicId = resp.getHeaders().getFirst("x-magiclabs-object-id");
		assertNotNull(resp.getHeaders().getFirst("x-magiclabs-object-id"));
		return magicId;
	}

	private void checkFindById(JsonObject contact, String contactId)
			throws Exception {
		GetRequest req = Unirest.get("http://{host}:8080/v1/contact/{id}")
				.routeParam("host", MAGIC_HOST).routeParam("id", contactId)
				.basicAuth("dave", "hi_dave")
				.header("x-magic-account-id", "test");

		JsonObject res = get(req, 200);
		assertTrue(Json.equals(contact, res));
	}

	private void checkUpdate(JsonValue contact, String contactId)
			throws UnirestException {
		RequestBodyEntity req = Unirest
				.put("http://{host}:8080/v1/contact/{id}")
				.routeParam("host", MAGIC_HOST).routeParam("id", contactId)
				.basicAuth("dave", "hi_dave")
				.header("x-magic-account-id", "test")
				.body(new JsonObject().add("eyeColor", "red").toString());

		exec(req.getHttpRequest(), req, 200);

		GetRequest req2 = Unirest.get("http://{host}:8080/v1/contact/{id}")
				.routeParam("host", MAGIC_HOST).routeParam("id", contactId)
				.basicAuth("dave", "hi_dave")
				.header("x-magic-account-id", "test");

		JsonObject res2 = get(req2, 200);
		assertEquals("red", res2.get("eyeColor").asString());
	}

	private String loadDataSet() throws Exception {
		return Resources.toString(
				Resources.getResource(getClass(), "dataset.json"),
				Charsets.UTF_8);
	}
}
