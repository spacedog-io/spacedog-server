/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.HttpRequest;

import io.spacedog.services.AdminResourceTest.ClientAccount;

public class ImportMappyPlaces extends Assert {

	private static ClientAccount examplesAccount;

	// public static void resetExamplesAccount() throws UnirestException,
	// IOException {
	//
	// HttpRequestWithBody req1 =
	// delete("/v1/admin/account/examples").basicAuth("examples", "hi
	// examples");
	// delete(req1, 200, 401);
	//
	// refresh("examples");
	// refresh(AdminResource.ADMIN_INDEX);
	//
	// RequestBodyEntity req2 = SpacePostRequest.post("/v1/admin/account/")
	// .body(Json.startObject().put("backendId", "examples").put("username",
	// "examples")
	// .put("password", "hi examples").put("email",
	// "hello@spacedog.io").toString());
	//
	// examplesAccount = post(req2,
	// 201).response().getHeaders().get(AdminResource.BACKEND_KEY_HEADER).get(0);
	//
	// assertFalse(Strings.isNullOrEmpty(examplesAccount));
	//
	// refresh(AdminResource.ADMIN_INDEX);
	// refresh("examples");
	// }

	public static void main(String[] args) {
		try {

			/*
			 * Uncomment this if you want to reset the "examples" account. Be
			 * careful to copy the backend key to the console scripts.
			 */
			// examplesAccount = AdminResourceTest.resetAccount("examples",
			// "examples", "hi examples",
			// "hello@spacedog.io");

			SpaceRequest.delete("/v1/schema/resto").basicAuth(examplesAccount).go(200, 404, 401);

			SpaceRequest.post("/v1/schema/resto").basicAuth(examplesAccount).body(buildRestoSchema().toString())
					.go(201);

			double step = 0.01;

			for (double lat = 48.5; lat <= 49; lat += step) {
				for (double lon = 1.8; lon <= 2.9; lon += step) {

					HttpRequest req1 = Unirest.get("http://search.mappy.net/search/1.0/find")
							.queryString("max_results", "100").queryString("extend_bbox", "0")
							.queryString("q", "restaurant")
							.queryString("bbox", "" + lat + ',' + lon + ',' + (lat + step) + ',' + (lon + step));

					// "48.671228,1.854415,49.034931,2.843185");

					JsonNode pois = new SpaceRequest(req1).go(200).objectNode().get("pois");

					if (pois != null)
						pois.forEach(ImportMappyPlaces::copyPoi);
				}
			}

			SpaceRequest.refresh("examples");
			SpaceRequest.refresh(AdminResource.ADMIN_INDEX);

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

		try {
			SpaceRequest.post("/v1/data/resto").basicAuth(examplesAccount).body(target.toString()).go(201);
		} catch (Exception e) {
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
