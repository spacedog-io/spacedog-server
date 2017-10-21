/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceTest;
import io.spacedog.model.DataPermission;
import io.spacedog.model.Schema;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;

public class ImportMappyPlaces extends SpaceTest {

	private static SpaceDog superadmin;

	public static void main(String[] args) {
		try {

			SpaceRequest.setForTestingDefault(false);
			superdog().admin().deleteBackend("examples");
			superadmin = createBackend("examples");
			superadmin.post("/1/schema/resto").bodySchema(buildRestoSchema()).go(201);

			double step = 0.01;

			for (double lat = 48.5; lat <= 49; lat += step) {
				for (double lon = 1.8; lon <= 2.9; lon += step) {

					// "48.671228,1.854415,49.034931,2.843185");

					JsonNode pois = SpaceRequest.get("/search/1.0/find")//
							.backend("http://search.mappy.net")//
							.queryParam("max_results", 100)//
							.queryParam("extend_bbox", 0)//
							.queryParam("q", "restaurant")//
							.queryParam("bbox", "" + lat + ',' + lon + ',' + (lat + step) + ',' + (lon + step))//
							.go(200)//
							.asJsonObject()//
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
		JsonBuilder<ObjectNode> target = Json.builder().object()//
				.add("name", src.get("name").asText()) //
				.add("town", src.get("town").asText()) //
				.add("zipcode", src.get("pCode").asText()) //
				.add("way", src.get("way").asText()) //
				.object("where") //
				.add("lat", src.get("lat").asDouble()) //
				.add("lon", src.get("lng").asDouble()) //
				.end();

		if (src.get("rubricId") != null)
			target.add("mainRubricId", src.get("rubricId").asText());

		if (src.get("phone") != null)
			target.add("phone", src.get("phone").asText());

		if (src.get("url") != null)
			target.add("url", src.get("url").asText());

		if (src.get("illustration") != null)
			target.add("illustration", src.get("illustration").get("url").asText());

		JsonNode allRubrics = src.get("allRubrics");
		if (allRubrics != null && allRubrics.size() > 0) {
			target.array("rubrics");
			allRubrics.forEach(rubric -> {
				target.object().add("rubricId", rubric.get("id").asText()) //
						.add("rubricLabel", rubric.get("label").asText()).end();
			});
			target.end();
		}

		try {
			superadmin.post("/1/data/resto").bodyJson(target.build()).go(201);
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

				.acl("key", DataPermission.search)//
				.acl("admin", DataPermission.create)//
				.build();
	}
}
