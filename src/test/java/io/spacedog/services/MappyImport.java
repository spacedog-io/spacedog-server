/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class MappyImport extends AbstractTest {

	private static String examplesKey;

	public static void resetExamplesAccount() throws UnirestException, IOException {

		HttpRequestWithBody req1 = prepareDelete("/v1/admin/account/examples").basicAuth("examples", "hi examples");
		delete(req1, 200, 401);

		refreshIndex("examples");
		refreshIndex(AdminResource.ADMIN_INDEX);

		RequestBodyEntity req2 = preparePost("/v1/admin/account/")
				.body(Json.startObject().put("backendId", "examples").put("username", "examples")
						.put("password", "hi examples").put("email", "hello@spacedog.io").toString());

		examplesKey = post(req2, 201).response().getHeaders().get(AdminResource.BACKEND_KEY_HEADER).get(0);

		assertFalse(Strings.isNullOrEmpty(examplesKey));

		refreshIndex(AdminResource.ADMIN_INDEX);
		refreshIndex("examples");
	}

	public static void main(String[] args) {
		try {
			resetExamplesAccount();

			HttpRequestWithBody req2 = prepareDelete("/v1/schema/resto").basicAuth("examples", "hi examples");
			delete(req2, 200, 404);

			RequestBodyEntity req3 = preparePost("/v1/schema/resto").basicAuth("examples", "hi examples")
					.body(buildRestoSchema().toString());

			post(req3, 201);

			for (double lat = 48.5; lat <= 49; lat += 0.1) {
				for (double lon = 1.8; lon <= 2.9; lon += 0.1) {

					HttpRequest req1 = Unirest.get("http://search.mappy.net/search/1.0/find")
							.queryString("max_results", "200").queryString("q", "restaurant")
							.queryString("bbox", "" + lat + ',' + lon + ',' + (lat + 0.1) + ',' + (lon + 0.1));

					// "48.671228,1.854415,49.034931,2.843185");

					ObjectNode res1 = get(req1, 200).objectNode();

					res1.get("pois").forEach(MappyImport::copyPoi);
				}
			}

		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	private static void copyPoi(JsonNode src) {
		JsonBuilder<ObjectNode> target = Json.startObject().put("name", src.get("name").asText()) //
				.put("town", src.get("town").asText()) //
				.put("zipcode", src.get("pCode").asText()) //
				.put("way", src.get("way").asText()) //
				.startObject("where") //
				.put("lat", src.get("lat").asDouble()) //
				.put("lon", src.get("lng").asDouble()) //
				.end();

		if (src.get("rubricId") != null)
			target.put("mainRubricId", src.get("rubricId").asText());

		if (src.get("phone") != null)
			target.put("phone", src.get("phone").asText());

		if (src.get("url") != null)
			target.put("url", src.get("url").asText());

		if (src.get("illustration") != null)
			target.put("illustration", src.get("illustration").get("url").asText());

		JsonNode allRubrics = src.get("allRubrics");
		if (allRubrics != null && allRubrics.size() > 0) {
			target.startArray("rubrics");
			allRubrics.forEach(rubric -> {
				target.startObject().put("rubricId", rubric.get("id").asText()) //
						.put("rubricLabel", rubric.get("label").asText()).end();
			});
			target.end();
		}

		RequestBodyEntity req = preparePost("/v1/data/resto", examplesKey).body(target.toString());

		try {
			post(req, 201);
		} catch (UnirestException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static ObjectNode buildRestoSchema() {
		return SchemaBuilder.builder("resto") //
				.property("name", "text").language("french").required().end() //
				.property("where", "geopoint").required().end() //
				.property("way", "text").language("french").required().end() //
				.property("town", "text").language("french").required().end() //
				.property("zipcode", "string").required().end() //
				.property("mainRubricId", "string").required().end() //
				.property("url", "string").end() //
				.property("illustration", "string").end() //
				.property("phone", "string").end() //
				.property("rubrics", "object").required().array() //
				.property("rubricId", "string").required().end() //
				.property("rubricLabel", "text").language("french").required().end() //
				.end() //
				.end() //
				.build();
	}
}
