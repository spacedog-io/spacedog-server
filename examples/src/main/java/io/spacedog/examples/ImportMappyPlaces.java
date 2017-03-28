/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.Schema;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json7;
import io.spacedog.utils.JsonBuilder;

public class ImportMappyPlaces extends SpaceTest {

	private static SpaceDog backend;

	public static void main(String[] args) {
		try {

			SpaceRequest.setForTestingDefault(false);
			backend = resetBackend("examples", "examples", "hi examples");
			SpaceRequest.post("/1/schema/resto").adminAuth(backend).bodySchema(buildRestoSchema()).go(201);

			double step = 0.01;

			for (double lat = 48.5; lat <= 49; lat += step) {
				for (double lon = 1.8; lon <= 2.9; lon += step) {

					// "48.671228,1.854415,49.034931,2.843185");

					JsonNode pois = SpaceRequest.get("/search/1.0/find")//
							.baseUrl("http://search.mappy.net")//
							.queryParam("max_results", "100")//
							.queryParam("extend_bbox", "0")//
							.queryParam("q", "restaurant")//
							.queryParam("bbox", "" + lat + ',' + lon + ',' + (lat + step) + ',' + (lon + step))//
							.go(200)//
							.objectNode()//
							.get("pois");

					if (pois != null)
						pois.forEach(ImportMappyPlaces::copyPoi);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	private static void copyPoi(JsonNode src) {
		JsonBuilder<ObjectNode> target = Json7.objectBuilder().put("name", src.get("name").asText()) //
				.put("town", src.get("town").asText()) //
				.put("zipcode", src.get("pCode").asText()) //
				.put("way", src.get("way").asText()) //
				.object("where") //
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
			target.array("rubrics");
			allRubrics.forEach(rubric -> {
				target.object().put("rubricId", rubric.get("id").asText()) //
						.put("rubricLabel", rubric.get("label").asText()).end();
			});
			target.end();
		}

		try {
			SpaceRequest.post("/1/data/resto").adminAuth(backend).body(target).go(201);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Schema buildRestoSchema() {
		return Schema.builder("resto") //
				.text("name").french() //
				.geopoint("where") //
				.text("way").french() //
				.text("town").french()//
				.string("zipcode")//
				.string("mainRubricId")//
				.string("url")//
				.string("illustration")//
				.string("phone")//

				.object("rubrics").array() //
				.string("rubricId") //
				.text("rubricLabel").french()//
				.close() //

				.build();
	}
}
